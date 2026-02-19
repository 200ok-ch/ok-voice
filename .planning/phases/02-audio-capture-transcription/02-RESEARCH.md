# Phase 2: Audio Capture & Transcription - Research

**Researched:** 2026-02-19
**Domain:** PipeWire audio capture, OpenAI Realtime Transcription API (WebSocket), Babashka WebSocket client
**Confidence:** HIGH

## Summary

Phase 2 implements the core audio-to-transcription pipeline: capturing microphone audio via PipeWire's `pw-record`, streaming it over a WebSocket to OpenAI's Realtime Transcription API, and receiving streaming transcription events. The three main technical domains are well-documented and verified.

The OpenAI Realtime Transcription API uses a dedicated WebSocket endpoint with `intent=transcription` in the URL. It accepts PCM16 audio at 24kHz, base64-encoded, and returns streaming transcription deltas as JSON events. Babashka's built-in `babashka.http-client.websocket` wraps Java's `java.net.http.WebSocket`, providing callbacks for connection lifecycle and message handling. PipeWire's `pw-record` can output raw PCM to stdout using `--raw` and `-` as the filename.

A critical finding is that transcription deltas only arrive after VAD detects a speech segment boundary (silence), not during active speech. This is inherent to the API design and means "streaming" refers to getting transcription results as each utterance completes, not word-by-word during continuous speech.

**Primary recommendation:** Use `wss://api.openai.com/v1/realtime?intent=transcription` with `session.update` (NOT `transcription_session.update`) to configure a transcription session. Stream raw PCM from `pw-record --raw --rate=24000 --channels=1 --format=s16 -` as a subprocess, read stdout in chunks, base64-encode, and send via `input_audio_buffer.append`.

## Standard Stack

### Core

| Library/Tool | Version | Purpose | Why Standard |
|---|---|---|---|
| babashka.http-client.websocket | 0.4.23 (built-in) | WebSocket client to OpenAI | Built into Babashka 1.1.171+, wraps java.net.http.WebSocket, zero dependencies |
| cheshire.core | Built-in | JSON encode/decode for WebSocket events | Built into Babashka, fast and reliable |
| babashka.process | Built-in | Manage pw-record subprocess | Built into Babashka, supports streaming I/O |
| java.util.Base64 | JVM standard | Base64-encode audio chunks | Available in all JVMs, no dependency needed |
| pw-record | System package | Capture microphone audio as raw PCM | PipeWire standard tool, part of pipewire-bin |

### Supporting

| Library/Tool | Version | Purpose | When to Use |
|---|---|---|---|
| clojure.java.io | Built-in | InputStream handling from subprocess | Reading binary audio data from pw-record stdout |
| java.nio.ByteBuffer | JVM standard | Buffer management for audio chunks | If needed for WebSocket binary frame handling |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|---|---|---|
| pw-record | arecord (ALSA) | arecord works via PipeWire's ALSA plugin but pw-record is native; use arecord only on systems without PipeWire |
| pw-record | parecord (PulseAudio) | PulseAudio utils work via PipeWire's PulseAudio plugin; pw-record is more direct |
| babashka.http-client.websocket | Raw java.net.http interop | More control but more boilerplate; the babashka wrapper is sufficient |

## Architecture Patterns

### Recommended Project Structure (New Files for Phase 2)

```
src/
├── ok_voice/
│   ├── audio.clj          # pw-record subprocess management, audio chunking
│   ├── websocket.clj      # WebSocket connection to OpenAI, send/receive
│   └── transcription.clj  # Event dispatch, text buffering, session config
```

### Pattern 1: Subprocess Audio Streaming

**What:** Start pw-record as a subprocess with `:out :stream`, read its stdout InputStream in fixed-size chunks, base64-encode each chunk, and send to WebSocket.

**When to use:** Always -- this is the only way to get audio from PipeWire into Babashka.

