(ns ok-voice.core
  (:require [ok-voice.config :as config]
            [ok-voice.deps :as deps]
            [ok-voice.notify :as notify]))

(defn -main [& args]
  (deps/check!)
  (let [cfg (config/load!)
        api-key (config/validate-api-key! cfg)]
    (notify/info "ok-voice" "ok-voice ready")
    (println "ok-voice ready")
    (System/exit 0)))

(-main)
