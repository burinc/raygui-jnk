(ns registration
  "The offline half of the registration check.

  Adding an example touches six places, listed in CONTRIBUTING.md. Two are
  already gated: `bb examples` compares every bb.edn task :doc against the
  registry description, and `bb readme:examples-check` wants a committed
  screenshot and a current gallery.

  This namespace covers the other three, which are exactly the ones readable
  without a compiler: the source file, the :profiles entry in
  raygui-examples/project.clj, and the :require PLUS `required` entry in
  check.jank.

  The check.jank pair is why this exists at all. Miss it and `bb check` never
  compiles your example, yet still prints success and a count. The compile
  gate goes green over an example it never looked at, which is the worst
  shape a gate can fail in. That is trap 3 in CONTRIBUTING.md."
  (:require [babashka.fs :as fs]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [examples-registry :as reg]))

(def ^:private ns-prefix "net.b12n.raygui-jnk.")

(defn source-path
  "Where an example's source must live. jank follows Clojure's underscore rule,
  so a hyphenated name maps to an underscored file."
  [root nm]
  (str (fs/path root "raygui-examples" "src" "net" "b12n" "raygui_jnk"
                (str (str/replace nm "-" "_") ".jank"))))

(defn- read-forms
  "Every top-level form in a file, read to EOF.

  A single `read-string` would stop after the first form, which in check.jank
  is the ns form. The `required` vector sits below it and would go unread, so
  the check would pass by never looking at the thing it claims to check.
  `edn/read-string` cannot be used here either: `required` holds quoted
  symbols and EDN has no quote."
  [path]
  (let [r (java.io.PushbackReader. (java.io.StringReader. (slurp path)))]
    (loop [forms []]
      (let [f (read {:read-cond :allow :eof ::eof} r)]
        (if (= f ::eof) forms (recur (conj forms f)))))))

(defn- unqualify
  "Example names from a seq of fully qualified namespace symbols. Anything
  outside this project's prefix is ignored rather than reported as a name."
  [syms]
  (set (keep (fn [s]
               (let [n (str s)]
                 (when (str/starts-with? n ns-prefix)
                   (subs n (count ns-prefix)))))
             syms)))

(defn profile-names
  "The :profiles keys in raygui-examples/project.clj, as strings.

  project.clj is one defproject form with no reader macros in it, so EDN
  reads it without needing Leiningen."
  [root]
  (let [form (edn/read-string (slurp (str (fs/path root "raygui-examples" "project.clj"))))]
    (set (map name (keys (second (drop-while (fn [x] (not= x :profiles)) form)))))))

(defn check-jank-coverage
  "The two lists in check.jank, each as a set of example names.

  Returns {:requires #{...} :required #{...}}. They are read separately on
  purpose: having one without the other is the failure this gate is for."
  [root]
  (let [forms    (read-forms (source-path root "check"))
        ns-form  (first (filter (fn [f] (and (seq? f) (= 'ns (first f)))) forms))
        requires (->> ns-form
                      (filter (fn [x] (and (seq? x) (= :require (first x)))))
                      first rest
                      (map first))
        required (->> forms
                      (filter (fn [f] (and (seq? f)
                                           (= 'def (first f))
                                           (= 'required (second f)))))
                      first last
                      ;; The reader hands back (quote sym) for each 'sym.
                      (map (fn [x] (if (and (seq? x) (= 'quote (first x))) (second x) x))))]
    {:requires (unqualify requires)
     :required (unqualify required)}))

(defn problems
  "Every registration gap, as [name explanation] pairs. Empty means clean.

  Only rows that already have a bb.edn task are checked. A registry row with
  no task is an example nobody has ported yet, which `bb examples` reports as
  pending rather than as a failure."
  [root task-names]
  (let [profiles (profile-names root)
        {:keys [requires required]} (check-jank-coverage root)
        ported   (filter (fn [row] (contains? task-names (nth row 0))) reg/examples)
        gaps     (mapcat
                  (fn [row]
                    (let [nm (nth row 0)
                          prof (nth row 1)]
                      (cond-> []
                        (not (fs/exists? (source-path root nm)))
                        (conj [nm (str "no source at " (fs/file-name (source-path root nm)))])

                        (not (contains? profiles prof))
                        (conj [nm (str "no :" prof " in raygui-examples/project.clj :profiles, "
                                       "so bb " nm " cannot run it")])

                        (not (contains? requires nm))
                        (conj [nm "missing from check.jank :require, so bb check never compiles it"])

                        (not (contains? required nm))
                        (conj [nm "missing from check.jank `required`, so bb check under-reports"]))))
                  ported)
        registered (set (map (fn [r] (nth r 0)) reg/examples))
        orphans  (->> (fs/glob (fs/path root "raygui-examples" "src") "**/*.jank")
                      (map (fn [p] (str/replace (str (fs/file-name p)) ".jank" "")))
                      (remove (fn [f] (= f "check")))
                      (map (fn [f] (str/replace f "_" "-")))
                      (remove registered)
                      (map (fn [f] [f "a source file with no row in scripts/examples_registry.clj"])))]
    (concat gaps orphans)))
