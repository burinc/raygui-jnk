;; Run by lein-jank with the build metadata on stdin (bound to *input*).
;; Emits jank-build:: directives, which become clang flags for anything that
;; depends on this package -- and for this project itself when built at the
;; root.
(require '[babashka.fs :as fs]
         '[babashka.process :as proc]
         '[clojure.string :as str])

(let [{:keys [src-dir out-dir inputs]} *input*
      ;; NOTE: the key is FULLY QUALIFIED. "raylib-sys" returns nil and the cc
      ;; invocation then fails with "no such file or directory: 'lib'", which
      ;; does not name the cause.
      rl      (get inputs "org.jank-lang.commons/raylib-sys")
      vendor  (str (fs/path src-dir "vendor"))
      libdir  (str (fs/path out-dir "lib"))
      mac?    (str/starts-with? (System/getProperty "os.name") "Mac")
      libname (if mac? "libraygui.dylib" "libraygui.so")
      target  (fs/path libdir libname)
      impl    (fs/path vendor "raygui_impl.c")
      header  (fs/path vendor "raygui.h")]
  (when-not rl
    (binding [*out* *err*]
      (println "raygui-sys: no raylib-sys build output in :inputs -" (pr-str inputs)))
    (System/exit 1))
  (fs/create-dirs libdir)
  ;; The root project's build is marked :always-build by lein-jank, so this
  ;; script runs on EVERY invocation. Skip the compile when the library is
  ;; newer than both of its sources.
  (when (or (not (fs/exists? target))
            (< (fs/file-time->millis (fs/last-modified-time target))
               (max (fs/file-time->millis (fs/last-modified-time impl))
                    (fs/file-time->millis (fs/last-modified-time header)))))
    (proc/shell
     (concat ["cc" "-O2" (if mac? "-dynamiclib" "-shared") "-fPIC"
              "-DBUILD_LIBTYPE_SHARED"
              "-I" vendor
              "-I" (str (fs/path rl "include"))
              "-L" (str (fs/path rl "lib"))
              "-lraylib"]
             (when mac?
               ["-framework" "CoreVideo" "-framework" "IOKit"
                "-framework" "Cocoa"     "-framework" "OpenGL"
                ;; rpath to raylib so the dylib resolves at load time, and to
                ;; our own libdir so consumers find libraygui.
                "-Wl,-rpath," (str (fs/path rl "lib"))
                "-Wl,-rpath," libdir])
             ["-o" (str target) (str impl)])))
  (println (str "jank-build::include-dir=" vendor))
  (println (str "jank-build::link-dir="    libdir))
  (println (str "jank-build::link-library=" "raygui")))
