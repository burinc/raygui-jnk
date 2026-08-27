# What the gates do not catch

The most useful page here if you are about to add an example.

`bb check` compiles. `bb lint` lints. Neither looks at a pixel, and a GUI
library fails in ways that survive both. Every item below happened during this
port, was caught by looking at a screenshot or reading the C, and would have
shipped otherwise.

## 1. A screenshot that was never written, with a zero exit code

`TakeScreenshot` resolves its argument against `CORE.Storage.basePath`, fixed
at `InitWindow`. Not the live working directory, so `ChangeDirectory` cannot
redirect it. From `rcore.c:1844`:

```c
strncpy(path, TextFormat("%s/%s", CORE.Storage.basePath, fileName), MAX_FILEPATH_LENGTH - 1);
```

Hand it `/tmp/x.png` and it builds `<basePath>//tmp/x.png`, which cannot be
created. raylib logs this and moves on:

```
WARNING: SYSTEM: [.../raygui-examples//tmp/basic-controls.png] Screenshot could not be saved
```

The process still exits 0. A gate that only checks the exit code passes with no
PNG at all. `maybe-screenshot!` captures by basename, where `basePath` resolves
cleanly, then renames the result. `bb shot` checks the file exists afterwards
and fails loudly when it does not.

## 2. A screenshot with three quarters of the frame missing

`FLAG_WINDOW_HIGHDPI` makes raylib double-count the DPI scale inside
`TakeScreenshot`. The first capture from this repo came back 1680 by 880 from a
420 by 220 window, with the content drawn correctly into the bottom-left
quadrant and the rest black.

No example here sets that flag. The sibling `b12n-raylib-jnk` does set it and
can, because it records through screen capture rather than `TakeScreenshot`.

## 3. A blank frame from the missing batch flush

raylib defers batched geometry until `EndDrawing`, so a mid-frame capture
without `rlDrawRenderBatchActive` writes a valid, perfectly blank PNG from a
program that just drew a screen full of controls.

This one is inherited rather than rediscovered. The jolt port hit it first and
`maybe-screenshot!` here flushes for the same reason. The symbol lives in
`rlgl.h`, which is why every example that screenshots carries
`(:include "raylib.h" "rlgl.h")`.

## 4. A lint gate that linted nothing

`clj-kondo` does not discover `.jank` files. Point it at a directory and it
scans for `.clj`, `.cljs` and `.cljc`, finds none, and reports success very
fast:

```
linting took 51ms, errors: 0, warnings: 0
```

That was this repo's lint gate for a phase and a half, over four real source
files. `bb lint` now globs every file explicitly and prints the count it is
about to check, so a number that stops climbing is visible.

`b12n-raylib-jnk`'s `.clj-kondo/config.edn` documents the same thing and this
repo adopted it wholesale.

## 5. A gate that reported problems and passed anyway

`clj-kondo` exits 2 on warnings and 3 on errors. `bb lint` discarded that exit
code, so the pre-commit hook printed two `unresolved-namespace` warnings and
let the commit through regardless.

Both now propagate. The fix was checked with a deliberate broken symbol rather
than by reading the change, which matters: the first version of this same gate
looked correct and was not.

## 6. A style that never loaded, under a label claiming it did

`GuiLoadStyleFromMemory` is fed by `LoadFileData`, which resolves against the
process working directory. A bare `"style_cyber.rgs"` works when run from the
repo root and finds nothing anywhere else. raylib logs a warning, raygui keeps
whatever style was already loaded, and the on-screen label goes on naming the
one you asked for.

Every path goes through `rl/styles-path`, which anchors against the vendored
directory and honours `RAYGUI_STYLES_DIR`.

Verifying this properly means checking each theme against the palette it
claims, not confirming that a screenshot exists. All seven were captured
separately and looked at. `cyber` is navy and amber, `terminal` green on black,
`candy` cream and salmon, `sunny` gold, `ashes` and `dark` two distinct greys.
Each also carries its own embedded font, and the font changing is the clearest
single sign that the `.rgs` actually loaded.

## 7. A colour that is wrong but plausible

raygui stores style colours as `0xRRGGBBAA` and raylib's `Color` packs
`0xAABBGGRR`. Feed one to the other and red and blue swap into something that
still looks deliberate. The jolt port measured this on the `cyber` background:
`0x81C0D0FF` is a light blue, and passing it straight to `ClearBackground`
renders salmon pink.

jank adds the sign. `GuiGetStyle` returns `unsigned int`, jank boxes it signed,
and the widening cast is `cpp/uint32_t`, since `cpp/unsigned_int` does not
exist. `clear-background!` does the conversion centrally so no example can get
it half right.

A glance at the screenshot will not catch this on its own, because a wrong
colour is still a colour. Comparing the render against the style's declared
value will.

## 8. `nil` where C wants a string

Passing `nil` for an optional caption compiles. It throws when that line
actually runs:

```
error: invalid object type (expected persistent_string found nil)
```

`bb check` compiles every example and never draws a frame, so it cannot see
this. The screenshot gate caught it three separate times here, in `line!`,
`panel!` and then across all six colour controls.

The wrapper handles it now, so an example can pass `nil` freely. If you add a
control that takes an optional caption, route it through `opt-text` or you will
meet this again.

## 9. A style property set on the wrong control

Nothing at all happens. No error, no log line, no visual difference except the
feature you wanted quietly not being there.

A guessed `TABBAR` index of 20, when the real value is 11, meant
`TAB_CLOSE_BUTTON` was set on a control that has no such property, and the tab
close buttons never appeared. The example compiled, ran, screenshotted, and
looked fine unless you knew what was supposed to be there.

`raygui.jank` exports every raygui enum as a jank var, each read from the C
enum. Use `g/TABBAR`, never a literal and never `cpp/TABBAR`. Examples do not
include `raygui.h`, so the `cpp/` form only resolves for them by accident.

## 10. Mouse interaction, all of it

Nothing in this repo has been clicked by a test, and nothing can be.
`b12n-raylib-jlt` measured that synthetic clicks do not actuate a raylib or
GLFW app at all: 0 of 8 clicks and 0 of 2 drags delivered at every hold
duration up to 300ms, against 3 of 3 for pointer motion.

So the automated claim this suite makes is that it **renders** correctly.
Whether a dropdown opens, a slider drags, or a tab closes has been checked by a
person with a real mouse, or not at all. `floating-window.jank` says so in its
own docstring, because its whole subject is a drag that no gate here exercises.

## The habit that catches the rest

Build each example so its own screenshot cross-checks its state. Print the cell
value next to the control, seed a scroll index to something other than zero,
seed a spinner above its own maximum so the clamp has to show.

`spinner-value-box.jank` starts its third spinner at 999 against a range ending
at 50. The screenshot reads 50. That is a working clamp, rather than a claim
that the clamp works.
