(ns ok-voice.notify
  (:require [babashka.process :refer [sh]]))

(defn error [title body]
  (sh "notify-send" "-u" "critical" title body))

(defn info [title body]
  (sh "notify-send" "-u" "normal" title body))
