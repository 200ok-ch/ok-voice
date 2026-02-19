# Stack Research

**Domain:** Linux voice-to-text CLI application
**Researched:** 2026-02-19
**Confidence:** HIGH

## Recommended Stack

### Core Technologies

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| **Babashka** | v1.12.215 | Native Clojure runtime for CLI scripting | Fast startup (~10ms), built-in http-client with WebSocket, GraalVM-native, perfect for CLI tools. Includes cheshire (JSON), tools.cli (args). |
| **babashka.http-client** | v0.4.23 (built-in) | WebSocket client for OpenAI Realtime API | Built into Babashka 1.1.171+, wraps java.net.http WebSocket, provides clean callbacks: `:on-open`, `:on-message`, `:on-close`, `:on-error`. No dependencies. |
| **OpenAI Realtime Transcription API** | GA (2025+) | Live audio-to-text transcription | Native streaming transcription with VAD, supports `gpt-4o-transcribe` for best accuracy, `gpt-4o-mini-transcribe` for cost efficiency. Sub-second latency. |
| **PipeWire / ALSA** | System packages | Audio capture from microphone | PipeWire is the 2025 Linux audio standard. Use `pw-record` (PipeWire native) or `arecord` (ALSA, works via PipeWire's ALSA plugin). |
| **xdotool** | v3.x | X11 text insertion at cursor position | Battle-tested X11 automation. `xdotool type "text"` simulates keyboard input. Simple, reliable, works with all X11 apps. |
| **libnotify** | System package | Desktop notifications for errors | `notify-send "Title" "Message"` is the freedesktop.org standard. Available on all Linux desktops. |

### Supporting Libraries

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| **cheshire.core** | Built-in | JSON encoding/decoding | Parsing OpenAI WebSocket JSON events, encoding session config. Built into Babashka. |
| **clojure.java.io** | Built-in | File/resource I/O | Reading config file, audio stream handling. Built into Clojure/Babashka. |
| **clojure.java.shell** | Built-in | Shell command execution | Invoking `xdotool type`, `notify-send`, `pw-record`. Use `babashka.process/sh` for better control. |
| **babashka.process** | Built-in | Process management | Starting/stopping audio capture, managing child processes with streaming I/O. |

### Development Tools

| Tool | Purpose | Notes |
|------|---------|-------|
| **bb.edn** | Project dependencies & tasks | Babashka's project file. Declare deps with `:deps`, tasks with `:tasks`. |
| **clj-kondo** | Static analysis/linting | Hooks into editor for Clojure linting. Config via `.clj-kondo/config.edn`. |
| ** rebel-readline** | Enhanced REPL | Optional: better REPL experience during development. |

## Installation

```bash
# Core runtime
# Download babashka from https://github.com/babashka/babashka/releases
# Or use your distro's package manager:
# Arch: pacman -S babashka
# Ubuntu/Debian: Download .deb from releases

# System dependencies (X11 audio capture & text insertion)
sudo pacman -S pipewire pipewire-pulse libpulse xdotool libnotify
# Or on Debian/Ubuntu:
# sudo apt install pipewire pipewire-pulse pulseaudio-utils xdotool libnotify-bin

# Verify audio capture works
pw-record --rate=24000 --channels=1 --format=s16 test.wav
# Press Ctrl+C to stop, then play back:
pw-play test.wav

# Verify xdotool works
xdotool type "Hello from ok-voice"

# Verify notifications work
notify-send "ok-voice" "Test notification"
```

## OpenAI Realtime Transcription API Details

### Connection
```
WebSocket URL: wss://api.openai.com/v1/realtime?model=gpt-realtime
Authorization: Bearer <OPENAI_API_KEY>
```

### Session Configuration
```json
{
  "type": "session.update",
  "session": {
    "type": "transcription",
    "audio": {
      "input": {
        "format": { "type": "audio/pcm", "rate": 24000 },
        "noise_reduction": { "type": "near_field" },
        "transcription": {
          "model": "gpt-4o-mini-transcribe",
          "language": "en"
        },
        "turn_detection": {
          "type": "server_vad",
          "threshold": 0.5,
          "silence_duration_ms": 500
        }
      }
    }
  }
}
```

### Audio Format Requirements
- **Format**: 16-bit signed little-endian PCM (S16_LE)
- **Sample Rate**: 24000 Hz (required for `audio/pcm`)
- **Channels**: 1 (mono)
- **Encoding**: Base64 for WebSocket transport

### Key Events
| Event | Direction | Purpose |
|-------|-----------|---------|
| `input_audio_buffer.append` | Client→Server | Send audio chunk (base64) |
| `conversation.item.input_audio_transcription.delta` | Server→Client | Incremental transcript |
| `conversation.item.input_audio_transcription.completed` | Server→Client | Final transcript for turn |
| `input_audio_buffer.speech_started` | Server→Client | VAD detected speech start |
| `input_audio_buffer.speech_stopped` | Server→Client | VAD detected speech end |
| `error` | Server→Client | API error |

### Transcription Models
| Model | Cost (per minute) | Quality | Speed |
|-------|-------------------|---------|-------|
| `gpt-4o-transcribe` | Higher | Best | Fast |
| `gpt-4o-mini-transcribe` | Lower | Good | Fastest |
| `whisper-1` | Lowest | Good | Slower |

**Recommendation**: Start with `gpt-4o-mini-transcribe` for balance of cost/quality.

## Audio Capture Commands

### PipeWire (Recommended - Modern Linux)
```bash
# Capture raw PCM to stdout (for piping to app)
pw-record --rate=24000 --channels=1 --format=s16 -

# Capture to file
pw-record --rate=24000 --channels=1 --format=s16 output.raw
```

### ALSA (Fallback - Works with PipeWire's ALSA plugin)
```bash
# Capture raw PCM to stdout
arecord -f S16_LE -r 24000 -c 1 -t raw -

# Capture to WAV file
arecord -f S16_LE -r 24000 -c 1 output.wav
```

### From Babashka
```clojure
(require '[babashka.process :as p])

;; Stream audio from PipeWire
(def audio-process
  (p/process 
    {:in :pipe :out :stream}
    "pw-record --rate=24000 --channels=1 --format=s16 -"))

;; Read chunks from (:out audio-process)
;; Convert to base64 and send via WebSocket
```

## xdotool Integration

### Basic Usage
```bash
# Type text at cursor
xdotool type "Hello, world!"

# Type with custom delay between keystrokes (ms)
xdotool type --delay 5 "Hello, world!"

# Type from file
xdotool type --file transcript.txt
```

### From Babashka
```clojure
(require '[babashka.process :as p])

(defn insert-text [text]
  (p/sh "xdotool" "type" "--delay" "5" text))

;; After receiving transcript from OpenAI:
(insert-text "Hello, this is your transcription.")
```

### Unicode/Special Characters
xdotool handles Unicode automatically in most cases. If issues arise:
```bash
# Use --clearmodifiers if modifier keys interfere
xdotool type --clearmodifiers "text"
```

## Desktop Notifications

### Basic Usage
```bash
# Simple notification
notify-send "ok-voice" "Recording started"

# With urgency level
notify-send -u critical "ok-voice" "Error: API connection failed"

# With icon
notify-send -i microphone "ok-voice" "Listening..."
```

### From Babashka
```clojure
(defn notify [title body & {:keys [urgency] :or {urgency "normal"}}]
  (p/sh "notify-send" "-u" urgency title body))

(notify "ok-voice" "Recording started")
(notify "ok-voice" "API Error" :urgency "critical")
```

## Configuration

### Config File Location
```
~/.config/ok-voice/config.edn
```

### Config Structure
```clojure
{:openai/api-key "sk-..."  ;; Or use OPENAI_API_KEY env var
 :audio/sample-rate 24000
 :audio/channels 1
 :transcription/model "gpt-4o-mini-transcribe"
 :transcription/language "en"  ;; nil for auto-detect
 :vad/enabled true
 :vad/threshold 0.5
 :vad/silence-ms 500}
```

### Loading Config in Babashka
```clojure
(require '[clojure.edn :as edn]
         '[clojure.java.io :as io])

(def config-path (io/file (System/getenv "HOME") 
                          ".config" "ok-voice" "config.edn"))

(defn load-config []
  (if (.exists config-path)
    (edn/read-string (slurp config-path))
    {}))

;; Get API key from config or env
(defn get-api-key []
  (or (:openai/api-key (load-config))
      (System/getenv "OPENAI_API_KEY")))
```

## Alternatives Considered

| Recommended | Alternative | When to Use Alternative |
|-------------|-------------|-------------------------|
| Babashka | JVM Clojure + GraalVM Native | When you need full Clojure ecosystem, not just scripting |
| Babashka | Python | When team doesn't know Clojure, or needs ML libraries |
| babashka.http-client WebSocket | Gniazdo | When you need JDK 8 compatibility (we're on 11+) |
| babashka.http-client WebSocket | Java interop with java.net.http | When you need maximum control over WebSocket |
| OpenAI Realtime Transcription | Whisper API (batch) | When you don't need real-time, want lower cost |
| OpenAI Realtime Transcription | Local Whisper.cpp | When you need offline, zero latency, no API costs |
| PipeWire | PulseAudio directly | When on older distros without PipeWire |
| xdotool | ydotool | When you need Wayland support (not X11-specific) |
| xdotool | wtype | When on Wayland (Wayland native) |
| libnotify | dunstify | When using dunst notification daemon |

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| babashka.curl | Shells out to curl, slower, Windows issues | babashka.http-client (built-in) |
| Aleph WebSocket | Server-focused, heavy dependency | babashka.http-client.websocket |
| Sente | Full-stack web framework, overkill | babashka.http-client.websocket |
| http-kit WebSocket | Buffers in memory, server-focused | babashka.http-client.websocket |
| PulseAudio (when PipeWire available) | PipeWire supersedes it in 2025 | PipeWire with pw-record |
| JACK | Pro audio, overkill for voice capture | PipeWire (includes JACK compatibility) |
| Raw REST polling | Latency too high for real-time | WebSocket streaming |

## Stack Patterns by Variant

**If Wayland instead of X11:**
- Use `wtype` instead of `xdotool`
- Because: xdotool doesn't work on Wayland; wtype is the Wayland native equivalent

**If offline transcription needed:**
- Use local Whisper.cpp or whisper.java
- Because: Zero latency, no API costs, works offline; trade-off is accuracy and hardware requirements

**If maximum compatibility with older Linux:**
- Use `arecord` instead of `pw-record`
- Use PulseAudio `parecord` as fallback
- Because: ALSA is universal, PipeWire requires newer distros

## Version Compatibility

| Package | Compatible With | Notes |
|---------|-----------------|-------|
| Babashka 1.12.x | Java 11+ | Requires Java 11 for WebSocket |
| babashka.http-client | Babashka 1.1.171+ | Built-in, no separate install |
| PipeWire 1.x | Linux kernel 5.6+ | Modern distros (2022+) |
| xdotool 3.x | X11 | Does not work on Wayland |

## Sources

- **Babashka releases** — https://github.com/babashka/babashka/releases (v1.12.215, Feb 2026)
- **babashka.http-client WebSocket API** — https://cljdoc.org/d/org.babashka/http-client/0.4.23/api/babashka.http-client.websocket
- **OpenAI Realtime Transcription Guide** — https://platform.openai.com/docs/guides/realtime-transcription
- **OpenAI Realtime API Reference** — https://platform.openai.com/docs/api-reference/realtime
- **PipeWire pw-cat docs** — https://docs.pipewire.org/page_man_pw-cat_1.html
- **ArchWiki PipeWire** — https://wiki.archlinux.org/title/PipeWire
- **ArchWiki Desktop Notifications** — https://wiki.archlinux.org/title/Desktop_notifications
- **xdotool manpage** — https://man.archlinux.org/man/xdotool.1.en

---
*Stack research for: Linux voice-to-text CLI with OpenAI Realtime Transcription*
*Researched: 2026-02-19*
