# Building raygui

raygui ships no library. It is a single header where the declarations and the
implementation live in the same file, and `RAYGUI_IMPLEMENTATION` has to be
defined in exactly one translation unit. There is no Homebrew formula, no
`libraygui.so` on any distro, and nothing to install.

jank has a native-package protocol that handles this, which is what the whole
`raygui-sys/` directory exists to use. The jolt port of the same library needs
a `bb lib:build` task, a gitignored `lib/` directory and build-if-missing logic
in every example task. None of that appears here.

Every command on this page was run against this repo, and the output is pasted
as it came back with `$HOME` shortened.

## The protocol

A `jank-build.bb` at a project root is run by lein-jank with the build metadata
on stdin, bound to `*input*`. Whatever it prints with a `jank-build::` prefix
becomes compiler flags:

```
jank-build::include-dir=<dir>
jank-build::link-dir=<dir>
jank-build::link-library=<name>
jank-build::define=K=V
```

`raygui-sys/jank-build.bb` compiles `vendor/raygui_impl.c` and emits three of
those. Here is what it actually printed on the last build:

```
jank-build::include-dir=~/dev/raygui-jnk/raygui-examples/target/_cache/raygui-sys-0.1.0-SNAPSHOT-src-6d9a5e87.../vendor
jank-build::link-dir=~/dev/raygui-jnk/raygui-examples/target/_cache/raygui-sys-0.1.0-SNAPSHOT-out-efd2201e.../lib
jank-build::link-library=raygui
```

That include directory is why `(:include "raygui.h")` resolves as an angled
include with no path juggling anywhere in the source.

## Linking against the right raylib

The load-bearing detail, and the one that would fail silently if it were wrong.

raygui's controls call `GetMousePosition`, `DrawRectangle` and `MeasureTextEx`
internally. Those have to resolve to the same libraylib the jank process has
already loaded. Against a second copy, raygui reads input state from a set of
globals nothing is updating, every control goes inert, and nothing raises.

The dependency graph supplies the right path. `(:inputs *input*)` maps each
dependency to its own build output, so the build script compiles against
raylib-sys rather than guessing at a system path:

```clojure
(let [{:keys [src-dir out-dir inputs]} *input*
      rl (get inputs "org.jank-lang.commons/raylib-sys")]
  ...
  "-I" (str (fs/path rl "include"))
  "-L" (str (fs/path rl "lib"))
  "-lraylib")
```

The key is fully qualified. `"raylib-sys"` returns nil, and the resulting `cc`
invocation fails with `no such file or directory: 'lib'`, which does not point
at the cause.

`otool -L` on the result shows the dynamic link landed:

```
libraygui.dylib:
	@rpath/libraylib.600.dylib (compatibility version 600.0.0, current version 6.0.0)
	/System/Library/Frameworks/CoreVideo.framework/...
	/System/Library/Frameworks/IOKit.framework/...
	/System/Library/Frameworks/Cocoa.framework/...
```

and `nm -gU ... | grep -c ' T _Gui'` reports **61**, matching the 61
`RAYGUIAPI` declarations in the vendored header.

## Using raygui from another project

`raygui-sys` is an ordinary jank package. Install it:

```sh
bb sys:install
```

Then depend on it, and that is the whole setup:

```clojure
:dependencies [[net.b12n/raygui-sys "0.1.0-SNAPSHOT"]]
```

The consuming project needs no `vendor/` directory and no `jank-build.bb` of
its own. `raygui-examples/` is exactly that arrangement, and it is what makes
restoring real raygui controls to `b12n-raylib-jnk` a one-line change rather
than a second vendoring.

Two things make the package work, and both are easy to leave out:

```clojure
:prep-tasks []                 ; or lein install aborts demanding bwrap
:verbatim-paths ["vendor"]     ; or the jar ships no C for a consumer to build
```

Leiningen's default `:prep-tasks` runs `compile`, which the jank middleware
aliases to `lein jank compile`, which wants the bwrap sandbox. macOS has no
bwrap, so the failure reads like a missing system dependency rather than a
task-ordering problem. `jank-build.bb` itself rides along automatically; the
middleware adds it to `:verbatim-paths` for you.

## A cache that will hand you a stale library

Worth knowing before you edit any of the vendored C, because nothing warns you.

lein-jank keys a dependency's output directory on `(fingerprint subtree-ops)`,
the fingerprint of its *descendants'* build steps, rather than its own source.
Change `raygui-sys`'s C and you get a new jar and a freshly extracted source
directory, but the output directory is unchanged, `is-already-built?` finds its
cache file, and `jank-build.bb` never runs.

Measured here on 2026-08-26: after a content edit to `raygui_impl.c`, the
`.dylib` stayed thirteen minutes older than the source it was supposedly built
from, and the build printed `Extracting` without ever printing `Compiling`.

`bb sys:install` drops the consumer's cached output for this package, which
forces the rebuild. Editing the `.jank` sources is unaffected, since those are
compiled from the re-extracted classpath every run. Only vendored C bites.

## macOS needs `--disable-sandbox`

There is no bwrap on macOS, so lein-jank's build sandbox cannot work there at
all. Every `lein` invocation in `bb.edn` already passes the flag. Running lein
by hand needs it too:

```sh
cd raygui-examples && lein with-profile +basic-controls run --disable-sandbox
```

## Linux

The `.so` branch of `jank-build.bb` swaps `-dynamiclib` for `-shared` and drops
the four macOS frameworks. **It has never been run.** Nobody has built this
repo on Linux, so treat those flags as a starting point rather than a tested
path.

## The vendored revision

`raygui-sys/vendor/raygui.h` is pinned at `5.0-9-gfbf5d95`, zlib licensed, and
committed unmodified. Pinning means the suite cannot break when upstream moves,
and it also means fixes do not arrive on their own. `NOTICE` records the
revision.

That revision is nine commits past the 5.0 release, and its API differs from
what the 5.0 documentation describes in at least three places:
`GuiMessageBox` and `GuiTextInputBox` carry an extra `int *btnActive`, and
`GuiTabBar` takes a semicolon-separated string with an `hscroll` cell rather
than an array with a count. Write bindings from the header in `vendor/`, never
from the published docs or from the jolt port's Clojure.
