(ns ok-voice.text
  (:require [babashka.process :as p]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [ok-voice.deps :as deps]))

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

(defn- wayland-clipboard-text []
  (try
    (let [{:keys [exit out]}
          (p/sh "wl-paste" "--no-newline" "--type" "text")]
      (when (zero? exit)
        out))
    (catch Exception _ nil)))

(defn- wayland-set-clipboard! [text]
  ;; wl-copy forks a selection owner, like xclip. Redirect inherited streams so
  ;; callers do not wait on pipes held open by the background process.
  (p/sh {:in (str text)
         :out null-device
         :err null-device}
        "wl-copy" "--type" "text/plain;charset=utf-8"))

(defn- wayland-paste-key! []
  ;; ydotool uses YDOTOOL_SOCKET when configured and otherwise discovers the
  ;; daemon socket from XDG_RUNTIME_DIR.
  (p/sh "ydotool" "key" "42:1" "110:1" "110:0" "42:0"))

(defn- wayland-paste-text! [text]
  (let [previous-text (wayland-clipboard-text)]
    (try
      (wayland-set-clipboard! text)
      (Thread/sleep 50)
      (wayland-paste-key!)
      (Thread/sleep 100)
      (finally
        (when (some? previous-text)
          (wayland-set-clipboard! previous-text)))))
  nil)


(defn paste-text!
  "Pastes UTF-8 text into the focused window and restores textual clipboard data."
  [text window-id]
  (when (and text (seq (str text)))
    (if deps/wayland?
      (wayland-paste-text! text)
      (let [previous-text (clipboard-text)
            shortcut (paste-shortcut window-id)]
        (try
          (set-clipboard! text)
          (Thread/sleep 50)
          (p/sh "xdotool" "key" "--clearmodifiers" shortcut)
          (Thread/sleep 100)
          (finally
            (when (some? previous-text)
              (set-clipboard! previous-text))))))
    nil))
