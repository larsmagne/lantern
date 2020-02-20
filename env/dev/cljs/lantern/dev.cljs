(ns ^:figwheel-no-load lantern.dev
  (:require
    [lantern.core :as core]
    [devtools.core :as devtools]))

(extend-protocol IPrintWithWriter
  js/Symbol
  (-pr-writer [sym writer _]
    (-write writer (str "\"" (.toString sym) "\""))))

(enable-console-print!)

(devtools.core/set-pref! :dont-detect-custom-formatters true)
(devtools/install! [:hints])

(core/init!)
