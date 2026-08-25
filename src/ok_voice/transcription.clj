(ns ok-voice.transcription
  (:require [cheshire.core :as json]
            [clojure.java.io :as io])
  (:import [java.io ByteArrayOutputStream]
           [java.net URI]
           [java.net.http HttpClient HttpRequest
            HttpRequest$BodyPublishers HttpResponse$BodyHandlers]
           [java.nio.charset StandardCharsets]
           [java.time Duration]
           [java.util UUID]))

(defn- write-string! [^ByteArrayOutputStream out value]
  (.write out (.getBytes ^String value StandardCharsets/UTF_8)))

(defn- multipart-body [file model boundary]
  (let [out (ByteArrayOutputStream.)
        part (fn [name value]
               (write-string! out (str "--" boundary "\r\n"
                                       "Content-Disposition: form-data; name=\"" name "\"\r\n\r\n"
                                       value "\r\n")))]
    (part "model" model)
    (part "response_format" "json")
    (write-string! out (str "--" boundary "\r\n"
                            "Content-Disposition: form-data; name=\"file\"; filename=\"recording.wav\"\r\n"
                            "Content-Type: audio/wav\r\n\r\n"))
    (with-open [in (io/input-stream file)]
      (io/copy in out))
    (write-string! out (str "\r\n--" boundary "--\r\n"))
    (.toByteArray out)))

(defn- response-error [body status]
  (try
    (or (get-in (json/parse-string body true) [:error :message])
        (str "HTTP " status))
    (catch Exception _
      (str "HTTP " status))))

(defn transcribe!
  "Uploads a completed WAV file to an OpenAI-compatible transcription endpoint."
  [api-url api-key model file]
  (let [boundary (str "ok-voice-" (UUID/randomUUID))
        body (multipart-body file model boundary)
        client (-> (HttpClient/newBuilder)
                   (.connectTimeout (Duration/ofSeconds 15))
                   (.build))
        request (-> (HttpRequest/newBuilder (URI/create api-url))
                    (.timeout (Duration/ofMinutes 10))
                    (.header "Authorization" (str "Bearer " api-key))
                    (.header "Content-Type" (str "multipart/form-data; boundary=" boundary))
                    (.POST (HttpRequest$BodyPublishers/ofByteArray body))
                    (.build))
        response (.send client request (HttpResponse$BodyHandlers/ofString))
        status (.statusCode response)
        response-body (.body response)]
    (when-not (<= 200 status 299)
      (throw (ex-info (str "Transcription failed: " (response-error response-body status))
                      {:status status})))
    (let [text (:text (json/parse-string response-body true))]
      (when-not (string? text)
        (throw (ex-info "Transcription response did not contain text" {})))
      text)))
