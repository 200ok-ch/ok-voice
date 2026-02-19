(ns ok-voice.toggle
  (:require [babashka.process :as p]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def ^:private pid-path
  (str (or (System/getenv "XDG_RUNTIME_DIR") "/tmp")
       "/ok-voice.pid"))

(defn- current-pid []
  (parse-long (str/trim (:out (p/sh "sh" "-c" "echo $PPID")))))

(defn write-pid! []
  (spit pid-path (str (current-pid)))
  nil)

(defn read-pid []
  (try
    (when (.exists (io/file pid-path))
      (let [content (str/trim (slurp pid-path))]
        (when (seq content)
          (parse-long content))))
    (catch Exception _ nil)))

(defn remove-pid! []
  (let [f (io/file pid-path)]
    (when (.exists f)
      (.delete f)))
  nil)

(defn process-alive? [pid]
  (try
    (zero? (:exit (p/sh "kill" "-0" (str pid))))
    (catch Exception _ false)))

(defn running-instance []
  (when-let [pid (read-pid)]
    (if (process-alive? pid)
      pid
      (do (remove-pid!) nil))))

(def ^:private wid-path (str pid-path ".wid"))

(defn write-target-window! [window-id]
  (spit wid-path (str window-id))
  nil)

(defn read-target-window []
  (try
    (when (.exists (io/file wid-path))
      (let [content (str/trim (slurp wid-path))]
        (when (seq content) content)))
    (catch Exception _ nil)))

(defn remove-target-window! []
  (let [f (io/file wid-path)]
    (when (.exists f)
      (.delete f)))
  nil)

(defn signal-stop! [pid]
  (p/sh "kill" (str pid))
  nil)
