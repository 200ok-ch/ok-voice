(ns ok-voice.transcription
  (:require [ok-voice.audio :as audio]
            [ok-voice.websocket :as ws]
            [ok-voice.notify :as notify]))

;; Pipeline state
(def ^:private text-buffer (atom ""))
(def ^:private ready? (atom (promise)))
(def ^:private pipeline (atom nil))

(declare stop!)

(defn start!
  "Connects to OpenAI, starts audio capture, and begins streaming transcription.
   Returns pipeline map on success, nil on failure."
  [api-key]
  (reset! text-buffer "")
  (reset! ready? (promise))
  (let [websocket (ws/connect! api-key
                    {:on-ready
                     (fn []
                       (deliver @ready? true)
                       (binding [*out* *err*]
                         (println "[ready]")))

                     :on-delta
                     (fn [delta]
                       (swap! text-buffer str delta)
                       (print delta)
                       (flush))

                     :on-completed
                     (fn [_transcript]
                       (println))

                     :on-speech-started
                     (fn [])

                     :on-speech-stopped
                     (fn [])

                     :on-error
                     (fn [msg]
                       (binding [*out* *err*]
                         (println "[error]" msg))
                       (notify/error "ok-voice" (str "Error: " msg))
                       (when-let [p @pipeline]
                         (stop! p)))

                     :on-close
                     (fn []
                       (binding [*out* *err*]
                         (println "[disconnected]"))
                       (when-let [p @pipeline]
                         (audio/stop! (:audio-proc p))))})]
    (if (deref @ready? 10000 nil)
      (let [audio-proc    (audio/start!)
            audio-future  (future
                            (audio/stream-chunks! audio-proc
                              (fn [chunk] (ws/send-audio! websocket chunk))))
            p             {:ws websocket
                           :audio-proc audio-proc
                           :audio-thread audio-future}]
        (reset! pipeline p)
        p)
      (do
        (notify/error "ok-voice" "Failed to connect to OpenAI")
        (ws/close! websocket)
        nil))))

(defn stop!
  "Stops audio capture, closes WebSocket, returns nil.
   Safe to call with nil."
  [pipeline-map]
  (when pipeline-map
    (reset! pipeline nil)
    (audio/stop! (:audio-proc pipeline-map))
    (deref (:audio-thread pipeline-map) 5000 nil)
    (ws/close! (:ws pipeline-map))
    (Thread/sleep 100)
    nil))

(defn get-text
  "Returns the accumulated transcription text."
  []
  @text-buffer)
