(ns gen-catalog
  "Regenerates docs/guide/example-catalog.md from the registry.

   Run with `bb scripts/gen_catalog.clj` from the repo root. The registry is
   the single source of truth for names, groups and descriptions; the prose
   notes below live here."
  (:require [clojure.string :as str]
            [examples-registry :as reg]))
(def notes
  {"basic-controls" "The smallest complete program here: a button, a label and a click counter. It proves the package built, the bounds arrived intact, and an int cell round-tripped through raygui's pointer API."
   "toggles" "Three controls that all hold a selection, with three different state shapes. `GuiToggle` owns a bool cell; `GuiToggleGroup` and `GuiToggleSlider` take their options as one semicolon-separated string and report an index through an int cell."
   "labels-lines" "The controls that draw and hold nothing: labels at two alignments, separators with and without a caption, a placeholder box, a status bar. Also the thing to watch about raygui styling, since `TEXT_ALIGNMENT` is global and has to be put back."
   "icon-buttons" "raygui carries 256 icons inside the header, so there is no image to load. `GuiIconText` composes one with a label for any control, `GuiDrawIcon` draws it directly. The scale is global and persists until it is set back."
   "text-box" "The first mutable char buffer. Edit mode is a plain bool the caller owns rather than a cell, and the screenshot prints the buffer's length so a failed readback would be visible."
   "text-input-box" "A whole dialog in one call, including an out-param that raygui 5.0's docs do not show. The masked field always draws sixteen asterisks whatever the buffer holds, because raygui masks with a fixed literal."
   "spinner-value-box" "Both hold an int cell and clamp to a range. The third spinner starts at 999 against a maximum of 50, so the screenshot shows the clamp holding rather than asserting that it does."
   "sliders" "`GuiSlider` has a draggable handle, `GuiSliderBar` fills from its left edge. Each value is drawn beside its control, so a cell that failed to round-trip would show up in the picture."
   "progress-bar" "Filled from `GetTime` rather than a frame counter. The only example that does not capture on frame 0, since an empty bar proves nothing."
   "dropdown-box" "Edit mode is a plain bool and the caller flips it. While the list is open the rest of the panel is locked and the dropdown draws last, or clicks fall through to whatever sits underneath."
   "combo-box" "The simple one. No edit mode, no overlay, no locking: clicking advances through the options and writes an int cell."
   "list-view" "Two int cells, a scroll index and an active row, over a semicolon-separated string. The scroll index is seeded non-zero, because a zero scroll looks the same as an unwired one."
   "list-view-ex" "The only control taking an array of strings rather than a joined one, and it adds a third out-param for the row under the pointer. The array is built and freed inside the wrapper, so examples pass a plain vector."
   "tab-bar" "Returns a result code rather than a tab index: the tab to close is the active one. Close buttons only appear when `TAB_CLOSE_BUTTON` is set on the tab bar."
   "panel-group-box" "Neither container clips its children or holds state. They draw a frame, and the caller places what goes inside."
   "scroll-panel" "The suite's only `Rectangle *` out-param. raygui reports the visible view back, which the example uses to scissor its content: 386 by 221 inside a 400 by 260 panel, once the scrollbars are subtracted. Ported from raygui's own example."
   "window-box" "Draws a titled frame and reports its close button, and manages nothing else. Visibility is a bool cell the caller keeps, along with a way back."
   "floating-window" "Dragging is not a raygui feature. The title-bar hit test, the offset and the movement are all the caller's, in float cells. Ported from raygui's own example. The drag itself is not covered by any gate here."
   "message-box" "Returns a result code and writes the button index into a cell: 0 for the window's own close button, then 1 upward in semicolon order. Modality is the caller's job, since raygui draws no scrim and blocks nothing."
   "custom-input-box" "The same dialog as `text-input-box`, assembled by hand from a panel, a label, a text box and two buttons. What that buys is validation: OK stays disabled while the field is empty. Ported from raygui's own example."
   "color-picker" "A `Color *` cell, seeded a distinctive orange so a failed readback could not hide. The swatch is drawn from the cell's own components, and the alpha strip sits over a chequer so transparency is visible."
   "color-picker-hsv" "Holds a `Vector3` of hue, saturation and value rather than a colour. The swatch converts back through `ColorFromHSV`, so both representations are on screen agreeing."
   "style-selector" "Cycles the six vendored themes plus raygui's built-in default. Palette and embedded font both come from the `.rgs`. Shown here with `cyber`, since the default theme shows nothing about styling. Ported from raygui's own example."
   "gui-state" "The global-state API, which has no bounds of its own. The same three controls under each forced state, then alpha, then lock. All three are global and sticky, so every row resets immediately."})
(def out (StringBuilder.))
(defn w [& xs] (.append out (str (str/join xs) "\n")))
(w "# The example catalog")
(w "")
(w "Every example in the suite, grouped the way `bb examples` groups them. Each is")
(w "one namespace under `raygui-examples/src/net/b12n/raygui_jnk/`.")
(w "")
(w "```sh")
(w "bb <name>          # run one, windowed. Q quits.")
(w "bb shot <name>     # run headless and screenshot to /tmp/<name>.png")
(w "bb examples        # this grouping, printed live")
(w "```")
(w "")
(w "`scripts/examples_registry.clj` is the single source of truth for the names and")
(w "descriptions below, and `bb examples` fails if a `bb.edn` task drifts from it.")
(w "Run that for the count that is true right now rather than this page's memory of")
(w "it. At the time of writing there are **" (count reg/examples)
   " examples across " (count reg/groups) " groups**, and the suite is complete.")
(w "")
(w "Screenshots are captured by the same `bb shot` task the gates use. None of them")
(w "shows an interaction, because no gate here can drive a mouse.")
(doseq [g reg/groups]
  (w "")
  (w "## " g)
  (w "")
  (w "| preview | `bb` name | what it shows |")
  (w "|---|---|---|")
  (doseq [row (get (reg/by-group) g)]
    (let [n (nth row 0)]
      (w "| [<img src=\"../demos/" n ".png\" width=\"200\">](../demos/" n ".png) | `" n "` | "
         (get notes n (nth row 3)) " |"))))
(w "")
(w "## The six vendored themes")
(w "")
(w "`style-selector` cycles these. Each `.rgs` carries a palette and its own")
(w "embedded font, and the font changing is the clearest sign the file loaded.")
(w "")
(w "| theme | | theme | |")
(w "|---|---|---|---|")
(w "| `ashes` | <img src=\"../demos/theme-ashes.png\" width=\"200\"> | `candy` | <img src=\"../demos/theme-candy.png\" width=\"200\"> |")
(w "| `cyber` | <img src=\"../demos/theme-cyber.png\" width=\"200\"> | `dark` | <img src=\"../demos/theme-dark.png\" width=\"200\"> |")
(w "| `sunny` | <img src=\"../demos/theme-sunny.png\" width=\"200\"> | `terminal` | <img src=\"../demos/theme-terminal.png\" width=\"200\"> |")
(spit "docs/guide/example-catalog.md" (str out))
(println "wrote docs/guide/example-catalog.md," (count reg/examples) "examples")
