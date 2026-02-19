(ns ok-voice.text
  (:require [babashka.process :as p]))

(defn activate-window!
  "Activates and raises an X11 window by ID."
  [window-id]
  (when window-id
    (p/sh "xdotool" "windowactivate" "--sync" window-id)))

(defn type-text!
  "Types text into the currently focused window via xdotool."
  [text]
  (when (and text (seq (str text)))
    (p/sh "xdotool" "type" "--clearmodifiers" "--delay" "0" "--" (str text))
    nil))
