# Project Research Summary

**Project:** ok-voice
**Domain:** Linux voice-to-text CLI (X11/i3)
**Researched:** 2026-02-19
**Confidence:** HIGH

## Executive Summary

ok-voice is a Linux CLI tool that provides instant voice-to-text anywhere on X11 desktops: press a hotkey, speak, and text appears at cursor. Unlike existing tools (Voxtype, VOXD, OpenWhispr) which use batch processing with local Whisper models, ok-voice uses the OpenAI Realtime Transcription API to provide live, streaming transcription as the user speaks. This is the primary competitive differentiator—words appear in real-time rather than after a delay.

The recommended architecture is a Babashka-based CLI with a pipeline streaming pattern: PipeWire captures audio (PCM 24kHz mono), which streams via WebSocket to OpenAI, transcription events flow back, and xdotool inserts text at cursor. Toggle mode is handled via a process singleton pattern—xbindkeys triggers the same binary, which checks if it's already running to start or stop recording. The minimal UI uses desktop notifications only (no GUI, no tray icon).

The key risks center on audio format precision (wrong format = garbled transcription), xdotool limitations (Unicode and race conditions), and WebSocket reliability (silent disconnects mid-session). These are well-documented with clear prevention strategies. The project should stay X11-only and defer offline mode—these anti-features keep scope focused.

## Key Findings

### Recommended Stack

Babashka is ideal for this project: ~10ms startup, built-in WebSocket support via `babashka.http-client`, and includes cheshire (JSON) and tools.cli. No external JVM or dependencies needed.

**Core technologies:**
- **Babashka v1.12.215** — Native Clojure runtime with fast startup, perfect for CLI tools triggered by hotkey
- **OpenAI Realtime Transcription API** — Live streaming transcription with sub-second latency; use `gpt-4o-mini-transcribe` for cost/quality balance
- **PipeWire (pw-record)** — 2025 Linux audio standard; use `--rate=24000 --channels=1 --format=s16` for PCM output
- **xdotool** — X11 text insertion via `type` command; works with all X11 apps
- **libnotify (notify-send)** — Desktop notifications for error/feedback; freedesktop.org standard

**Critical format requirements:**
- Audio: 16-bit signed little-endian PCM, 24kHz sample rate, mono channel
- Transport: Base64-encoded audio chunks over WebSocket

### Expected Features

**Must have (table stakes):**
- Toggle mode recording — press hotkey to start, press again to stop (via xbindkeys)
- Text insertion at cursor — via xdotool for X11
- Configuration file — API key storage in `~/.config/ok-voice/config.edn`
- Basic error notifications — notify-send when things fail
- Auto-detect language — use OpenAI's language detection

**Should have (competitive):**
- Real-time streaming transcription — words appear as you speak (major differentiator vs batch tools)
- Recording feedback — audio beep or notification on start/stop
- Spoken punctuation — "period" → ".", "new line" → "\n"
- Systemd daemon — autostart, always-ready experience

**Defer (v2+):**
- Local/offline transcription — requires Whisper.cpp integration
- Multiple API providers — Groq, Anthropic endpoints
- Wayland support — would require ydotool/wtype

### Architecture Approach

The architecture follows a pipeline streaming pattern with toggle state managed by a process singleton. External trigger (xbindkeys) calls the same binary; the process checks a PID lock to determine whether to start or stop.

**Major components:**
1. **core.clj** — Entry point, toggle logic (check PID, start/stop), main orchestration
2. **config.clj** — Load API key from EDN config, fail fast if missing
3. **audio.clj** — Spawn `pw-record` subprocess, stream PCM chunks
4. **websocket.clj** — Connect to OpenAI Realtime API, send audio, receive events
5. **transcription.clj** — Parse JSON events, accumulate text in atom buffer
6. **output.clj** — Call `xdotool type` to insert text at cursor
7. **notify.clj** — Desktop notifications for errors and feedback
8. **state.clj** — PID file lock for singleton detection

### Critical Pitfalls

1. **Audio Format Mismatch** — OpenAI requires exact format (PCM16, 24kHz, mono). Wrong format produces gibberish. Verify with `pw-record --rate=24000 --channels=1 --format=s16 test.raw`
2. **xdotool Unicode Failures** — xdotool's `type` has poor Unicode support. Use clipboard paste (`xclip` + `xdotool key ctrl+shift+v`) or test extensively with target character set
3. **xdotool Race Condition** — First characters lost due to focus timing. Add `--delay 12` and `windowfocus --sync` before typing
4. **WebSocket Disconnects Mid-Session** — Network issues cause silent freezes. Implement exponential backoff reconnection and show error notification
5. **Linux Audio Fragmentation** — PipeWire/PulseAudio/ALSA differ. Detect with `pactl info`, prefer PipeWire (works on both PipeWire and PulseAudio systems)

## Implications for Roadmap

Based on research, suggested phase structure:

