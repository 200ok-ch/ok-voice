(ns ok-voice.text
  (:require [babashka.process :as p]))

(defn insert-at-cursor!
  "Inserts text at cursor position via clipboard paste.
   Sets X11 clipboard via xclip, then simulates ctrl+v via xdotool."
  [text]
  (when (and text (seq (str text)))
    (p/sh {:in (str text)} "xclip" "-selection" "clipboard")
    (Thread/sleep 50)
    (p/sh "xdotool" "key" "--clearmodifiers" "ctrl+v")
    nil))
