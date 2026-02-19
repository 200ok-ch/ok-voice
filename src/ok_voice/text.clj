(ns ok-voice.text
  (:require [babashka.process :as p]))

(defn insert-at-cursor!
  "Inserts text at cursor position via xdotool type.
   When window-id is provided, activates that window before typing."
  [text window-id]
  (when (and text (seq (str text)))
    (when window-id
      (p/sh "xdotool" "windowactivate" "--sync" window-id))
    (p/sh "xdotool" "type" "--clearmodifiers" "--delay" "0" "--" (str text))
    nil))
