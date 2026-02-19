(ns ok-voice.core
  (:require [ok-voice.config :as config]
            [ok-voice.deps :as deps]
            [ok-voice.notify :as notify]
            [ok-voice.transcription :as transcription]))

(defn -main [& args]
  (deps/check!)
  (let [cfg (config/load!)
        api-key (config/validate-api-key! cfg)]
    (notify/info "ok-voice" "Recording started...")
    (let [pipeline (transcription/start! api-key)]
      (when-not pipeline
        (System/exit 1))
      (.addShutdownHook (Runtime/getRuntime)
        (Thread. (fn []
                   (transcription/stop! pipeline)
                   (let [text (transcription/get-text)]
                     (when (seq text)
                       (println)
                       (println "--- Transcription ---")
                       (println text))))))
      (deref (promise)))))

(-main)
