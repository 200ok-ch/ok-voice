# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-02-19)

**Core value:** Instant voice-to-text anywhere on Linux: press hotkey, speak, text appears at cursor
**Current focus:** Audio Capture & Transcription (Phase 2)

## Current Position

Phase: 2 of 4 (Audio Capture & Transcription)
Plan: 2 of 3 in current phase (02-01, 02-02 complete)
Status: Executing Phase 2 -- 02-02 complete, 02-03 next
Last activity: 2026-02-19 — Completed plan 02-02 (OpenAI Realtime WebSocket module)

Progress: [████████░░] ~80% (phases 1 complete + phase 2 plan 2/3)

## Performance Metrics

**Velocity:**
- Total plans completed: 4
- Average duration: 2 min
- Total execution time: 0.13 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01 | 2 | 6 min | 3 min |
| 02 | 2 | 2 min | 1 min |

**Recent Trend:**
- Last 5 plans: 01-01 (2 min), 01-02 (4 min), 02-01 (1 min), 02-02 (1 min)
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
- Phase 2 (02-02): WebSocket via babashka.http-client.websocket (built into bb, no extra deps)
- Phase 2 (02-02): Per-connection StringBuilder for message fragment accumulation
- Phase 2 (02-02): Language omitted from session config for auto-detection (CORE-05)
- Phase 3 (upcoming): Toggle via PID file singleton pattern
- Phase 4 (upcoming): Text insertion via xdotool with clipboard fallback for Unicode

### Pending Todos

None yet.

### Blockers/Concerns

None yet.

## Session Continuity

Last session: 2026-02-19
Stopped at: Completed 02-02-PLAN.md (OpenAI Realtime WebSocket module)
Resume file: .planning/phases/02-audio-capture-transcription/02-02-SUMMARY.md
