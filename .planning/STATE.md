# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-02-19)

**Core value:** Instant voice-to-text anywhere on Linux: press hotkey, speak, text appears at cursor
**Current focus:** Phase 2 complete. Next: Toggle & Process Control (Phase 3)

## Current Position

Phase: 2 of 4 (Audio Capture & Transcription)
Plan: 3 of 3 in current phase (02-01, 02-02, 02-03 complete)
Status: Phase 2 complete -- ready for Phase 3
Last activity: 2026-02-19 — Completed plan 02-03 (Transcription pipeline & recording mode)

Progress: [█████████░] ~90% (phases 1-2 complete, phases 3-4 remaining)

## Performance Metrics

**Velocity:**
- Total plans completed: 5
- Average duration: 2 min
- Total execution time: 0.15 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01 | 2 | 6 min | 3 min |
| 02 | 3 | 3 min | 1 min |

**Recent Trend:**
- Last 5 plans: 01-02 (4 min), 02-01 (1 min), 02-02 (1 min), 02-03 (1 min)
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
- Phase 2 (02-03): Module-level atoms for pipeline state (callback access from WebSocket handlers)
- Phase 2 (02-03): Forward-declared stop! for on-error callback reference
- Phase 2 (02-03): Atom-wrapped promise for resettable ready synchronization
- Phase 3 (upcoming): Toggle via PID file singleton pattern
- Phase 4 (upcoming): Text insertion via xdotool with clipboard fallback for Unicode

### Pending Todos

None yet.

### Blockers/Concerns

None yet.

## Session Continuity

Last session: 2026-02-19
Stopped at: Completed 02-03-PLAN.md (Transcription pipeline & recording mode) -- Phase 2 complete
Resume file: .planning/phases/02-audio-capture-transcription/02-03-SUMMARY.md
