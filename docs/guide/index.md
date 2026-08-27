# raygui-jnk guide

24 [raygui](https://github.com/raysan5/raygui) examples written in
[jank](https://jank-lang.org), a native Clojure dialect that compiles through
C++ and LLVM rather than running on the JVM.

raygui was not available to jank before this. It is a header-only C library
with no package anywhere, which is why the sibling
[`b12n-raylib-jnk`](https://github.com/burinc/b12n-raylib-jnk) replaced raygui
with keyboard controls in seventeen of its examples and wrote up the pattern.
This repo builds raygui as an ordinary jank package, so those controls can be
real again.

The suite is complete: 24 examples across 7 groups. `bb examples` prints the
live count.

## Why raygui suits this

Nearly every raygui control has the same shape. Bounds in, application state
through a pointer, an `int` out:

```c
int GuiSlider(Rectangle bounds, const char *textLeft, const char *textRight,
              float *value, float minValue, float maxValue);
```

There is no callback machinery, no retained widget tree, and raygui keeps no
per-control state at all. jank has one genuine gap in its C interop, which is
that you cannot hand a jank function to a C API expecting a function pointer.
raygui never asks for one, so that gap does not touch this repo.

## Where the work actually went

Not into declaring functions. jank compiles through C++, so `raygui.h` is the
binding and the count of hand-written signatures here is zero. The sibling
`raygui-jlt` writes 61 `defcfn` forms to reach the same place through an FFI.

The work went into one rule instead: a native value cannot cross a jank
function boundary. A `Rectangle` cannot be passed to or returned from a `defn`,
so the wrapper is built so every native value is born and consumed inside the
same function body, and only numbers, bools, strings and opaque boxes travel
between namespaces. [`the-jank-shape.md`](the-jank-shape.md) is the full
account.

## The commands that matter

```sh
bb examples              # the suite, grouped
bb check                 # compile every example headless, no window
bb lint                  # clj-kondo over every .jank file
bb basic-controls        # run one, windowed. Q quits.
bb shot basic-controls   # run headless and screenshot
```

There is no separate build step for raygui. `raygui-sys/jank-build.bb` compiles
it as part of the first `lein` invocation, against the same libraylib the
`raylib-sys` package provides.

## The pages

- [`the-jank-shape.md`](the-jank-shape.md): the boundary rule and everything
  that follows from it. Cells, text buffers, the two argument shapes that look
  identical in C, the array marshalling, and the one place where a rule the
  sibling repo states turns out to be narrower than it reads.
- [`building-raygui.md`](building-raygui.md): the jank package protocol, why
  linking against the right libraylib is load-bearing rather than incidental,
  how to use `raygui-sys` from another project, and a build cache that will
  hand you a stale library without saying so.
- [`what-the-gates-do-not-catch.md`](what-the-gates-do-not-catch.md): ten ways
  an example here can be wrong while every automated check passes. Each one
  happened during this port. Read it before adding an example.
- [`example-catalog.md`](example-catalog.md): all 24 with screenshots, plus the
  six vendored themes.

## The screenshot is a gate

For a GUI library that is not a nicety. A control at the wrong bounds, a style
that silently failed to load, a colour with red and blue swapped, or a frame
captured before the render batch flushed all compile cleanly and pass
`bb check`. Every example in this repo has had its PNG looked at by a person.

What no gate here covers is interaction. Synthetic clicks do not actuate a
raylib or GLFW app at all, which `b12n-raylib-jlt` measured at 0 of 8 clicks
delivered across every hold duration it tried. So the automated claim is that
the suite renders correctly. Whether a dropdown opens or a slider drags has
been checked by hand.

## Demo GIFs are a stated non-goal

Same measurement, taken to its conclusion. raygui is mouse-driven by
definition, so a recorded GIF could show hover and pointer motion but never a
button press, a dropdown opening, or a slider being dragged. Twenty-four
recordings of controls nobody touches would be worse than none, so the suite
ships static screenshots instead.

## Vendored revision

`raygui-sys/vendor/raygui.h` is pinned at `5.0-9-gfbf5d95`, zlib licensed. That
is nine commits past the 5.0 release and its API differs from the published 5.0
documentation in a few places, so write bindings from the header in `vendor/`.
`NOTICE` carries the full attribution, including the four examples ported from
raygui's own example tree.

## See also

- [raygui](https://github.com/raysan5/raygui), the upstream library.
- [raylib](https://github.com/raysan5/raylib), which raygui draws through.
- [jank](https://jank-lang.org), and its book chapter on
  [reaching into C++](https://book.jank-lang.org/cpp-interop/index.html).
- `b12n-raylib-jnk`'s `docs/guide/`, for the general jank interop rules this
  repo builds on rather than restates.
- `raygui-jlt`, the same 24 examples in jolt, for a side-by-side of FFI
  bindings against compiled C++ interop.
