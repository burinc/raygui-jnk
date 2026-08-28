# Contributing

New examples are welcome. The suite is deliberately mechanical to grow.

The documentation is published at <https://raygui-jnk.b12n.app>, generated from
this repo's `docs/guide/` by the shared `b12n-docs` engine. Edit the Markdown
here, never the generated site.

## Adding an example

Six touchpoints, and missing any of them fails quietly rather than loudly:

1. `raygui-examples/src/net/b12n/raygui_jnk/<name>.jank`
2. a `:profiles` entry in `raygui-examples/project.clj`
3. a `:require` **and** a `required` entry in `check.jank`, or `bb check` will
   never compile your example and will still report success
4. a `bb.edn` task whose `:doc` matches the registry description exactly
5. a row in `scripts/examples_registry.clj`, description capped at 49
   characters
6. a screenshot you have looked at, committed to `docs/demos/<name>.png`,
   then `bb readme:examples` to fold it into the README gallery

`bb examples` checks 4 and 5 against each other and fails on drift.
`bb readme:examples-check` fails when the screenshot is missing or the gallery
is stale, and the pre-commit hook runs it. Whether you actually looked at the
picture is the one thing no gate can check.

## The gates

```sh
bb check                  # compiles every registered example, no window
bb lint                   # clj-kondo, and it fails on warnings
bb examples               # grouping, the cap, and registry/bb.edn agreement
bb shot <n>               # screenshot, and it fails when no file was written
bb readme:examples-check  # gallery matches the registry, every demo committed
```

`bb hooks:install` puts `bb lint` and `bb readme:examples-check` in a
pre-commit hook. Both are sub-second, and since this repo has no CI the hook
is where those two get enforced at all. `bb info` lists every
task grouped, which is easier to scan than flat `bb tasks` now that there is
one per example.

`bb run-all` cycles the whole suite as a demo reel. It gives each example a
window deadline rather than killing the process, so raylib shuts down cleanly
and Q still skips ahead.

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

## Republishing the docs (maintainer)

```sh
bb docs-sync              # rebuild, push the site repo, mirror the wiki, S3 + CloudFront
bb docs-sync --no-push    # commit locally, publish nothing outward
```

Needs the sibling checkouts (`b12n-docs`, `raygui-jnk.github.io`, `b12n-wikis`)
and AWS credentials for the `b12n` profile. The AWS CLI honours `HTTPS_PROXY`,
so on a restricted network unset it first or every call dies at the proxy while
git keeps working.

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
