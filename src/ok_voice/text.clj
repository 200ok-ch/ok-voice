(ns ok-voice.text
  (:require [babashka.process :as p]))

(defn insert-at-cursor!
  "Inserts text at cursor position via clipboard paste.
   Sets X11 clipboard via xclip, then simulates ctrl+v via xdotool.
   When window-id is provided, focuses that window before pasting."
  [text window-id]
  (when (and text (seq (str text)))
    (p/sh {:in (str text)} "xclip" "-selection" "clipboard")
    (Thread/sleep 50)
    (when window-id
      (p/sh "xdotool" "windowfocus" "--sync" window-id))
    (p/sh "xdotool" "key" "--clearmodifiers" "ctrl+v")
    nil))
