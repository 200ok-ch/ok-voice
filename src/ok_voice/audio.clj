(ns ok-voice.audio
  (:require [babashka.process :as p])
  (:import [java.util Base64]))

;; Audio format constants — matches OpenAI Realtime API requirements
(def ^:private sample-rate 24000)
(def ^:private channels 1)
(def ^:private bytes-per-sample 2)
(def ^:private chunk-ms 100)
(def ^:private chunk-size
  (* sample-rate channels bytes-per-sample (/ chunk-ms 1000)))  ;; = 4800 bytes

;; Base64 encoder for converting raw PCM to wire format
(def ^:private encoder (Base64/getEncoder))

(defn- encode-base64 [^bytes ba]
  (.encodeToString encoder ba))

(defn start!
  "Spawns pw-record capturing PCM16 24kHz mono to stdout.
   Returns the process object. Caller decides when/how to read."
  []
  (p/process ["pw-record" "--raw" "--rate=24000" "--channels=1" "--format=s16" "-"]
             {:out :stream :err :inherit}))

(defn stop!
  "Destroys the pw-record process tree cleanly. Returns nil."
  [audio-proc]
  (p/destroy-tree audio-proc)
  nil)

(defn read-chunk
  "Reads one chunk (100ms) of audio from pw-record stdout.
   Returns a base64-encoded string, or nil if stream is closed."
  [audio-proc]
  (let [buf (byte-array chunk-size)
        in  ^java.io.InputStream (:out audio-proc)
        n   (.read in buf)]
    (when (pos? n)
      (let [chunk (if (= n chunk-size)
                    buf
                    (java.util.Arrays/copyOf buf n))]
        (encode-base64 chunk)))))

(defn stream-chunks!
  "Loops reading chunks and calling (callback base64-chunk).
   Blocks until stream closes or pw-record is destroyed.
   Caller should run this in a future."
  [audio-proc callback]
  (try
    (loop []
      (when-let [chunk (read-chunk audio-proc)]
        (callback chunk)
        (recur)))
    (catch java.io.IOException _
      nil)))