### Phase 1: Foundation & Config
**Rationale:** Must have API key before any network activity; notifications needed for error feedback throughout
**Delivers:** Config loading, error notifications, dependency checks
**Addresses:** Config file, error notifications
**Avoids:** API key security pitfall (permissions, .gitignore)
**Stack:** Babashka, EDN config, libnotify

### Phase 2: Audio Capture Pipeline
**Rationale:** Core capability—without audio, nothing works; get format right early
**Delivers:** Microphone capture in correct format (PCM 24kHz mono)
**Addresses:** Audio capture from FEATURES.md
**Avoids:** Audio format mismatch pitfall, audio stack fragmentation
**Stack:** PipeWire pw-record, babashka.process

### Phase 3: OpenAI WebSocket Integration
**Rationale:** Transcription is the core value; WebSocket is the transport
**Delivers:** Live connection to Realtime API, session config, event parsing
**Addresses:** OpenAI Realtime Transcription, auto-detect language
**Avoids:** WebSocket disconnect pitfall (reconnection logic)
**Stack:** babashka.http-client WebSocket, cheshire JSON

### Phase 4: Toggle Mode & State
**Rationale:** Needed for hotkey workflow; coordinates all other components
**Delivers:** PID-based toggle, start/stop orchestration
**Addresses:** Toggle mode recording
**Avoids:** Toggle state sync pitfall, race conditions
**Stack:** state.clj, PID file locking

### Phase 5: Text Output
**Rationale:** Final step—insert transcribed text at cursor
**Delivers:** xdotool integration, Unicode handling, focus management
**Addresses:** Text insertion at cursor
**Avoids:** xdotool Unicode pitfall, race condition pitfall
**Stack:** xdotool, xclip (for Unicode fallback)

### Phase 6: Polish & UX
**Rationale:** MVP works but needs user feedback and quality of life
**Delivers:** Recording feedback, spoken punctuation, better error messages
**Addresses:** Recording feedback, spoken punctuation
**Stack:** libnotify, text post-processing

### Phase Ordering Rationale

- **Config first:** API key is required for WebSocket connection; fail fast principle
- **Audio before WebSocket:** Need audio format correct before sending to API
- **WebSocket before Toggle:** Core transcription must work before orchestration
- **Toggle before Output:** Need full pipeline before testing end-to-end
- **Polish last:** Core functionality first, quality improvements after validation

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 3:** OpenAI Realtime API event types are documented but complex; may need `/gsd-research-phase` for detailed event handling patterns
- **Phase 5:** xdotool Unicode handling has known issues; clipboard fallback approach needs validation

Phases with standard patterns (skip research-phase):
- **Phase 1:** Config loading is well-documented EDN pattern
- **Phase 2:** PipeWire audio capture has clear CLI documentation
- **Phase 4:** PID file locking is standard Unix pattern

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Babashka WebSocket verified in official docs; OpenAI Realtime API well-documented |
| Features | HIGH | Competitive analysis from 4+ similar tools; clear table stakes and differentiators |
| Architecture | HIGH | Pipeline streaming is established pattern; component boundaries clear |
| Pitfalls | HIGH | All pitfalls verified with official docs or multiple community sources |

**Overall confidence:** HIGH

### Gaps to Address

- **WebSocket reconnection specifics:** Research shows need for exponential backoff, but exact retry counts/timeout values need tuning during implementation
- **Audio device detection:** PipeWire/PulseAudio/ALSA detection logic needs testing across distros (Ubuntu, Fedora, Arch)
- **Unicode clipboard fallback:** xdotool Unicode issues are documented; clipboard approach needs implementation validation

## Sources

### Primary (HIGH confidence)
- OpenAI Realtime Transcription Guide — https://platform.openai.com/docs/guides/realtime-transcription
- OpenAI Realtime API Reference — https://platform.openai.com/docs/api-reference/realtime
- babashka.http-client WebSocket API — https://cljdoc.org/d/org.babashka/http-client/0.4.23/api/babashka.http-client.websocket
- PipeWire Documentation — https://docs.pipewire.org/
- Latent.Space: OpenAI Realtime API - The Missing Manual — https://www.latent.space/p/realtime-api

### Secondary (MEDIUM confidence)
- Microsoft Azure: GPT Realtime Audio — Audio format specifications
- ArchWiki: PulseAudio/PipeWire — Linux audio troubleshooting
- xdotool GitHub Issues — Unicode and race condition issues documented
- OpenAI Community Forums — Transcription errors, WebSocket disconnect reports
- Competitor repos (VOXD, Voxtype, OpenWhispr) — Feature patterns and comparison

### Tertiary (LOW confidence)
- Reddit discussions (r/linux, r/archlinux) — User expectations and pain points
- Various forum posts — xdotool timing issues, audio format gotchas

---
*Research completed: 2026-02-19*
*Ready for roadmap: yes*
