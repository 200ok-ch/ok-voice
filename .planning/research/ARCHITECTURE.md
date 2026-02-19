# Architecture Research

**Domain:** Linux CLI Voice-to-Text with WebSocket Streaming
**Researched:** 2026-02-19
**Confidence:** HIGH (official docs for all major components)

## Standard Architecture

### System Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           EXTERNAL TRIGGER LAYER                             │
│                         (xbindkeys / user hotkey)                           │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           APPLICATION LAYER                                  │
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐  ┌─────────────┐  │
│  │  State Manager │  │ Config Loader │  │  Notifier     │  │   Logger    │  │
│  │  (toggle mode) │  │   (API key)   │  │ (libnotify)   │  │  (stderr)   │  │
│  └───────┬───────┘  └───────────────┘  └───────────────┘  └─────────────┘  │
│          │                                                                   │
│          ▼                                                                   │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                        CORE ORCHESTRATOR                               │  │
│  │   - Manages toggle state (idle ↔ recording)                           │  │
│  │   - Coordinates audio capture and WebSocket streaming                 │  │
│  │   - Buffers transcription text for output                             │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
          │                           │                           │
          ▼                           ▼                           ▼
┌─────────────────┐     ┌─────────────────────────┐     ┌─────────────────────┐
│   AUDIO LAYER   │     │    NETWORK LAYER         │     │   OUTPUT LAYER      │
│  ┌───────────┐  │     │  ┌─────────────────┐    │     │  ┌───────────────┐  │
│  │ PipeWire/ │  │     │  │  WebSocket      │    │     │  │   xdotool     │  │
│  │ PulseAudio│  │────▶│  │  Connection     │────│────▶│  │   type        │  │
│  │ (parecord)│  │     │  │  (to OpenAI)    │    │     │  │  (text out)   │  │
│  └───────────┘  │     │  └─────────────────┘    │     │  └───────────────┘  │
│                 │     │           │              │     │                     │
│  Audio: PCM     │     │           ▼              │     │  Output: X11       │
│  24kHz mono     │     │  ┌─────────────────┐    │     │  keyboard input    │
│  s16le          │     │  │ Event Handler   │    │     │                     │
└─────────────────┘     │  │ (transcription) │    │     └─────────────────────┘
                        │  └─────────────────┘    │
                        └─────────────────────────┘
                                    │
                                    ▼
                        ┌─────────────────────────┐
                        │   OpenAI Realtime API   │
                        │   (gpt-4o-transcribe)   │
                        └─────────────────────────┘
```

### Component Responsibilities

| Component | Responsibility | Typical Implementation |
|-----------|----------------|------------------------|
| State Manager | Toggle state (idle ↔ recording), process singleton check | PID file lock or D-Bus |
| Config Loader | Load API key from config file | EDN/YAML file in `~/.config/ok-voice/` |
| Audio Capture | Record microphone audio in PCM format | `parecord` or `pw-record` subprocess |
| WebSocket Client | Bidirectional streaming to OpenAI | HTTP client with WebSocket support |
| Event Handler | Process transcription events from API | Parse JSON events, buffer text |
| Text Output | Insert text at cursor position | `xdotool type` |
| Notifier | Desktop notifications for errors | `notify-send` via libnotify |

## Recommended Project Structure

```
src/
├── ok_voice/
│   ├── core.clj           # Entry point, toggle logic, main loop
│   ├── config.clj         # Config file loading (API key)
│   ├── audio.clj          # Audio capture via parecord/pw-record
│   ├── websocket.clj      # WebSocket connection to OpenAI
│   ├── transcription.clj  # Handle transcription events
│   ├── output.clj         # xdotool text insertion
│   ├── state.clj          # State management (recording vs idle)
│   └── notify.clj         # Desktop notifications
├── ok_voice.bb            # Babashka entry point (optional)
resources/
├── config.example.edn     # Example configuration
test/
├── ok_voice/
│   └── ..._test.clj       # Unit tests
```

### Structure Rationale

- **core.clj:** Single entry point handles toggle mode - check if already running, then either start or stop
- **audio.clj:** Isolated audio capture - can swap between parecord/pw-record based on system
- **websocket.clj:** Pure WebSocket handling - separate from transcription logic
- **transcription.clj:** Event parsing and text buffering - decoupled from output method
- **output.clj:** xdotool wrapper - could be swapped for Wayland later if needed

## Architectural Patterns

### Pattern 1: Toggle Mode with Process Singleton

**What:** Single process manages recording state. External trigger (xbindkeys) always calls same binary. First call starts, second call stops.

**When to use:** When hotkey handling is external and you want press-to-toggle behavior.

**Trade-offs:** 
- (+) Simple external integration - no args needed
- (+) No background daemon required
- (-) Requires IPC or process state check

**Example:**
```clojure
;; core.clj
(defn -main [& args]
  (if (already-running?)
    ;; Signal running process to stop and exit
    (do
      (send-stop-signal)
      (System/exit 0))
    ;; Otherwise, start recording
    (do
      (acquire-lock)
      (start-recording)
      (wait-for-transcription)
      (type-result)
      (release-lock))))