**Example:**
```clojure
;; Source: babashka.process docs + java.util.Base64 JVM API
(require '[babashka.process :as p])

(def audio-proc
  (p/process ["pw-record" "--raw" "--rate=24000" "--channels=1" "--format=s16" "-"]
             {:out :stream :err :inherit}))

;; Read chunks from (:out audio-proc) which is a java.io.InputStream
(let [buf (byte-array 4800)  ; 100ms of audio at 24kHz 16-bit mono = 4800 bytes
      in (:out audio-proc)]
  (loop []
    (let [n (.read in buf)]
      (when (pos? n)
        (let [chunk (if (= n (alength buf)) buf (java.util.Arrays/copyOf buf n))
              b64 (.encodeToString (java.util.Base64/getEncoder) chunk)]
          ;; send b64 to WebSocket
          (send-audio! ws b64))
        (recur)))))

;; To stop:
(p/destroy-tree audio-proc)
```

### Pattern 2: WebSocket Event Dispatch

**What:** Receive JSON text frames from OpenAI, parse with cheshire, dispatch on `"type"` field to handler functions.

**When to use:** For all server event handling.

**Example:**
```clojure
;; Source: OpenAI Realtime API docs + babashka.http-client.websocket docs
(require '[cheshire.core :as json]
         '[babashka.http-client.websocket :as ws])

(defn handle-message [ws data last?]
  ;; IMPORTANT: data may be a CharSequence fragment if last? is false
  ;; Must accumulate fragments until last? is true
  (let [event (json/parse-string (str data) true)]
    (case (:type event)
      "session.created"
      (println "Session ready:" (:session event))

      "session.updated"
      (println "Session configured")

      "conversation.item.input_audio_transcription.delta"
      (on-transcription-delta event)

      "conversation.item.input_audio_transcription.completed"
      (on-transcription-completed event)

      "input_audio_buffer.speech_started"
      (on-speech-started event)

      "input_audio_buffer.speech_stopped"
      (on-speech-stopped event)

      "error"
      (on-error event)

      ;; Ignore other events
      nil)))
```

### Pattern 3: Message Fragment Accumulation

**What:** The Java WebSocket.Listener delivers text messages potentially split across multiple `onText` calls. The `last` parameter in `on-message` indicates whether the current delivery completes the message. Fragments MUST be accumulated before parsing JSON.

**When to use:** Always -- this is a requirement of the underlying Java WebSocket API.

**Example:**
```clojure
;; Source: java.net.http.WebSocket.Listener docs
(def message-buffer (atom (StringBuilder.)))

(defn on-message [ws data last?]
  (.append @message-buffer data)
  (when last?
    (let [full-msg (str @message-buffer)]
      (reset! message-buffer (StringBuilder.))
      (handle-event ws full-msg))))
```

### Anti-Patterns to Avoid

- **Parsing JSON on every on-message call without checking `last?`:** Messages can be fragmented. Always accumulate until `last?` is true.
- **Using `input_audio_buffer.commit` with server VAD enabled:** When server VAD is on, the server automatically commits the buffer when it detects speech boundaries. Manual commits with VAD on will produce errors or duplicate transcriptions.
- **Using `transcription_session.update` as the event type:** This is documented in some OpenAI docs but the API actually rejects it. Use `session.update` with `"type": "transcription"` in the session object.
- **Blocking the main thread on audio read:** Audio reading should happen in a dedicated thread/future so the WebSocket can still receive events.
- **Sending audio chunks that are too large:** Keep chunks to ~100-200ms of audio (4800-9600 bytes raw, ~6400-12800 bytes base64) for responsive VAD. Max per event is 15 MiB but small chunks improve latency.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---|---|---|---|
| Base64 encoding | Custom base64 implementation | `java.util.Base64/getEncoder` | JVM standard, zero allocation overhead with `encodeToString` |
| JSON parsing | Manual string parsing | `cheshire.core/parse-string` | Built into Babashka, handles all edge cases |
| Process management | Manual PID/signal handling | `babashka.process/process` + `destroy-tree` | Handles process trees, streams, cleanup |
| WebSocket connection | Raw Java interop | `babashka.http-client.websocket/websocket` | Clean Clojure API over java.net.http.WebSocket |
| Voice Activity Detection | Custom silence detection | OpenAI server VAD | Server-side VAD is more accurate and already integrated |
| Audio format conversion | PCM format manipulation | pw-record `--format=s16 --rate=24000` flags | Let pw-record handle resampling and format conversion |

