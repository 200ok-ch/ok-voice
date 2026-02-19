(ns ok-voice.config
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [ok-voice.notify :as notify]))

(def config-path
  (str (System/getProperty "user.home") "/.config/ok-voice/config.yaml"))

(defn load! []
  (let [f (io/file config-path)]
    (if (.exists f)
      (yaml/parse-string (slurp f))
      (do
        (notify/error "ok-voice" (str "Config not found: " config-path))
        (System/exit 1)))))

(defn- not-blank [s]
  (when (and s (not (clojure.string/blank? s)))
    s))

(defn get-api-key [config]
  (or (not-blank (:api-key config))
      (not-blank (System/getenv "OPENAI_API_KEY"))))

(defn validate-api-key! [config]
  (if-let [key (get-api-key config)]
    key
    (do
      (notify/error "ok-voice" "No API key found. Set api-key in config or OPENAI_API_KEY env var.")
      (System/exit 1))))
