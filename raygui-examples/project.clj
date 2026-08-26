(defproject raygui-examples "0.1.0-SNAPSHOT"
  :license {:name "zlib" :url "https://opensource.org/license/zlib"}
  :plugins [[org.jank-lang/lein-jank "2026.06-1"]]
  :middleware [leiningen.jank/middleware]
  :dependencies [[net.b12n/raygui-sys "0.1.0-SNAPSHOT"]]
  :main net.b12n.raygui-jnk.basic-controls
  :jank {:target-dir         "target"
         :optimization-level 2}
  :profiles {:check          {:main net.b12n.raygui-jnk.check}
             :basic-controls {:main net.b12n.raygui-jnk.basic-controls}
             :toggles         {:main net.b12n.raygui-jnk.toggles}
             :labels-lines    {:main net.b12n.raygui-jnk.labels-lines}
             :icon-buttons    {:main net.b12n.raygui-jnk.icon-buttons}
             :text-box           {:main net.b12n.raygui-jnk.text-box}
             :text-input-box     {:main net.b12n.raygui-jnk.text-input-box}
             :spinner-value-box  {:main net.b12n.raygui-jnk.spinner-value-box}
             :sliders            {:main net.b12n.raygui-jnk.sliders}
             :progress-bar       {:main net.b12n.raygui-jnk.progress-bar}
             :dropdown-box       {:main net.b12n.raygui-jnk.dropdown-box}
             :combo-box          {:main net.b12n.raygui-jnk.combo-box}
             :list-view          {:main net.b12n.raygui-jnk.list-view}
             :list-view-ex       {:main net.b12n.raygui-jnk.list-view-ex}
             :tab-bar            {:main net.b12n.raygui-jnk.tab-bar}
             :panel-group-box    {:main net.b12n.raygui-jnk.panel-group-box}
             :scroll-panel       {:main net.b12n.raygui-jnk.scroll-panel}
             :window-box         {:main net.b12n.raygui-jnk.window-box}
             :floating-window    {:main net.b12n.raygui-jnk.floating-window}
             :message-box        {:main net.b12n.raygui-jnk.message-box}
             :custom-input-box   {:main net.b12n.raygui-jnk.custom-input-box}
             :color-picker       {:main net.b12n.raygui-jnk.color-picker}
             :color-picker-hsv   {:main net.b12n.raygui-jnk.color-picker-hsv}
             :style-selector     {:main net.b12n.raygui-jnk.style-selector}
             :gui-state          {:main net.b12n.raygui-jnk.gui-state}})
