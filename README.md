# raygui-jnk

[raygui](https://github.com/raysan5/raygui), raylib's immediate-mode GUI
companion, ported to **[jank](https://jank-lang.org)**: a native Clojure
dialect that compiles through C++ and LLVM rather than running on the JVM.

**24 examples across 7 groups**, covering raygui's control catalogue. Same
names and same descriptions as the sibling
[`raygui-jlt`](https://github.com/burinc/raygui-jlt), so the two ports read as
one library seen through two Clojure implementations.

raygui was not available to jank before this. It is a header-only C library
with no package anywhere, which is why
[`b12n-raylib-jnk`](https://github.com/burinc/b12n-raylib-jnk) replaced raygui
with keyboard controls in seventeen of its examples. This repo builds raygui as
an ordinary jank package, so those controls can be real again.

<p align="center">
  <img src="docs/demos/basic-controls.png" width="270">
  <img src="docs/demos/color-picker.png" width="270">
  <img src="docs/demos/style-selector.png" width="270">
</p>

## Requirements

- [jank](https://jank-lang.org) and the `lein-jank` plugin
- A C++ compiler, and CMake
- [Babashka](https://babashka.org)

Verified on macOS, Apple M1 Pro, with jank `0.1-alpha`, `lein-jank 2026.06-1`
and `org.jank-lang.commons/raylib-sys 2026.08-2`. Linux and x86-64 are
untested.

## Quick start

```sh
bb examples              # the suite, grouped
bb basic-controls        # run one, windowed. Q quits.
bb shot basic-controls   # run headless and screenshot to /tmp/

bb check                 # compile every example headless, no window
bb lint                  # clj-kondo over every .jank file
```

There is no separate build step for raygui. `raygui-sys/jank-build.bb`
compiles it during the first `lein` invocation, against the same libraylib the
`raylib-sys` package provides. First run takes a minute or two while raylib
itself builds; after that `bb check` runs in about 7 seconds and `bb lint` in
half a second.

macOS has no `bwrap`, so lein-jank's build sandbox cannot work there. Every
`lein` call in `bb.edn` already passes `--disable-sandbox`.

## Layout

```
raygui-sys/                 the installable jank package
  jank-build.bb               compiles raygui, emits the build directives
  vendor/raygui.h             pinned at 5.0-9-gfbf5d95
  vendor/styles/*.rgs         six themes, each with its own embedded font
  src/net/b12n/raygui_jnk/
    raygui.jank               the only namespace that includes raygui.h
    raylib.jank               window, screenshot and style-path helpers
raygui-examples/            the suite: no vendor/, no jank-build.bb
  src/net/b12n/raygui_jnk/    24 examples plus the check namespace
```

## Using raygui in your own jank project

```sh
bb sys:install
```

```clojure
:dependencies [[net.b12n/raygui-sys "0.1.0-SNAPSHOT"]]
```

That is the whole setup. Your project needs no vendored copy of raygui and no
build script of its own, which is what `raygui-examples/` demonstrates.
[`docs/guide/building-raygui.md`](docs/guide/building-raygui.md) covers the
details, including a build cache that will hand you a stale library without
saying so.

## The examples

See [`docs/guide/example-catalog.md`](docs/guide/example-catalog.md) for all 24
with screenshots, or run `bb examples` for the same grouping live.

| group | examples |
|---|---|
| basics | `basic-controls` `toggles` `labels-lines` `icon-buttons` |
| inputs | `text-box` `text-input-box` `spinner-value-box` `sliders` `progress-bar` |
| collections | `dropdown-box` `combo-box` `list-view` `list-view-ex` `tab-bar` |
| containers | `panel-group-box` `scroll-panel` `window-box` `floating-window` |
| dialogs | `message-box` `custom-input-box` |
| color | `color-picker` `color-picker-hsv` |
| styling | `style-selector` `gui-state` |

Four are ports of raygui's own examples: `scroll-panel`, `floating-window`,
`custom-input-box` and `style-selector`.

## Documentation

- [`the-jank-shape.md`](docs/guide/the-jank-shape.md): the boundary rule that
  shapes the whole binding, and everything that follows from it.
- [`building-raygui.md`](docs/guide/building-raygui.md): the jank package
  protocol, and why linking against the right libraylib is load-bearing.
- [`what-the-gates-do-not-catch.md`](docs/guide/what-the-gates-do-not-catch.md):
  ten ways an example can be wrong while every automated check passes.
- [`example-catalog.md`](docs/guide/example-catalog.md): all 24, with the six
  themes.

## What this repo does not claim

Every example has had its screenshot looked at by a person, so the suite
renders correctly. Nothing here has been clicked by a test, and nothing can be:
synthetic clicks do not actuate a raylib or GLFW app at all, which
`b12n-raylib-jlt` measured at 0 of 8 clicks delivered across every hold
duration it tried. Whether a dropdown opens or a slider drags has been checked
by hand.

Demo GIFs are a stated non-goal for the same reason. raygui is mouse-driven, so
a recording could show pointer motion but never a press or a drag.

## Licence

zlib, the same as raygui and raylib, so the original terms carry through. This
project vendors and redistributes raygui. See [`NOTICE`](NOTICE).
