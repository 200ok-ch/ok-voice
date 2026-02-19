# Requirements: ok-voice

**Defined:** 2026-02-19
**Core Value:** Instant voice-to-text anywhere on Linux: press hotkey, speak, text appears at cursor

## v1 Requirements

Requirements for initial release. Each maps to roadmap phases.

### Core

- [ ] **CORE-01**: User can trigger recording via xbindkeys hotkey
- [ ] **CORE-02**: User can stop recording by pressing hotkey again (toggle mode)
- [ ] **CORE-03**: System captures audio and streams to OpenAI Realtime Transcription API
- [ ] **CORE-04**: System inserts transcribed text at cursor position via xdotool
- [ ] **CORE-05**: System auto-detects spoken language via OpenAI API
- [ ] **CORE-06**: System shows desktop notification on errors via notify-send
- [ ] **CORE-07**: System loads API key from config file (~/.config/ok-voice/config.yaml)

## v2 Requirements

Deferred to future release. Tracked but not in current roadmap.

### UX Enhancements

- **UX-01**: Recording feedback — audio beep on start/stop
- **UX-02**: Spoken punctuation — "period" → ".", "new line" → "\n"
- **UX-03**: Custom word replacements — fix common misrecognitions

### Infrastructure

- **INFRA-01**: Systemd user service for autostart
- **INFRA-02**: Local Whisper fallback for offline operation

## Out of Scope

Explicitly excluded. Documented to prevent scope creep.

| Feature | Reason |
|---------|--------|
| Built-in hotkey handling | xbindkeys handles this, avoid duplication |
| GUI interface | CLI-only design, config file for settings |
| Always-on listening | Toggle mode gives user control, avoids privacy concerns |
| Account/billing system | BYOK — user provides their own API key |
| Wayland support | X11 only (i3 focus), avoid compositor complexity |
| File transcription mode | Focus on real-time dictation only |
| Voice commands | Text input only, not computer control |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| CORE-01 | - | Pending |
| CORE-02 | - | Pending |
| CORE-03 | - | Pending |
| CORE-04 | - | Pending |
| CORE-05 | - | Pending |
| CORE-06 | - | Pending |
| CORE-07 | - | Pending |

**Coverage:**
- v1 requirements: 7 total
- Mapped to phases: 0
- Unmapped: 7 ⚠️

---
*Requirements defined: 2026-02-19*
*Last updated: 2026-02-19 after initial definition*
