# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-02-19)

**Core value:** Instant voice-to-text anywhere on Linux: press hotkey, speak, text appears at cursor
**Current focus:** All 4 phases complete. Milestone ready for verification.

## Current Position

Phase: 4 of 4 (Text Output)
Plan: 2 of 2 in current phase (04-01, 04-02 complete)
Status: Phase 4 complete -- all phases done
Last activity: 2026-02-19 — Completed plan 04-02 (Core.clj integration)

Progress: [██████████] 100% (all 4 phases complete)

## Performance Metrics

**Velocity:**
- Total plans completed: 9
- Average duration: 1.3 min
- Total execution time: 0.20 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01 | 2 | 6 min | 3 min |
| 02 | 3 | 3 min | 1 min |
| 03 | 2 | 2 min | 1 min |
| 04 | 2 | 1 min | <1 min |

**Recent Trend:**
- Last 5 plans: 02-03 (1 min), 03-01 (1 min), 03-02 (1 min), 04-01 (<1 min), 04-02 (<1 min)
- Trend: Stable at ~1 min/plan

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
- Phase 3 (03-01): PID via $PPID in spawned shell (ProcessHandle unavailable in bb/GraalVM)
- Phase 3 (03-01): No file locking -- race window negligible for hotkey use case
- Phase 3 (03-02): Toggle check first in -main for fast stop path (no config/deps loading)
- Phase 3 (03-02): PID written before pipeline start to minimize race window
- Phase 3 (03-02): Shutdown hook removes PID first, then stops pipeline
- Phase 4 (04-01): Clipboard paste (xclip + xdotool ctrl+v) for Unicode reliability
- Phase 4 (04-01): 50ms delay between clipboard set and paste for X11 event propagation
- Phase 4 (04-02): Transcription logged to stderr, not stdout (clean for pipeline usage)

### Pending Todos

None.

### Blockers/Concerns

None.

## Session Continuity

Last session: 2026-02-19
Stopped at: Completed 04-02-PLAN.md (Core.clj integration) -- All phases complete
Resume file: .planning/phases/04-text-output/04-02-PLAN.md
