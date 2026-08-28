## What this changes

<!-- One or two sentences. If it adds an example, name it and say whether it
     is a port of one of raygui's own examples or an original. -->

## Verification

CI runs the five gates that need no compiler: that `bb.edn` still loads,
`bb lint`, `bb examples`, `bb check:registration` and
`bb readme:examples-check`. It cannot compile anything, because jank publishes
no current prebuilt binary, and it cannot look at a picture. Those two are
yours.

- [ ] `bb check` passes locally, so the whole suite still compiles
- [ ] `bb lint` is clean

## If this adds or changes an example

<!-- Skip this section otherwise. -->

All six touchpoints, or the example does not appear in `bb info` and may not
compile at all. `bb check:registration` checks the first three and CI runs it,
so this list is here to save you a red build rather than to catch you out.

- [ ] `raygui-examples/src/net/b12n/raygui_jnk/<name>.jank`
- [ ] a `:profiles` entry in `raygui-examples/project.clj`
- [ ] a `:require` **and** a `required` entry in `check.jank`
- [ ] a `bb.edn` task whose `:doc` matches the registry description exactly
- [ ] a row in `scripts/examples_registry.clj`, description within 49 characters
- [ ] a screenshot committed to `docs/demos/<name>.png`, then
      `bb readme:examples` to fold it into the gallery

### The screenshot

**This is the gate.** A control at the wrong bounds, a style that silently
failed to load, or a colour with red and blue swapped all compile cleanly and
pass `bb check`. `what-the-gates-do-not-catch.md` lists ten such failures,
every one of which actually happened while this suite was written.

- [ ] I ran `bb shot <name>` and **looked at the PNG**
- [ ] The example prints or displays enough state that the picture would look
      wrong if the control misbehaved

<!-- Say what the screenshot proves. "Spinner seeded above its maximum, so the
     clamp shows", not "it looks right". -->

## Bindings

<!-- Skip unless you touched raygui-sys/src/. -->

- [ ] Written against `raygui-sys/vendor/raygui.h`, not the published raygui
      docs. The vendored revision is nine commits past 5.0 and differs in
      several places.
- [ ] No native value crosses a jank function boundary
- [ ] `bb sys:install` after any change under `vendor/`, since the build cache
      will otherwise hand you a stale library without saying so
- [ ] Constants come from `g/`, never `cpp/`

## Environment you tested on

- OS and arch (`uname -sm`):
- jank (`jank --version`):

## Notes for the reviewer

<!-- Anything surprising, any deliberate deviation from raygui's own example,
     anything you are unsure about. -->
