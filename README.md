# raygui-jnk

[raygui](https://github.com/raysan5/raygui), raylib's immediate-mode GUI
companion, ported to **[jank](https://jank-lang.org)** — a native Clojure
dialect that compiles through C++/LLVM rather than running on the JVM.

raygui was previously **unavailable to jank**: it is a header-only C library
with no package, which is why
[`b12n-raylib-jnk`](https://github.com/burinc/b12n-raylib-jnk) substituted
keyboard controls for raygui in 17 of its examples. This repo builds raygui as
an ordinary jank package, so those controls can be real again.

> **Status: in progress.** The foundation is in place and the suite is being
> ported. See `bb examples` for what has landed.

## Requirements

- [jank](https://jank-lang.org) `0.1-alpha` and the `lein-jank` plugin `2026.06-1`
- A C++ compiler and CMake
- [Babashka](https://babashka.org)

Verified on macOS, Apple M1 Pro.

## Quick start

```sh
bb examples              # the suite, grouped
bb check                 # compile every ported example, headless
bb lint                  # clj-kondo
bb basic-controls        # run one, windowed (Q quits)
bb shot basic-controls   # run headless and screenshot to /tmp/
```

There is no separate build step for raygui: `raygui-sys/jank-build.bb`
compiles it as part of the first `lein` invocation, against the same libraylib
the `raylib-sys` package provides.

macOS has no `bwrap`, so every `lein` call here passes `--disable-sandbox` for
you.

## Layout

```
raygui-sys/         the installable jank package
  jank-build.bb       compiles vendor/raygui_impl.c, emits build directives
  vendor/             raygui.h pinned at 5.0-9-gfbf5d95, plus six .rgs styles
  src/.../raygui.jank the only namespace that includes raygui.h
raygui-examples/    the example suite
```

Any jank project can use raygui by depending on `net.b12n/raygui-sys` — no
vendored copy of its own required.

## Documentation

Guide pages are written as the suite lands; see `docs/guide/`.

## Licence

zlib, the same as raygui and raylib. This project vendors and redistributes
raygui — see [`NOTICE`](NOTICE).