```

### Pattern 2: Pipeline Streaming (Audio → WebSocket → Text)

**What:** Audio chunks flow through pipeline: capture → base64 encode → WebSocket send. Transcription events flow back: receive → parse → buffer → output.

**When to use:** Real-time streaming with bidirectional data flow.

**Trade-offs:**
- (+) Low latency - audio streamed as captured
- (+) Clean separation of concerns
- (-) Must handle backpressure if API lags

**Example:**
```clojure
;; Audio → WebSocket
(defn audio->websocket [ws]
  (fn [audio-chunk]
    (let [base64-audio (base64-encode audio-chunk)]
      (ws/send! ws {:type "input_audio_buffer.append"
                    :audio base64-audio}))))

;; WebSocket → Text buffer
(defn handle-transcription-event [event text-buffer]
  (case (:type event)
    "conversation.item.input_audio_transcription.delta"
    (swap! text-buffer str (:delta event))
    
    "conversation.item.input_audio_transcription.completed"
    (swap! text-buffer str (:transcript event))))
```

### Pattern 3: Config-First Initialization

**What:** Load all config at startup, fail fast if missing required values (API key).

**When to use:** CLI tools with minimal runtime configuration.

**Trade-offs:**
- (+) Early failure detection
- (+) No config checking in hot path
- (-) Less flexible than runtime config

**Example:**
```clojure
;; config.clj
(defn load-config! []
  (let [config-file (io/file (System/getenv "HOME") ".config/ok-voice/config.edn")]
    (when-not (.exists config-file)
      (notify/error "Config not found" "Create ~/.config/ok-voice/config.edn")
      (System/exit 1))
    (let [cfg (edn/read-string (slurp config-file))]
      (when-not (:api-key cfg)
        (notify/error "Missing API key" "Add :api-key to config.edn")
        (System/exit 1))
      cfg)))
```

## Data Flow

### Request Flow (Recording to Transcription)

```
[xbindkeys hotkey]
        │
        ▼
[ok-voice process starts]
        │
        ├─── Check PID lock ───▶ Already running? ──▶ Send STOP signal ──▶ Exit
        │                              │ No
        │                              ▼
        │                    Acquire PID lock
        │                              │
        │                              ▼
        │                    Load config (API key)
        │                              │
        │                              ▼
        │                    Connect WebSocket to OpenAI
        │                              │
        │                              ▼
        │                    Start parecord subprocess
        │                              │
        ▼                              ▼
[User speaks]  ──────────▶  Audio chunks (PCM s16le 24kHz)
                                      │
                                      ▼
                            Base64 encode
                                      │
                                      ▼
                            WebSocket send: input_audio_buffer.append
                                      │
                                      ▼
                            OpenAI processes audio
                                      │
                                      ▼
                            Receive: transcription.delta events
                                      │
                                      ▼
                            Buffer text incrementally
```

### Response Flow (Transcription to Output)

```
[OpenAI sends transcription events]
                │
                ▼
    conversation.item.input_audio_transcription.delta
                │
                ▼
        Append :delta to buffer atom
                │
                ▼
    (repeat until STOP signal or completion)
                │
                ▼
    User presses hotkey again
                │
                ▼
        Stop audio capture
        Close WebSocket
                │
                ▼
        Get final buffer text
                │
                ▼
        xdotool type "$text"
                │
                ▼
        Release PID lock
        Exit
