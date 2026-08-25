(ns ok-voice.toggle
  (:require [babashka.process :as p]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def ^:private runtime-dir
  (or (System/getenv "XDG_RUNTIME_DIR") "/tmp"))

(def ^:private pid-path (str runtime-dir "/ok-voice.pid"))
(def ^:private stop-path (str runtime-dir "/ok-voice.stop"))

(defn- delete-file! [path]
  (let [file (io/file path)]
    (when (.exists file)
      (.delete file))))

(defn- current-pid []
  (parse-long (str/trim (:out (p/sh "sh" "-c" "echo $PPID")))))

(defn write-pid! []
  (delete-file! stop-path)
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
  (delete-file! pid-path)
  (delete-file! stop-path)
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

(defn request-stop! []
  (spit stop-path "stop")
  nil)

(defn wait-for-stop! []
  (loop []
    (if (.exists (io/file stop-path))
      (delete-file! stop-path)
      (do
        (Thread/sleep 100)
        (recur))))
  nil)