**Key insight:** The OpenAI Realtime API handles VAD, language detection, and transcription model selection server-side. The client's job is purely to capture audio, encode it, and forward it. Don't add client-side intelligence for things the server handles.

## Common Pitfalls

### Pitfall 1: Using `transcription_session.update` Instead of `session.update`

**What goes wrong:** The API returns an error: supported types are `session.update`, `input_audio_buffer.append`, `input_audio_buffer.commit`, etc. -- `transcription_session.update` is NOT in the list.
**Why it happens:** Some OpenAI documentation examples show `transcription_session.update` but the actual API only accepts `session.update`.
**How to avoid:** Always use `"type": "session.update"` with `"session": {"type": "transcription", ...}` in the payload.
**Warning signs:** Error event received immediately after sending configuration.

### Pitfall 2: Not Accumulating WebSocket Message Fragments

**What goes wrong:** JSON parsing fails with incomplete data. The `on-message` callback receives a partial string.
**Why it happens:** Java's `java.net.http.WebSocket.Listener` may deliver a single logical text message across multiple `onText` invocations. The `last` parameter (third arg in babashka's `on-message`) indicates whether this is the final fragment.
**How to avoid:** Always buffer data in a StringBuilder and only parse when `last?` is true.
**Warning signs:** Intermittent JSON parse errors, especially on longer server responses.

### Pitfall 3: Expecting Word-by-Word Streaming During Speech

**What goes wrong:** Developer expects transcription deltas to arrive while the user is still speaking, but deltas only arrive after VAD detects silence.
**Why it happens:** The API transcribes each speech segment (between silences) as a unit. Delta events stream the transcription of a completed segment, not individual words during speech.
**How to avoid:** Set expectations correctly. The UX is: user speaks -> brief pause -> transcription appears. For continuous speech, transcription arrives in chunks at natural pause points.
**Warning signs:** User reports "nothing happens while I'm speaking" -- this is expected behavior.

### Pitfall 4: Not Handling WebSocket Close/Error Events

**What goes wrong:** pw-record keeps running after WebSocket disconnects. Audio buffer fills up. Application hangs.
**Why it happens:** Network issues, API rate limits, or API errors cause WebSocket to close but audio capture keeps going.
**How to avoid:** In `on-close` and `on-error` callbacks, always stop the audio subprocess via `destroy-tree`. Use a shared atom to signal the audio read loop to stop.
**Warning signs:** High CPU usage after connection loss, zombie pw-record processes.

### Pitfall 5: Audio Format Mismatch

**What goes wrong:** Garbled transcription or API errors.
**Why it happens:** pw-record output format doesn't match what the API expects. The API requires PCM16 at exactly 24000 Hz mono.
**How to avoid:** Always specify `--rate=24000 --channels=1 --format=s16` for pw-record and `{"type": "audio/pcm", "rate": 24000}` in the session config.
**Warning signs:** Empty transcriptions, error events from API, noise in transcription.

### Pitfall 6: Manual Commits with Server VAD Enabled

**What goes wrong:** Sending `input_audio_buffer.commit` while server VAD is enabled causes duplicate or garbled transcriptions. Accuracy degrades dramatically because commits may split words mid-utterance.
**Why it happens:** Developer tries to force faster transcription by manually committing partial audio.
**How to avoid:** When using `server_vad` turn detection, let the server handle all commits. Only use manual commits if turn detection is disabled.
**Warning signs:** Transcription quality drops, partial words appear, duplicate text segments.

## Code Examples

### Complete WebSocket Connection to OpenAI Realtime Transcription

