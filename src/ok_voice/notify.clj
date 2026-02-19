(ns ok-voice.notify
  (:require [babashka.process :refer [sh]]))

(defn- send-notification [urgency title body]
  (try
    (sh "notify-send" "-u" urgency title body)
    (catch Exception _
      (binding [*out* *err*]
        (println (str "[" urgency "] " title ": " body))))))

(defn error [title body]
  (send-notification "critical" title body))

(defn info [title body]
  (send-notification "normal" title body))
