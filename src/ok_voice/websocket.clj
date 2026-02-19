(ns ok-voice.websocket
  (:require [babashka.http-client.websocket :as ws]
            [cheshire.core :as json]
            [ok-voice.notify :as notify]))

(def ^:private api-url "wss://api.openai.com/v1/realtime?intent=transcription")

(defn- session-config []
  {"type" "transcription_session.update"
   "session" {"input_audio_format" "pcm16"
              "input_audio_noise_reduction" {"type" "near_field"}
              "input_audio_transcription"
                {"model" "gpt-4o-mini-transcribe"}
              "turn_detection"
                {"type" "server_vad"
                 "threshold" 0.5
                 "prefix_padding_ms" 300
                 "silence_duration_ms" 500}}})

(defn- dispatch-event [ws event handlers]
  (case (:type event)
    ("session.created" "transcription_session.created")
    (ws/send! ws (json/generate-string (session-config)))

    ("session.updated" "transcription_session.updated")
    (when-let [f (:on-ready handlers)] (f))

    "conversation.item.input_audio_transcription.delta"
    (when-let [f (:on-delta handlers)] (f (:delta event)))

    "conversation.item.input_audio_transcription.completed"
    (when-let [f (:on-completed handlers)] (f (:transcript event)))

    "input_audio_buffer.speech_started"
    (when-let [f (:on-speech-started handlers)] (f))

    "input_audio_buffer.speech_stopped"
    (when-let [f (:on-speech-stopped handlers)] (f))

    "conversation.item.input_audio_transcription.failed"
    (when-let [f (:on-error handlers)]
      (f (get-in event [:error :message])))

    "error"
    (let [msg (get-in event [:error :message])]
      (notify/error "ok-voice" (str "API error: " msg))
      (when-let [f (:on-error handlers)] (f msg)))

    nil))

(defn connect! [api-key handlers]
  (let [msg-buf (atom (StringBuilder.))]
    (ws/websocket
      {:uri api-url
       :headers {"Authorization" (str "Bearer " api-key)
                 "OpenAI-Beta" "realtime=v1"}
       :on-open (fn [_ws] nil)
       :on-message (fn [ws data last?]
                     (.append @msg-buf data)
                     (when last?
                       (let [full (str @msg-buf)]
                         (reset! msg-buf (StringBuilder.))
                         (let [event (json/parse-string full true)]
                           (dispatch-event ws event handlers)))))
       :on-close (fn [_ws _status _reason]
                   (when-let [f (:on-close handlers)] (f)))
       :on-error (fn [_ws err]
                   (when-let [f (:on-error handlers)]
                     (f (.getMessage err))))})))

(defn send-audio! [ws base64-chunk]
  (ws/send! ws (json/generate-string {"type" "input_audio_buffer.append"
                                       "audio" base64-chunk})))

(defn close! [ws]
  (ws/close! ws 1000 "done"))