```clojure
;; Source: OpenAI Realtime API docs + babashka.http-client.websocket API
(require '[babashka.http-client.websocket :as ws]
         '[cheshire.core :as json])

(def msg-buf (atom (StringBuilder.)))

(defn on-ws-message [ws data last?]
  (.append @msg-buf data)
  (when last?
    (let [full (str @msg-buf)]
      (reset! msg-buf (StringBuilder.))
      (let [event (json/parse-string full true)]
        (case (:type event)
          "session.created"     (configure-session! ws)
          "session.updated"     (println "Session configured, ready for audio")
          "conversation.item.input_audio_transcription.delta"
            (print (:delta event))  ;; streaming text output
          "conversation.item.input_audio_transcription.completed"
            (println "\n[completed]" (:transcript event))
          "input_audio_buffer.speech_started"
            (println "[speech started]")
          "input_audio_buffer.speech_stopped"
            (println "[speech stopped]")
          "conversation.item.input_audio_transcription.failed"
            (println "[transcription failed]" (get-in event [:error :message]))
          "error"
            (println "[error]" (get-in event [:error :message]))
          nil)))))

(defn connect! [api-key]
  (ws/websocket
    {:uri "wss://api.openai.com/v1/realtime?intent=transcription"
     :headers {"Authorization" (str "Bearer " api-key)
               "OpenAI-Beta" "realtime=v1"}
     :on-open (fn [ws] (println "Connected to OpenAI"))
     :on-message on-ws-message
     :on-close (fn [ws status reason]
                 (println "WebSocket closed:" status reason))
     :on-error (fn [ws err]
                 (println "WebSocket error:" (.getMessage err)))}))
```

### Session Configuration for Transcription

```clojure
;; Source: OpenAI Realtime Transcription API docs, community verification
(defn configure-session! [ws]
  (ws/send! ws
    (json/generate-string
      {"type" "session.update"
       "session" {"type" "transcription"
                  "input_audio_format" "pcm16"
                  "input_audio_noise_reduction" {"type" "near_field"}
                  "input_audio_transcription"
                    {"model" "gpt-4o-mini-transcribe"}
                  "turn_detection"
                    {"type" "server_vad"
                     "threshold" 0.5
                     "prefix_padding_ms" 300
                     "silence_duration_ms" 500}}})))
```

**Note on session configuration format:** There are two configuration styles observed in OpenAI docs -- a flat style (using `input_audio_format`, `input_audio_transcription`, `turn_detection` at the session level) and a nested style (using `session.audio.input.format`, `session.audio.input.transcription`, etc.). The flat style matches the `RealtimeRequestSession` schema from the API reference and is the verified working format.

### Audio Capture and Streaming

```clojure
;; Source: babashka.process docs + java.util.Base64 JVM API
(require '[babashka.process :as p])

(def ^:private base64-encoder (java.util.Base64/getEncoder))

(defn encode-base64 [^bytes byte-arr]
  (.encodeToString base64-encoder byte-arr))

(defn start-audio-capture! []
  (p/process ["pw-record" "--raw" "--rate=24000" "--channels=1" "--format=s16" "-"]
             {:out :stream :err :inherit}))

(defn stream-audio!
  "Read audio chunks from pw-record and send to WebSocket.
   chunk-size-bytes: 4800 = 100ms at 24kHz 16-bit mono (2 bytes/sample * 24000 * 0.1s)
   Returns when the InputStream is closed (process killed)."
  [audio-proc ws & {:keys [chunk-size-bytes] :or {chunk-size-bytes 4800}}]
  (let [buf (byte-array chunk-size-bytes)
        in ^java.io.InputStream (:out audio-proc)]
    (try
      (loop []
        (let [n (.read in buf)]
          (when (pos? n)
            (let [chunk (if (= n chunk-size-bytes)
                          buf
                          (java.util.Arrays/copyOf buf n))
                  b64 (encode-base64 chunk)
                  event (json/generate-string
                          {"type" "input_audio_buffer.append"
                           "audio" b64})]
              (ws/send! ws event))
            (recur))))
      (catch java.io.IOException _
        ;; Stream closed, audio capture ended
        nil))))

(defn stop-audio-capture! [audio-proc]
  (p/destroy-tree audio-proc))
```

### Chunk Size Calculation

```clojure
;; Audio math for PCM16 24kHz mono:
;; - 1 sample = 2 bytes (16-bit)
;; - 24000 samples/second
;; - 48000 bytes/second
;;
;; Chunk duration → bytes:
;; 20ms  →   960 bytes  (very responsive VAD, high overhead)
;; 50ms  →  2400 bytes  (good balance)
;; 100ms →  4800 bytes  (recommended default)
;; 200ms →  9600 bytes  (lower overhead, slightly less responsive)
;;
;; Base64 overhead: ~33% increase
;; 4800 bytes raw → ~6400 bytes base64
;; Well within the 15 MiB per-event limit
```

