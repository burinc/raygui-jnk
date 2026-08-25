(defproject raygui-examples "0.1.0-SNAPSHOT"
  :license {:name "zlib" :url "https://opensource.org/license/zlib"}
  :plugins [[org.jank-lang/lein-jank "2026.06-1"]]
  :middleware [leiningen.jank/middleware]
  :dependencies [[net.b12n/raygui-sys "0.1.0-SNAPSHOT"]]
  :main net.b12n.raygui-jnk.basic-controls
  :jank {:target-dir         "target"
         :optimization-level 2}
  :profiles {:basic-controls {:main net.b12n.raygui-jnk.basic-controls}})
