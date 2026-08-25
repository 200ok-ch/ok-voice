(ns ok-voice.core
  (:require [babashka.process :as p]
            [clojure.string :as str]
            [ok-voice.audio :as audio]
            [ok-voice.config :as config]
            [ok-voice.deps :as deps]
            [ok-voice.notify :as notify]
            [ok-voice.text :as text]
            [ok-voice.toggle :as toggle]
            [ok-voice.transcription :as transcription]))

(defn -main [& _args]
  ;; Toggle check first: fast path for "stop" case
  (if (toggle/running-instance)
    ;; Instance running: ask it to stop and exit
    (do
      (toggle/request-stop!)
      (System/exit 0))
    ;; No instance -> start recording
    (do
      (deps/check!)
      (let [cfg (config/load!)
            api-key (config/validate-api-key! cfg)
            api-url (config/validate-api-url! cfg)
            model (config/get-model cfg)
            wid (str/trim (:out (p/sh "xdotool" "getactivewindow")))]
        (toggle/write-pid!)
        (notify/info "ok-voice" "Recording started...")
        (let [recording (audio/start!)]
          (try
            (toggle/wait-for-stop!)
            (audio/stop! recording)
            (notify/info "ok-voice" "Transcribing...")
            (let [transcript (transcription/transcribe!
                               api-url api-key model (:file recording))]
              (text/activate-window! wid)
              (text/paste-text! transcript wid)
              (notify/info "ok-voice" "Transcription complete."))
            (catch Exception e
              (binding [*out* *err*]
                (println "[error]" (.getMessage e)))
              (notify/error "ok-voice" (.getMessage e)))
            (finally
              (audio/stop! recording)
              (audio/delete! recording)
              (toggle/remove-pid!))))))))