### Base64 Encoding in Babashka

```clojure
;; Source: java.util.Base64 JDK docs
;; java.util.Base64 is available in all JVMs that Babashka targets

(import 'java.util.Base64)

(def encoder (Base64/getEncoder))

(defn bytes->base64 [^bytes ba]
  (.encodeToString encoder ba))

;; Example:
(bytes->base64 (byte-array [0 1 2 3]))
;; => "AAECAw=="
```

### Thread Coordination Pattern

```clojure
;; Audio streaming runs in a separate thread from WebSocket event handling.
;; Use an atom to coordinate shutdown.

(def running? (atom false))

(defn start-pipeline! [api-key]
  (reset! running? true)
  (let [ws (connect! api-key)
        ;; Wait briefly for session.created + session.updated
        audio-proc (start-audio-capture!)
        audio-thread (future
                       (stream-audio! audio-proc ws))]
    {:ws ws :audio-proc audio-proc :audio-thread audio-thread}))

(defn stop-pipeline! [{:keys [ws audio-proc audio-thread]}]
  (reset! running? false)
  (stop-audio-capture! audio-proc)  ;; kills pw-record, closes InputStream
  @audio-thread                      ;; wait for audio loop to exit
  (ws/close! ws 1000 "done"))        ;; orderly WebSocket close
```

## OpenAI Realtime Transcription API - Complete Reference

### Connection

| Parameter | Value |
|---|---|
| WebSocket URL | `wss://api.openai.com/v1/realtime?intent=transcription` |
| Auth header | `Authorization: Bearer <API_KEY>` |
| Beta header | `OpenAI-Beta: realtime=v1` |
| Protocol | WebSocket (wss) |

### Session Configuration (session.update)

```json
{
  "type": "session.update",
  "session": {
    "type": "transcription",
    "input_audio_format": "pcm16",
    "input_audio_noise_reduction": {
      "type": "near_field"
    },
    "input_audio_transcription": {
      "model": "gpt-4o-mini-transcribe",
      "language": "en",
      "prompt": ""
    },
    "turn_detection": {
      "type": "server_vad",
      "threshold": 0.5,
      "prefix_padding_ms": 300,
      "silence_duration_ms": 500
    }
  }
}
```

**Configuration fields:**

| Field | Type | Required | Description |
|---|---|---|---|
| `session.type` | string | Yes (for transcription) | Must be `"transcription"` for transcription-only mode |
| `session.input_audio_format` | string | No (default pcm16) | `"pcm16"`, `"g711_ulaw"`, or `"g711_alaw"` |
| `session.input_audio_noise_reduction.type` | string | No | `"near_field"` (headphones/close mic), `"far_field"` (laptop/room mic), or null to disable |
| `session.input_audio_transcription.model` | string | No | `"gpt-4o-transcribe"`, `"gpt-4o-mini-transcribe"`, `"whisper-1"`, `"gpt-4o-transcribe-diarize"` |
| `session.input_audio_transcription.language` | string | No | ISO-639-1 code (e.g. `"en"`). Omit for auto-detection. Supplying improves accuracy and latency |
| `session.input_audio_transcription.prompt` | string | No | Guidance text. For whisper-1: keyword list. For gpt-4o models: free text |
| `session.turn_detection.type` | string | No | `"server_vad"` or `"semantic_vad"`. null to disable |
| `session.turn_detection.threshold` | number | No | 0.0-1.0, default 0.5. Higher = less sensitive |
| `session.turn_detection.prefix_padding_ms` | number | No | Audio to include before speech, default 300ms |
| `session.turn_detection.silence_duration_ms` | number | No | Silence to wait before end-of-speech, default 200-500ms |

### Language Auto-Detection

To enable auto-detection, **omit the `language` field** from `input_audio_transcription` configuration. The API will detect the spoken language automatically. Setting `language` to a specific ISO-639-1 code like `"en"` acts as a preference hint that improves accuracy and latency, but does not strictly enforce that language.

