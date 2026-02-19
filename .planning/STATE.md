# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-02-19)

**Core value:** Instant voice-to-text anywhere on Linux: press hotkey, speak, text appears at cursor
**Current focus:** Audio Capture & Transcription (Phase 2)

## Current Position

Phase: 2 of 4 (Audio Capture & Transcription)
Plan: 1 of 3 in current phase (02-01 complete)
Status: Executing Phase 2 -- 02-01 complete, 02-02 next
Last activity: 2026-02-19 — Completed plan 02-01 (PipeWire audio capture module)

Progress: [███████░░░] ~60% (phases 1 complete + phase 2 plan 1/3)

## Performance Metrics

**Velocity:**
- Total plans completed: 3
- Average duration: 2.3 min
- Total execution time: 0.12 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01 | 2 | 6 min | 3 min |
| 02 | 1 | 1 min | 1 min |

**Recent Trend:**
- Last 5 plans: 01-01 (2 min), 01-02 (4 min), 02-01 (1 min)
- Trend: Accelerating

*Updated after each plan completion*

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

- Phase 1 (01-01): Used clj-commons/clj-yaml 1.0.29 (plan had wrong coordinates clj-yaml/clj-yaml)
- Phase 1 (01-01): Task names in bb.edn use symbols not keywords (for bb tasks listing)
- Phase 1 (01-02): Config file format is YAML (~/.config/ok-voice/config.yaml)
- Phase 1 (01-02): API key lookup: config file first, OPENAI_API_KEY env var fallback
- Phase 1 (01-02): notify.clj falls back to stderr when notify-send unavailable
- Phase 2 (02-01): Stateless audio module -- no atoms, no notifications, no device selection
- Phase 2 (02-01): chunk-size = 4800 bytes (24kHz * 1ch * 2bytes * 100ms)
- Phase 2 (upcoming): WebSocket via babashka.http-client
- Phase 3 (upcoming): Toggle via PID file singleton pattern
- Phase 4 (upcoming): Text insertion via xdotool with clipboard fallback for Unicode

### Pending Todos

None yet.

### Blockers/Concerns

None yet.

## Session Continuity

Last session: 2026-02-19
Stopped at: Completed 02-01-PLAN.md (PipeWire audio capture module)
Resume file: .planning/phases/02-audio-capture-transcription/02-01-SUMMARY.md
