(ns ok-voice.audio
  (:require [babashka.process :as p]))

(defn start!
  "Starts recording a 16 kHz mono WAV file for Whisper."
  []
  (let [file (doto (java.io.File/createTempFile "ok-voice-" ".wav")
               (.deleteOnExit))
        process (p/process ["parecord"
                            "--file-format=wav"
                            "--rate=16000"
                            "--channels=1"
                            "--format=s16le"
                            (.getAbsolutePath file)]
                           {:out :inherit :err :inherit})]
    {:process process
     :file file}))

(defn stop!
  "Stops recording and waits for parecord to finish the WAV header."
  [{:keys [process]}]
  (when process
    (when (p/alive? process)
      (p/destroy-tree process))
    (deref process 5000 nil))
  nil)

(defn delete! [{:keys [file]}]
  (when file
    (.delete ^java.io.File file))
  nil)