**Confidence: MEDIUM** -- The official docs don't explicitly state "omit for auto-detect" but this is the consistent behavior observed in community implementations and matches the Whisper API pattern where language is optional.

### Transcription Models

| Model | Quality | Speed | Cost | Notes |
|---|---|---|---|---|
| `gpt-4o-transcribe` | Best | Fast | Higher | Best accuracy for transcription |
| `gpt-4o-mini-transcribe` | Good | Fastest | Lower | Good balance of cost/quality (recommended for ok-voice) |
| `whisper-1` | Good | Slower | Lowest | Legacy, batch-style transcription |
| `gpt-4o-transcribe-diarize` | Best | Fast | Higher | Adds speaker diarization labels |

### Client-to-Server Events (For Transcription)

| Event Type | Purpose | Fields |
|---|---|---|
| `session.update` | Configure transcription session | `type`, `session` object |
| `input_audio_buffer.append` | Send audio chunk | `type`, `audio` (base64 string) |
| `input_audio_buffer.commit` | Manually commit audio buffer (only when VAD disabled) | `type` |
| `input_audio_buffer.clear` | Clear audio buffer | `type` |

### Server-to-Client Events (For Transcription)

| Event Type | Purpose | Key Fields |
|---|---|---|
| `session.created` | Confirms connection, returns default session | `session` object |
| `session.updated` | Confirms session configuration | `session` object |
| `input_audio_buffer.speech_started` | VAD detected speech start | `audio_start_ms`, `item_id` |
| `input_audio_buffer.speech_stopped` | VAD detected speech end | `audio_end_ms`, `item_id` |
| `input_audio_buffer.committed` | Audio buffer was committed | `item_id`, `previous_item_id` |
| `conversation.item.created` | New conversation item created | `item` object |
| `conversation.item.input_audio_transcription.delta` | Incremental transcription text | `item_id`, `content_index`, `delta` |
| `conversation.item.input_audio_transcription.completed` | Final transcription for segment | `item_id`, `content_index`, `transcript` |
| `conversation.item.input_audio_transcription.failed` | Transcription failed | `item_id`, `error` object |
| `error` | API error | `error` object with `code`, `message`, `param` |

### Session Lifecycle

```
1. Client connects to wss://api.openai.com/v1/realtime?intent=transcription
   with Authorization: Bearer <key> and OpenAI-Beta: realtime=v1

2. Server sends: session.created (with default config)

3. Client sends: session.update (with transcription config)

4. Server sends: session.updated (confirms config)

5. Client starts sending: input_audio_buffer.append (base64 audio chunks)
   (repeat continuously while recording)

6. Server VAD detects speech start:
   Server sends: input_audio_buffer.speech_started

7. Server VAD detects speech end:
   Server sends: input_audio_buffer.speech_stopped
   Server sends: input_audio_buffer.committed
   Server sends: conversation.item.created

8. Server transcribes the committed audio:
   Server sends: conversation.item.input_audio_transcription.delta (one or more)
   Server sends: conversation.item.input_audio_transcription.completed

9. Steps 5-8 repeat for each utterance

10. Client stops recording:
    Client stops sending audio
    Client sends: close frame (WebSocket close)
```

### Error Event Structure

```json
{
  "type": "error",
  "error": {
    "type": "invalid_request_error",
    "code": "invalid_value",
    "message": "Human-readable error description",
    "param": "session.input_audio_transcription.model",
    "event_id": "evt_abc123"
  }
}
```

## babashka.http-client.websocket API - Complete Reference

### Connection

```clojure
(require '[babashka.http-client.websocket :as ws])

(def my-ws
  (ws/websocket
    {:uri "wss://example.com/ws"
     :headers {"Authorization" "Bearer token123"
               "OpenAI-Beta" "realtime=v1"}
     :connect-timeout 5000  ;; milliseconds
     :subprotocols ["realtime"]  ;; optional
     :on-open    (fn [ws] ...)
     :on-message (fn [ws data last?] ...)
     :on-close   (fn [ws status reason] ...)
     :on-error   (fn [ws err] ...)}))
```

### Key Functions

