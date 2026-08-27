# Contributing

## Adding an example

Six touchpoints, and missing any of them fails quietly rather than loudly:

1. `raygui-examples/src/net/b12n/raygui_jnk/<name>.jank`
2. a `:profiles` entry in `raygui-examples/project.clj`
3. a `:require` **and** a `required` entry in `check.jank`, or `bb check` will
   never compile your example and will still report success
4. a `bb.edn` task whose `:doc` matches the registry description exactly
5. a row in `scripts/examples_registry.clj`, description capped at 49
   characters
6. a screenshot you have looked at

`bb examples` checks 4 and 5 against each other and fails on drift. Nothing
checks 6 for you.

## The gates

```sh
bb check     # compiles every registered example, no window
bb lint      # clj-kondo, and it fails on warnings
bb examples  # grouping, the 49-char cap, and registry/bb.edn agreement
bb shot <n>  # screenshot, and it fails when no file was written
```

`bb hooks:install` puts `bb lint` in a pre-commit hook.

After editing anything under `raygui-sys/src/`, run `bb sys:install`. The
consuming project resolves the wrapper from `~/.m2`, and for vendored C there
is a cache that will otherwise hand you a stale library without saying so.
[`docs/guide/building-raygui.md`](docs/guide/building-raygui.md) explains that
one.

## The screenshot is a gate

A control at the wrong bounds, a style that silently failed to load, or a
colour with red and blue swapped all compile cleanly and pass `bb check`.
[`what-the-gates-do-not-catch.md`](docs/guide/what-the-gates-do-not-catch.md)
lists ten such failures, every one of which happened while this suite was
being written.

Build your example so its own screenshot cross-checks its state. Print the
cell value beside the control. Seed a scroll index to something other than
zero. Seed a spinner above its own maximum so the clamp has to prove itself.

## Writing bindings

Read [`the-jank-shape.md`](docs/guide/the-jank-shape.md) first. The short
version:

- A native value cannot cross a jank function boundary, so build the
  `Rectangle` inside the wrapper function and never accept or return one.
- Application state travels in cells, allocated before the frame loop.
- A `bool *` out-param takes a cell. A plain `bool editMode` takes a jank
  boolean. Getting these the wrong way round compiles.
- An optional caption goes through the `opt-text` macro. Passing `nil` to a
  `const char *` compiles and throws at runtime.
- Take constants from `g/`, never `cpp/`. A wrong control index does nothing
  at all, silently.

Write every binding from `raygui-sys/vendor/raygui.h`. The vendored revision
is nine commits past 5.0 and differs from the published documentation in
several places.

## Style

Prose in this repo follows plain, direct English with no em-dashes. Code
follows what is already there.
