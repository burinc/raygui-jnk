# Changelog

Notable changes to raygui-jnk. Written from the commit history rather than
from memory.

## Unreleased

Everything so far. The suite is complete and the repo has no tagged release
yet.

### Added

- **raygui as a jank package.** `raygui-sys` vendors raygui 5.0-9-gfbf5d95 and
  builds it through jank's native-package protocol, so a consuming project
  needs one dependency line and no vendored copy of its own.
- **A shared wrapper namespace.** `net.b12n.raygui-jnk.raygui` binds 53 of
  raygui's 61 functions, plus its control, property, state and result enums as
  jank vars read from the C header.
- **24 examples across 7 groups**, name-for-name identical to the `raygui-jlt`
  suite: basics, inputs, collections, containers, dialogs, color and styling.
  Four are ports of raygui's own examples.
- **Six vendored themes**, each loaded with its embedded font through
  `GuiLoadStyleFromMemory`.
- **Gates.** `bb check` compiles every example headless, `bb lint` runs
  clj-kondo over every `.jank` file, `bb examples` fails when a task's
  documentation drifts from the registry, and `bb shot` captures a screenshot
  and fails when no file was written.
- **Four guide pages** and a catalog generated from the registry.

### Not included

- **`GuiSetFont` and `GuiGetFont`** are left unbound rather than bound and
  unused. The vendored themes carry and apply their own embedded fonts, so
  nothing in the suite needs them. `GuiLoadStyle` is also unbound, since every
  style path goes through `GuiLoadStyleFromMemory` to keep the working
  directory out of it. `GuiTabBarEx`, `GuiValueBoxFloat`, `GuiGetIcons`,
  `GuiLoadIcons` and `GuiLoadIconsFromMemory` round out the eight.
- **Demo GIFs.** Synthetic clicks do not actuate a raylib or GLFW app, so a
  recording could never show a press or a drag.

### Known limits

- No control here has been exercised by an automated test. The suite's
  automated claim is that it renders correctly.
- Linux and x86-64 are untested.