| Function | Signature | Description |
|---|---|---|
| `websocket` | `(websocket opts)` | Create WebSocket connection. Returns `java.net.http.WebSocket` |
| `send!` | `(send! ws data)` or `(send! ws data opts)` | Send data. Data: String, byte[], ByteBuffer. Opts: `{:last true}` |
| `close!` | `(close! ws)` or `(close! ws status reason)` | Orderly close |
| `abort!` | `(abort! ws)` | Abrupt close |
| `ping!` | `(ping! ws data)` | Send ping frame |
| `pong!` | `(pong! ws data)` | Send pong frame |

### Callback Details

| Callback | Parameters | Notes |
|---|---|---|
| `:on-open` | `[ws]` | Called when connection established. Good place to log or set state. |
| `:on-message` | `[ws data last?]` | `data` is CharSequence (text) or ByteBuffer (binary). `last?` indicates if this completes the message. **MUST accumulate fragments when `last?` is false.** |
| `:on-close` | `[ws status reason]` | `status` is int (1000=normal), `reason` is string. Called when Close frame received. |
| `:on-error` | `[ws err]` | `err` is Throwable. Connection may be unusable after this. |

### Critical: Message Fragmentation

The underlying `java.net.http.WebSocket.Listener` may split a single text message across multiple `onText` calls. Each call provides a `CharSequence` fragment and a `last` boolean. **You MUST accumulate fragments until `last` is true before parsing JSON.** This is not optional -- it will cause intermittent failures if not handled.

## PipeWire pw-record - Complete Reference

### Command for ok-voice

```bash
pw-record --raw --rate=24000 --channels=1 --format=s16 -
```

| Flag | Value | Purpose |
|---|---|---|
| `--raw` | (no value) | Output raw PCM data without WAV header |
| `--rate` | 24000 | Sample rate in Hz (matches OpenAI API requirement) |
| `--channels` | 1 | Mono audio |
| `--format` | s16 | 16-bit signed integer samples (PCM16) |
| `-` | (filename) | Output to stdout instead of file |

### Key Options

| Option | Default | Description |
|---|---|---|
| `--rate=VALUE` | 48000 | Sample rate in Hz |
| `--channels=VALUE` | 2 | Number of channels |
| `--format=VALUE` | s16 | Sample format: u8, s8, s16, s24, s32, f32, f64 |
| `--latency=VALUE` | 100ms | Node latency. Lower = smaller buffers, higher overhead |
| `--target=VALUE` | auto | Audio source. `auto` uses default microphone |
| `--volume=VALUE` | 1.000 | Stream volume multiplier |
| `-q, --quality=VALUE` | 4 | Resampler quality (0-15) |

### Audio Math

```
PCM16 24kHz Mono:
- 1 sample = 2 bytes (16-bit signed)
- 24000 samples/second
- 48000 bytes/second = 48 KB/s
- 100ms chunk = 2400 samples = 4800 bytes
- 1 second = 48000 bytes = ~64000 bytes base64
```

### Latency Considerations

The `--latency` flag controls PipeWire's internal buffer size. Default is 100ms. For voice transcription, 100ms is fine -- lower values add CPU overhead without meaningful benefit since the API processes speech segments, not individual samples.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|---|---|---|---|
| Whisper API (batch transcription) | Realtime Transcription API (streaming) | 2025 GA | Real-time results instead of wait-for-complete |
| `parecord` (PulseAudio) | `pw-record` (PipeWire native) | 2022-2024 | PipeWire is now the Linux audio standard |
| `transcription_session.update` event | `session.update` with `"type": "transcription"` | 2025 (API evolution) | Single event type for all session modes |
| Session type in URL model param | `intent=transcription` query param | 2025 | Cleaner separation of transcription vs conversation |

**Deprecated/outdated:**
- `transcription_session.update` event type: Never actually worked despite appearing in some documentation. Use `session.update`.
- Using `model=gpt-4o-realtime-preview` for transcription: Use `intent=transcription` in URL and specify transcription model in session config.

## Open Questions

1. **`OpenAI-Beta: realtime=v1` header requirement**
   - What we know: Some examples include this header, the Azure reference includes it
   - What's unclear: Whether it's still required for GA or only for beta period
   - Recommendation: Include it to be safe; if the API rejects it, remove it
   - Confidence: MEDIUM

