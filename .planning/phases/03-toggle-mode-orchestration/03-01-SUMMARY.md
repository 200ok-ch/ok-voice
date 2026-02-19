---
plan: 03-01
status: complete
started: 2026-02-19
completed: 2026-02-19
duration: 1 min
---

# Summary: PID-based singleton detection

## What was done
Created `src/ok_voice/toggle.clj` with PID file management and process signaling:

- `write-pid!` — writes current bb PID to `$XDG_RUNTIME_DIR/ok-voice.pid` (fallback `/tmp`)
- `read-pid` — reads PID from file, returns long or nil
- `remove-pid!` — deletes PID file
- `process-alive?` — checks process existence via `kill -0`
- `running-instance` — returns live PID or nil (cleans stale PID files)
- `signal-stop!` — sends SIGTERM via `kill`

## Decisions
- PID obtained via `$PPID` in spawned shell (ProcessHandle unavailable in bb/GraalVM)
- No file locking — race window negligible for hotkey use case

## Verification
All tests passed: write/read/remove cycle, alive detection, stale PID cleanup.
