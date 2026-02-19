# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-02-19)

**Core value:** Instant voice-to-text anywhere on Linux: press hotkey, speak, text appears at cursor
**Current focus:** Phase 3 complete. Next: Text Output (Phase 4)

## Current Position

Phase: 3 of 4 (Toggle Mode & Orchestration)
Plan: 2 of 2 in current phase (03-01, 03-02 complete)
Status: Phase 3 complete -- ready for Phase 4
Last activity: 2026-02-19 — Completed plan 03-02 (Toggle mode orchestration)

Progress: [█████████░] ~95% (phases 1-3 complete, phase 4 remaining)

## Performance Metrics

**Velocity:**
- Total plans completed: 7
- Average duration: 1.6 min
- Total execution time: 0.19 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01 | 2 | 6 min | 3 min |
| 02 | 3 | 3 min | 1 min |
| 03 | 2 | 2 min | 1 min |

**Recent Trend:**
- Last 5 plans: 02-01 (1 min), 02-02 (1 min), 02-03 (1 min), 03-01 (1 min), 03-02 (1 min)
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
- Phase 4 (upcoming): Text insertion via xdotool with clipboard fallback for Unicode

### Pending Todos

None yet.

### Blockers/Concerns

None yet.

## Session Continuity

Last session: 2026-02-19
Stopped at: Completed 03-02-PLAN.md (Toggle mode orchestration) -- Phase 3 complete
Resume file: .planning/phases/03-toggle-mode-orchestration/03-02-SUMMARY.md
