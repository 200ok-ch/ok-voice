(ns ok-voice.deps
  (:require [babashka.process :refer [sh]]
            [clojure.string :as str]
            [ok-voice.notify :as notify]))

(def wayland?
  "True inside a Wayland session; paste must then avoid X11-only tools."
  (some? (System/getenv "WAYLAND_DISPLAY")))

(def required-deps
  (cond-> [{:cmd "parecord"   :name "PulseAudio" :install "pulseaudio-utils"}
           {:cmd "notify-send" :name "libnotify"  :install "libnotify"}]
    wayland? (into [{:cmd "wl-copy"  :name "wl-clipboard" :install "wl-clipboard"}
                    {:cmd "ydotool" :name "ydotool"       :install "ydotool (uinput)"}])
    (not wayland?) (into [{:cmd "xdotool" :name "xdotool" :install "xdotool"}
                          {:cmd "xclip"   :name "xclip"   :install "xclip"}])))

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
