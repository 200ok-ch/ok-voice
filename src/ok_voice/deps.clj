(ns ok-voice.deps
  (:require [babashka.process :refer [sh]]
            [clojure.string :as str]
            [ok-voice.notify :as notify]))

(def required-deps
  [{:cmd "pw-record" :name "PipeWire" :install "pipewire"}
   {:cmd "xdotool"   :name "xdotool"  :install "xdotool"}
   {:cmd "notify-send" :name "libnotify" :install "libnotify"}])

(defn check-cmd [cmd]
  (try
    (let [result (sh "which" cmd)]
      (zero? (:exit result)))
    (catch Exception _
      false)))

(defn check! []
  (let [missing (remove #(check-cmd (:cmd %)) required-deps)]
    (when (seq missing)
      (let [msg (str "Missing dependencies:\n"
                     (str/join "\n"
                       (map #(str "  - " (:name %) " (install: " (:install %) ")")
                            missing)))]
        (notify/error "ok-voice" msg)
        (System/exit 1)))
    true))