2. **Exact session.update format for transcription mode**
   - What we know: Two formats observed -- flat (`input_audio_format`, `input_audio_transcription` at session level) and nested (`session.audio.input.format`). The flat format matches the Azure/official reference schema.
   - What's unclear: Whether the nested format (`session.audio.input.*`) also works, or if it's documentation-only
   - Recommendation: Use the flat format which matches `RealtimeRequestSession` schema. If it fails, try the nested format.
   - Confidence: MEDIUM -- the flat format is verified against the schema but not tested live

3. **Language field omission for auto-detection**
   - What we know: The `language` field in `input_audio_transcription` accepts ISO-639-1 codes. It's listed as optional.
   - What's unclear: Whether omitting it triggers auto-detection or defaults to English
   - Recommendation: Omit it initially for auto-detection. If that doesn't work, try empty string or null.
   - Confidence: MEDIUM

## Sources

### Primary (HIGH confidence)
- [Azure OpenAI Realtime Audio Reference](https://learn.microsoft.com/en-us/azure/ai-foundry/openai/realtime-audio-reference) -- Complete event schemas, session configuration, component types (mirrors OpenAI API)
- [java.net.http.WebSocket.Listener](https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/WebSocket.Listener.html) -- Message fragmentation behavior, callback signatures
- [babashka.http-client.websocket API](https://cljdoc.org/d/org.babashka/http-client/0.4.23/api/babashka.http-client.websocket) -- WebSocket function signatures, options
- [babashka.http-client API.md](https://github.com/babashka/http-client/blob/main/API.md) -- Complete API including WebSocket details
- [babashka/process README](https://github.com/babashka/process/blob/master/README.md) -- Process management, streaming I/O, destroy-tree
- [PipeWire pw-cat man page](https://docs.pipewire.org/page_man_pw-cat_1.html) -- pw-record options, formats, raw mode
- [pw-record Debian manpage](https://manpages.debian.org/unstable/pipewire-bin/pw-record.1.en.html) -- Additional pw-record details

### Secondary (MEDIUM confidence)
- [OpenAI Realtime Transcription Guide](https://platform.openai.com/docs/guides/realtime-transcription) -- Transcription session setup (403 on direct fetch, verified via search)
- [OpenAI Realtime API Reference](https://platform.openai.com/docs/api-reference/realtime) -- Event types and schemas
- [OpenAI Client Events Reference](https://platform.openai.com/docs/api-reference/realtime-client-events) -- Client event list
- [OpenAI Server Events Reference](https://platform.openai.com/docs/api-reference/realtime-server-events) -- Server event list
- [OpenAI Developers WebSocket Guide](https://developers.openai.com/api/docs/guides/realtime-websocket/) -- Connection setup, auth
- [OpenAI Community: transcription_session.update bug](https://community.openai.com/t/why-does-transcription-session-update-cause-an-error-realtime-transcription-api-bug/1370560) -- Confirmed session.update is correct
- [OpenAI Community: streaming transcription](https://community.openai.com/t/realtime-streaming-transcription/1371205) -- Session config examples, VAD behavior
- [OpenAI Community: transcription issue](https://community.openai.com/t/realtime-transcription-issue/1150994) -- Language detection behavior

### Tertiary (LOW confidence)
- [Realtime API Medium article](https://medium.com/@anirudhgangwal/real-time-speech-transcription-with-openai-and-websockets-76eccf4fe51a) -- Implementation walkthrough (could not fetch, 403)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- all tools are built-in to Babashka or system packages, well documented
- Architecture (WebSocket events): HIGH -- verified against Azure reference docs which mirror OpenAI API
- Architecture (session config format): MEDIUM -- two formats observed, flat format matches schema but not live-tested
- Pitfalls: HIGH -- verified through community reports and official docs
- Language auto-detection: MEDIUM -- inferred from optional field documentation, not explicitly documented

**Research date:** 2026-02-19
**Valid until:** 2026-03-19 (30 days -- OpenAI API is relatively stable post-GA but may evolve)

---
*Phase 2 research for: Audio Capture & Transcription pipeline*
*Researched: 2026-02-19*
