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
- **`bb check:registration`.** Verifies the three registration touchpoints a
  compiler would otherwise have to find: the source file, the `:profiles`
  entry, and the `:require` plus `required` entry in `check.jank`. That last
  one is why it exists. An example missing from `check.jank` is never compiled
  by `bb check`, which still prints success and a count, so the compile gate
  goes green over code it never read. It also reports a source file with no
  registry row.
- **CI.** A GitHub Actions workflow runs the five gates that need no compiler,
  on every pull request and on `main`: that `bb.edn` still loads, clj-kondo
  over every `.jank` file, registry agreement with each task's `:doc`, the
  registration touchpoints, and the README gallery against the registry.
- **Four guide pages** and a catalog generated from the registry, published at
  <https://raygui-jnk.b12n.app> and mirrored into `b12n-wikis`.
- **Task surfaces.** `bb info` groups every task, `bb run-all` cycles the suite
  as a demo reel, `bb readme:examples` regenerates the README gallery from the
  registry, and `bb docs-sync` republishes the site.

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
- CI cannot compile anything. jank publishes no current prebuilt binary, so
  `bb check` and every screenshot are still run by hand.
