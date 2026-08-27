# The jank shape

raygui's C API meets one jank rule that shapes everything else in this repo: a
native value cannot cross a jank function boundary. Every design choice below
follows from that, and each claim names the file that proves it.

If you have read `b12n-raylib-jnk`'s
[`native-value-lifetimes.md`](https://github.com/burinc/b12n-raylib-jnk/blob/main/docs/guide/native-value-lifetimes.md)
and [`cpp-interop-toolbox.md`](https://github.com/burinc/b12n-raylib-jnk/blob/main/docs/guide/cpp-interop-toolbox.md),
you know the general rules already. This page covers what binding a GUI library
added on top, including one place where a rule stated there turns out to be
narrower than it sounds.

## There is no binding layer

Worth saying first, because it is the biggest difference from the jolt port of
the same library. jank compiles through C++, so `raygui.h` *is* the binding.
There is no signature to declare, no struct layout to describe, no ABI to get
wrong:

```clojure
(ns net.b12n.raygui-jnk.raygui
  (:include "raylib.h" "raygui.h"))

(cpp/GuiButton bounds "Click me")   ; the real function, resolved by clang
```

The sibling `raygui-jlt` writes 61 `defcfn` forms to reach the same place. Here
the count of hand-written signatures is zero. What replaces that work is the
boundary rule.

## The rule, and the one place it bends

A `Rectangle`, `Color`, `Vector2` or `Font` has no conversion trait, so it
cannot be passed to or returned from a `defn`. Integers, floats, bools and
strings cross freely.

Every control wrapper is built so the native value is born and consumed inside
the same function body:

```clojure
(defn button! [x y w h text]
  (= 1 (int (cpp/GuiButton (cpp/Rectangle (cpp/float x) (cpp/float y)
                                          (cpp/float w) (cpp/float h))
                           text))))
```

`x y w h` arrive as ordinary jank numbers. The `Rectangle` never leaves that
body. An `int` comes back. Examples in this repo never see a `Rectangle` at
all, which lands in the same place the jolt port reached for a different
reason: there it avoided a per-frame allocation, here the type simply cannot
travel.

Closures count as boundaries too, so `dotimes` and `doseq` are out wherever a
native pointer is in scope. `loop`/`recur` is inline and works. Several
examples iterate that way for no other reason.

### Where the sibling repo's rule is narrower than it reads

`native-value-lifetimes.md` says boxing cannot be wrapped in a function, and
gives this as the failing case:

```clojure
(defn box-it [s] (cpp/box (cpp/new cpp/Shader s)))   ; fails
```

That holds when the argument is native. By the time the body runs, `s` has
already arrived as an `object_ref`, and there is nothing left to box. It does
not hold when the argument is trait-convertible, because then no boxing has
been lost on the way in:

```clojure
(defn fcell [v] (cpp/box (cpp/new cpp/float (cpp/float v))))   ; works
```

`(cpp/float v)` on a jank number is a conversion rather than a re-box. Every
cell constructor in `raygui.jank` relies on this. Reading the broader claim
alone would have pushed all of them into macros for no benefit, so the
distinction is worth carrying: the rule is about *native* arguments, not about
`defn` as such.

## Cells

raygui keeps no state. The application owns it and C wants a pointer to it, so
this repo allocates a typed native slot once, outside the frame loop, and hands
raygui its address:

```clojure
(let [volume (g/fcell 0.35)]
  (loop [frame 0]
    (g/slider! 20.0 40.0 200.0 24.0 nil nil volume 0.0 1.0)
    (g/fvalue volume)))                     ; => 0.3499999940395355
```

Six types cover the whole suite: `fcell`, `icell`, `bcell`, `ccell` for a
`Color`, `v2cell` and `v3cell`. Each has a matching reader and a `reset!`.
Text is the seventh and behaves differently, so it gets its own section below.

A pointer passed as a jank function parameter arrives as an `object_ref`, which
is why cells are boxed rather than raw. Allocate them before the loop, never
inside it. A cell allocated per frame leaks at sixty a second, and for text
cells that exhausts the heap in a long run.

## Two argument shapes the compiler will not separate for you

This is the sharpest trap in the whole binding, because both shapes are spelled
the same in C and only one of them takes a cell.

Controls with a `bool *` out-param, like `GuiToggle` and `GuiCheckBox`, take a
cell. Controls with a plain `bool editMode`, like `GuiTextBox`,
`GuiDropdownBox`, `GuiSpinner` and `GuiValueBox`, take an ordinary jank
boolean, and the caller owns the mode:

```clojure
(when (pos? (g/text-box! 20.0 40.0 200.0 30.0 buf 64 (g/bvalue editing?)))
  (g/breset! editing? (not (g/bvalue editing?))))
```

Pass a cell where the plain bool belongs and it compiles cleanly, because C++
converts a pointer to `true`. The control then sits permanently in edit mode.
The spike behind this repo made exactly that mistake and did not notice,
because the text box still rendered and still accepted text. Only reading the
header showed the argument was never a pointer.

## Text buffers

`GuiTextBox` wants a mutable `char *`. `cpp/new` does not make arrays, so the
buffer comes from `cpp/MemAlloc` with a cast, seeded through raylib's own
`TextCopy`:

```clojure
(defn tcell [s cap]
  (let [p (cpp/unsafe-cast (:* cpp/char) (cpp/MemAlloc (cpp/uint32_t cap)))]
    (cpp/TextCopy p s)
    (cpp/box p)))
```

Reading one back needs a cast, not `TextFormat`. jank cannot call variadic C
functions at all, so raylib's usual string helper is unavailable here:

```clojure
(defn tvalue [c]
  (str (cpp/cast (:* (:const cpp/char)) (cpp/unbox (:* cpp/char) c))))
```

`text-box.jank` prints the buffer's length beside the field for this reason. A
readback that silently failed would show up in the picture rather than only in
a log.

## Optional captions need two call sites

Many raygui controls accept `NULL` for their caption. jank cannot express that
by passing `nil`, and the failure arrives late:

```clojure
(g/line! 20.0 90.0 380.0 12.0 nil)
;; compiles fine, then at runtime:
;; invalid object type (expected persistent_string found nil)
```

Coercing at the argument position does not work either. `(if text text
cpp/nullptr)` fails during code generation, because the two branches have
incompatible C++ types:

```
error: assigning to 'std::nullptr_t' from incompatible type
       'jank::runtime::oref<jank::runtime::object>'
```

What works is two complete calls, one per type, which a macro emits so the
duplication stays in one place:

```clojure
(defmacro ^:private opt-text [f x y w h text & more]
  `(if ~text
     (~f (cpp/Rectangle (cpp/float ~x) (cpp/float ~y) (cpp/float ~w) (cpp/float ~h))
         ~text ~@more)
     (~f (cpp/Rectangle (cpp/float ~x) (cpp/float ~y) (cpp/float ~w) (cpp/float ~h))
         cpp/nullptr ~@more)))
```

A macro rather than a function, since neither a `Rectangle` nor a `const char *`
could cross a function boundary to get there.

The slider family is the exception. `GuiSlider`, `GuiSliderBar` and
`GuiProgressBar` each take two optional captions, and covering both positions
would need four call sites. They substitute `""` instead, which is safe for a
specific reason worth checking rather than assuming: raygui guards each caption
with `if (textLeft != NULL)` around a draw whose width is
`GuiGetTextWidth(textLeft)`, and that function returns 0 for an empty string
because its scan loop exits on the terminator immediately. An empty caption
reserves no space and draws nothing.

## Arrays

`GuiListViewEx` is the only control in the suite taking `char **`. The array is
built, used and freed inside one function body, since neither it nor its
elements can travel:

```clojure
(let [arr (cpp/unsafe-cast (:* (:* cpp/char)) (cpp/MemAlloc (cpp/uint32_t (* 8 n))))]
  (loop [i 0] ...)                       ; fill with loop/recur, not doseq
  (cpp/GuiListViewEx bounds arr (cpp/int n) ...)
  (loop [i 0] ...)                       ; free each element
  (cpp/MemFree arr))
```

Examples pass a plain jank vector of strings and never see the array.
`list-view-ex.jank` is the one that uses it. The pointer width is hardcoded to
8, because jank has no `sizeof` and every platform this repo has run on is
64-bit.

## Out-params that are structs

`GuiScrollPanel` reports the visible view back through a `Rectangle *`. A
native struct cannot be returned across a boundary, so the wrapper reads its
four floats and hands back numbers:

```clojure
(defn scroll-panel! [x y w h text cx cy cw ch scroll-cell]
  (let [view (cpp/new cpp/Rectangle)]
    (cpp/GuiScrollPanel ... view)
    [(+ 0.0 (.-x (cpp/* view))) (+ 0.0 (.-y (cpp/* view)))
     (+ 0.0 (.-width (cpp/* view))) (+ 0.0 (.-height (cpp/* view)))]))
```

`scroll-panel.jank` scissors its content to that rectangle, which is what shows
the value is live. raygui reports 386 by 221 inside a 400 by 260 panel, having
subtracted its own scrollbars.

`ColorFromHSV` has the same problem in the other direction: it returns a
`Color` by value. `hsv->rgb` consumes it inside the wrapper and returns
`[r g b]`.

## Style colours are byte-swapped and signed

raygui stores style colours as `0xRRGGBBAA`. raylib's `Color` packs
little-endian as `0xAABBGGRR`. Feeding one straight to the other produces a
plausible wrong colour rather than an error, and `GetColor` is the conversion
raygui's own README uses.

jank adds a second half. `GuiGetStyle` returns `unsigned int`, and jank boxes it
signed:

```
GuiGetStyle(DEFAULT, BACKGROUND_COLOR)     ->  -168430081     (0xF5F5F5FF)
GuiGetStyle(DEFAULT, BORDER_COLOR_NORMAL)  -> -2088532993     (0x838383FF)
```

Both correct, both negative. Widening needs `cpp/uint32_t`, since
`cpp/unsigned_int` does not exist and the compiler answers `Unable to find
'unsigned_int' within the global namespace`. `clear-background!` does the whole
conversion once so no example repeats it.

## Constants come from the wrapper, not from `cpp/`

Examples do not include `raygui.h`. For a while they still reached `cpp/DEFAULT`
and it worked, because the wrapper's own include had made the enum visible in
the shared C++ session. That is an accident to rely on, and the failure mode
when an index is wrong gives you nothing: a style set on the wrong control does
nothing at all, silently.

That happened here. A guessed `TABBAR` of 20, when the real value is 11, meant
the tab close buttons simply never appeared. Nothing raised, nothing logged.

`raygui.jank` now exports the enums as jank vars, each read from the C enum so
none can drift from the vendored header:

```clojure
(def TABBAR (int (cpp/cast cpp/int cpp/TABBAR)))
```

The cast is required. `(int cpp/TABBAR)` on its own throws `Can't convert
GuiControl to integer`.

## Small things that cost time

- `cpp/int` on a jank double throws at runtime with `invalid object type
  (expected integer found small_real)`. Box first: `(cpp/int (int x))`.
- `case` will not compile a clause list whose results are C string literals.
  jank raises `no viable overloaded '='` from inside `clojure/core.jank`, naming
  neither your form nor your file. `cond` works. `message-box.jank` uses it.
- There is no JVM, so `Math/round`, `format` and `.indexOf` are all absent.
  `sliders.jank` carries a hand-rolled `fmt2`, borrowed from
  `b12n-raylib-jnk`'s.
- Several native values in one `println` fails during code generation with
  `member reference base type 'i64' is not a structure or union`. Print one per
  call.
- `rlDrawRenderBatchActive` lives in `rlgl.h`, so any namespace taking a
  screenshot needs it in `:include`.

## What is not verified

No control in this repo has ever been clicked by a test, and none can be. The
sibling `b12n-raylib-jlt` measured that synthetic clicks do not actuate a
raylib or GLFW app at all: 0 of 8 clicks and 0 of 2 drags delivered at every
hold duration up to 300ms, against 3 of 3 for pointer motion. Every control's
*behaviour* here has been checked by a person moving a real mouse, or not at
all. The automated claim is that the suite renders correctly.

x86-64 and Linux are both untested. Unlike the jolt port this carries no ABI
risk of its own, since clang handles the struct passing, but nobody has run it.
