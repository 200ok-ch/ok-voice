# Roadmap: ok-voice

## Overview

ok-voice delivers instant voice-to-text on Linux X11. The journey starts with configuration and error handling foundation, then builds the audio-to-transcription pipeline, adds toggle mode orchestration for hotkey workflow, and finally connects everything to insert text at cursor. Four phases take the project from empty directory to fully functional voice dictation tool.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [x] **Phase 1: Foundation & Configuration** - Config loading and error notifications
- [ ] **Phase 2: Audio Capture & Transcription** - Audio pipeline and OpenAI WebSocket integration
- [ ] **Phase 3: Toggle Mode & Orchestration** - Hotkey-triggered start/stop recording
- [ ] **Phase 4: Text Output** - Insert transcription at cursor via xdotool

## Phase Details

### Phase 1: Foundation & Configuration
**Goal**: Application is ready to use with proper configuration and can communicate errors to the user
**Depends on**: Nothing (first phase)
**Requirements**: CORE-06, CORE-07
**Success Criteria** (what must be TRUE):
  1. User can create a config file with API key and application loads it on startup
  2. Application validates dependencies (PipeWire, xdotool, notify-send) are installed on first run
  3. User receives desktop notification if config file is missing or API key is invalid
  4. User receives desktop notification if required dependencies are not found
**Plans**: 2 plans in 2 waves

Plans:
- [x] 01-01-PLAN.md — Project setup and notification system
- [x] 01-02-PLAN.md — Config loading and dependency validation

### Phase 2: Audio Capture & Transcription
**Goal**: System captures microphone audio and produces live, streaming transcription via OpenAI
**Depends on**: Phase 1
**Requirements**: CORE-03, CORE-05
**Success Criteria** (what must be TRUE):
  1. User speaks into microphone and audio is captured in correct format (PCM 16-bit, 24kHz mono)
  2. System establishes WebSocket connection to OpenAI Realtime Transcription API
  3. User sees transcription text arriving as they speak (streaming, not batch)
  4. System auto-detects spoken language without manual configuration
**Plans**: 3 plans in 2 waves

Plans:
- [ ] 02-01-PLAN.md — Audio capture with PipeWire (pw-record subprocess, base64 chunking)
- [ ] 02-02-PLAN.md — WebSocket connection to OpenAI Realtime Transcription API
- [ ] 02-03-PLAN.md — Transcription pipeline orchestration and core.clj integration

### Phase 3: Toggle Mode & Orchestration
**Goal**: User can start and stop recording via external hotkey trigger (xbindkeys)
**Depends on**: Phase 2
**Requirements**: CORE-01, CORE-02
**Success Criteria** (what must be TRUE):
  1. User presses hotkey (configured via xbindkeys) and recording starts
  2. User presses hotkey again while recording and recording stops
  3. Application detects if already running and toggles state appropriately (singleton pattern)
  4. Multiple rapid hotkey presses do not cause race conditions or duplicate processes
**Plans**: TBD

Plans:
- [ ] 03-01: PID-based singleton detection
- [ ] 03-02: Toggle mode orchestration

### Phase 4: Text Output
**Goal**: Transcribed text appears at cursor position in the active window
**Depends on**: Phase 3
**Requirements**: CORE-04
**Success Criteria** (what must be TRUE):
  1. When recording stops, accumulated transcription is inserted at current cursor position
  2. Text appears in the currently active X11 window (focus is maintained correctly)
  3. Special characters and Unicode text are inserted correctly (not garbled)
  4. Text insertion completes fast enough to feel instantaneous to the user
**Plans**: TBD

Plans:
- [ ] 04-01: xdotool text insertion
- [ ] 04-02: Unicode and focus handling

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Foundation & Configuration | 2/2 | Complete | 2026-02-19 |
| 2. Audio Capture & Transcription | 0/3 | Not started | - |
| 3. Toggle Mode & Orchestration | 0/2 | Not started | - |
| 4. Text Output | 0/2 | Not started | - |
