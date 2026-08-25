(ns ok-voice.text
  (:require [babashka.process :as p]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def ^:private null-device (io/file "/dev/null"))

(defn activate-window!
  "Activates and raises an X11 window by ID."
  [window-id]
  (when window-id
    (p/sh "xdotool" "windowactivate" "--sync" window-id)))

(defn- clipboard-text []
  (try
    (let [{:keys [exit out]}
          (p/sh "xclip" "-selection" "clipboard" "-target" "UTF8_STRING" "-out")]
      (when (zero? exit)
        out))
    (catch Exception _ nil)))

(defn- set-clipboard! [text]
  ;; xclip forks a selection owner. Redirect its inherited streams so callers
  ;; do not wait forever for the background process to close captured pipes.
  (p/sh {:in (str text)
         :out null-device
         :err null-device}
        "xclip" "-selection" "clipboard" "-target" "UTF8_STRING" "-in"))

(defn- paste-shortcut [window-id]
  (let [window-class (try
                       (some-> (p/sh "xdotool" "getwindowclassname" window-id)
                               :out
                               str/trim
                               str/lower-case)
                       (catch Exception _ nil))]
    (if (= "kitty" window-class)
      "ctrl+shift+v"
      "shift+Insert")))

(defn paste-text!
  "Pastes UTF-8 text into the focused window and restores textual clipboard data."
  [text window-id]
  (when (and text (seq (str text)))
    (let [previous-text (clipboard-text)
          shortcut (paste-shortcut window-id)]
      (try
        (set-clipboard! text)
        (Thread/sleep 50)
        (p/sh "xdotool" "key" "--clearmodifiers" shortcut)
        (Thread/sleep 100)
        (finally
          (when (some? previous-text)
            (set-clipboard! previous-text)))))
    nil))
