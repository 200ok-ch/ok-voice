(ns ok-voice.core
  (:require [ok-voice.config :as config]
            [ok-voice.deps :as deps]
            [ok-voice.notify :as notify]
            [ok-voice.text :as text]
            [ok-voice.toggle :as toggle]
            [ok-voice.transcription :as transcription]))

(defn -main [& args]
  ;; Toggle check first — fast path for "stop" case
  (if-let [pid (toggle/running-instance)]
    ;; Instance running → signal stop and exit
    (do
      (toggle/signal-stop! pid)
      (System/exit 0))
    ;; No instance → start recording
    (do
      (deps/check!)
      (let [cfg (config/load!)
            api-key (config/validate-api-key! cfg)]
        (toggle/write-pid!)
        (notify/info "ok-voice" "Recording started...")
        (let [pipeline (transcription/start! api-key)]
          (when-not pipeline
            (toggle/remove-pid!)
            (System/exit 1))
          (.addShutdownHook (Runtime/getRuntime)
            (Thread. (fn []
                       (toggle/remove-pid!)
                       (transcription/stop! pipeline)
                       (let [result (transcription/get-text)]
                         (when (seq result)
                           (binding [*out* *err*]
                             (println "[text]" result))
                           (text/insert-at-cursor! result)))
                       (notify/info "ok-voice" "Recording stopped."))))
          (deref (promise)))))))

(-main)
