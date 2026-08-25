(ns ok-voice.config
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [ok-voice.notify :as notify]))

(def default-api-url "https://api.openai.com/v1/audio/transcriptions")
(def default-model "whisper-1")

(def ^:private local-hosts #{"localhost" "127.0.0.1" "::1"})

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
  (when (and s (not (str/blank? s)))
    s))

(defn get-api-url [config]
  (or (not-blank (:api-url config))
      (not-blank (System/getenv "OPENAI_API_URL"))
      default-api-url))

(defn get-api-key [config]
  (or (not-blank (:api-key config))
      (not-blank (System/getenv "OPENAI_API_KEY"))))

(defn get-model [config]
  (or (not-blank (:model config))
      (not-blank (System/getenv "OPENAI_MODEL"))
      default-model))

(defn validate-api-key! [config]
  (if-let [key (get-api-key config)]
    key
    (do
      (notify/error "ok-voice"
                    "No API key found. Set api-key in config or OPENAI_API_KEY env var.")
      (System/exit 1))))

(defn validate-api-url! [config]
  (let [api-url (get-api-url config)
        uri (try
              (java.net.URI/create api-url)
              (catch IllegalArgumentException _ nil))
        scheme (some-> uri .getScheme str/lower-case)
        host (some-> uri .getHost str/lower-case)]
    (cond
      (not (and (#{"http" "https"} scheme) host))
      (do
        (notify/error "ok-voice"
                      "Invalid API URL. Set api-url to a full HTTP(S) transcription endpoint.")
        (System/exit 1))

      (and (= "http" scheme)
           (not (local-hosts host))
           (not (true? (:allow-insecure-http config))))
      (do
        (notify/error "ok-voice"
                      "Refusing to send audio and credentials over HTTP. Use HTTPS or set allow-insecure-http: true.")
        (System/exit 1))

      :else api-url)))
