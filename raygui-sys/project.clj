(defproject net.b12n/raygui-sys "0.1.0-SNAPSHOT"
  :description "raygui built for jank, plus the shared wrapper namespace."
  :url "https://github.com/burinc/raygui-jnk"
  :license {:name "zlib"
            :url  "https://opensource.org/license/zlib"}
  :plugins [[org.jank-lang/lein-jank "2026.06-1"]]
  :middleware [leiningen.jank/middleware]
  :dependencies [[org.jank-lang.commons/raylib-sys "2026.08-2"]]
  :jank {:target-dir         "target"
         :optimization-level 2}
  ;; Leiningen's default :prep-tasks runs "compile", which the jank middleware
  ;; aliases to `lein jank compile` -> a sandboxed native build -> aborts on
  ;; macOS, which has no bwrap. This package ships source and vendored C only;
  ;; there is nothing to prep. Without this, `lein install` fails with a bwrap
  ;; message that reads like a missing system dependency.
  :prep-tasks []
  ;; vendor/ must ride into the jar so a CONSUMER of this package can build
  ;; raygui without carrying its own copy. jank-build.bb is added to
  ;; :verbatim-paths automatically by the jank middleware.
  :verbatim-paths ["vendor"])