```

### Key Data Flows

1. **Audio capture flow:** `parecord --raw --rate=24000 --channels=1 --format=s16le` → stdout → base64 → WebSocket
2. **Transcription flow:** WebSocket JSON events → parse → accumulate text → xdotool type
3. **State flow:** PID file `/tmp/ok-voice.pid` → check on start → create if not exists → delete on exit

## Scaling Considerations

| Scale | Architecture Adjustments |
|-------|--------------------------|
| Single user (MVP) | Single process, synchronous, no buffering needed |
| Multiple recordings | Add transcript buffer per session, cleanup on exit |
| Extended sessions | Add audio chunk queue, handle WebSocket reconnects |

### Scaling Priorities

1. **First bottleneck:** Network latency causing audio buffer overflow
   - Fix: Add bounded queue, drop old chunks if needed

2. **Second bottleneck:** Long transcription sessions accumulating too much text
   - Fix: Stream output incrementally (type as you go) vs. batch at end

## Anti-Patterns

### Anti-Pattern 1: Blocking on Audio Capture

**What people do:** Read audio synchronously, blocking the main thread.

**Why it's wrong:** Can't respond to stop signals quickly. User must wait for buffer to drain.

**Do this instead:** Use async/threads or subprocess with signal handling:
```clojure
;; Use a subprocess that can be killed
(def audio-process (atom nil))

(defn start-audio []
  (reset! audio-process (shell/sh "parecord" "--raw" "--rate=24000" ...)))

(defn stop-audio []
  (when-let [proc @audio-process]
    (.destroy proc)  ; Send SIGTERM
    (reset! audio-process nil)))
```

### Anti-Pattern 2: Storing API Key in Environment

**What people do:** Read `OPENAI_API_KEY` from environment.

**Why it's wrong:** Not discoverable, harder to debug config issues.

**Do this instead:** Explicit config file with validation:
```clojure
;; Good: Config file with validation
(defn load-config! []
  (let [cfg (read-config-file)]
    (validate-config! cfg)  ; Throws with helpful message
    cfg))
```

### Anti-Pattern 3: Not Handling WebSocket Disconnects

**What people do:** Assume WebSocket stays connected during recording.

**Why it's wrong:** Network issues will hang the app, no transcription output.

**Do this instead:** Add reconnect logic or timeout with notification:
```clojure
;; Add heartbeat/timeout detection
(defn with-websocket-timeout [ws timeout-ms]
  (future
    (Thread/sleep timeout-ms)
    (when-not @transcription-complete?
      (notify/error "Connection timeout" "Lost connection to API")
      (System/exit 1))))
```

## Integration Points

### External Services

| Service | Integration Pattern | Notes |
|---------|---------------------|-------|
| OpenAI Realtime API | WebSocket to `wss://api.openai.com/v1/realtime` | Auth via Bearer token in header |
| PipeWire/PulseAudio | Subprocess `parecord` or `pw-record` | Raw PCM to stdout |
| X11/xdotool | Subprocess `xdotool type` | Text from transcription buffer |
| libnotify | Subprocess `notify-send` | Error notifications only |

### Internal Boundaries

| Boundary | Communication | Notes |
|----------|---------------|-------|
| core ↔ audio | Function call / subprocess stdout | Audio chunks as byte arrays |
| audio ↔ websocket | Core orchestrates data flow | Core passes audio-chunks to ws-send |
| websocket ↔ transcription | Core dispatches events | Event map passed to handler |
| transcription ↔ output | Atom buffer + function call | Text string to xdotool |

## Build Order Implications

Based on component dependencies, recommended implementation order:

### Phase 1: Foundation
1. **config.clj** - Config loading (no dependencies)
2. **notify.clj** - Error notifications (only needs notify-send)

### Phase 2: Core Flow
3. **state.clj** - PID lock, toggle state management
4. **core.clj** - Entry point skeleton (toggle logic only)

### Phase 3: Audio Path
5. **audio.clj** - Audio capture subprocess
6. **websocket.clj** - WebSocket connection, event parsing
7. **transcription.clj** - Event handling, text buffering

### Phase 4: Output
8. **output.clj** - xdotool text insertion
9. **core.clj** - Complete integration

### Dependency Graph
```
config ──────────────────────────────────────┐
notify ──────────────────────────────────────┤
                                             ▼
state ────────────────────────────────────▶ core
                                             │
audio ──────────────────────────────────────┤
websocket ──────────────────────────────────┤
transcription ◀─── websocket ───────────────┤
                                             │
output ──────────────────────────────────────┘
```

## Sources

- OpenAI Realtime API WebSocket: https://platform.openai.com/docs/guides/realtime-websocket
- OpenAI Realtime Transcription: https://platform.openai.com/docs/guides/realtime-transcription
- PipeWire Documentation: https://docs.pipewire.org/
- PulseAudio Utils (parecord): https://manpages.ubuntu.com/manpages/jammy/man1/parecord.1.html
- xdotool Manual: `man xdotool`

---
*Architecture research for: Linux CLI voice-to-text*
*Researched: 2026-02-19*
