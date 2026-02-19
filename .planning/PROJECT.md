# ok-voice

## What This Is

A Linux voice-to-text application that uses OpenAI's Realtime Transcription API. Press a hotkey to start speaking, press again to stop — transcribed text appears at your cursor position. Designed for i3/X11 with minimal visual footprint.

## Core Value

Instant voice-to-text anywhere on Linux: press hotkey, speak, text appears at cursor. No UI clutter, no friction.

## Requirements

### Validated

(None yet — ship to validate)

### Active

- [ ] Trigger via external hotkey (xbindkeys)
- [ ] Toggle mode: first press starts recording, second press stops
- [ ] Live transcription via OpenAI Realtime Transcription API
- [ ] Insert transcribed text at current cursor position (xdotool)
- [ ] Auto-detect spoken language
- [ ] Desktop notification on errors (no internet, API errors)
- [ ] API key stored in config file

### Out of Scope

- Built-in hotkey handling — user configures via xbindkeys or similar
- Visual feedback during recording — stay minimal/invisible
- Multi-language configuration — API auto-detects
- Wayland support — X11 only (i3)
- GUI/Settings panel — config file only

## Context

- Target user: Linux power user with i3/X11 setup
- Already uses xbindkeys for hotkey management
- Wants frictionless voice input without UI overhead
- OpenAI Realtime Transcription provides streaming results

## Constraints

- **Platform**: Linux X11 only (i3) — xdotool for text insertion
- **Tech Stack**: Clojure/Babashka
- **API**: OpenAI Realtime Transcription API
- **External Trigger**: App is started by xbindkeys, handles toggle internally

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| No built-in hotkey handling | User already has xbindkeys workflow | — Pending |
| Config file for API key | Simple, portable, version-controllable | — Pending |
| X11/xdotool only | User uses i3, not Sway/Wayland | — Pending |
| Toggle mode | Press to start, press to stop — more flexible than hold | — Pending |

---
*Last updated: 2026-02-19 after initialization*
