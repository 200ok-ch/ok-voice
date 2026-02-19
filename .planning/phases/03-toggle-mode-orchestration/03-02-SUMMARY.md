---
plan: 03-02
status: complete
started: 2026-02-19
completed: 2026-02-19
duration: 1 min
---

# Summary: Toggle mode orchestration

## What was done
Updated `src/ok_voice/core.clj` with toggle-aware `-main`:

- **Stop path**: if instance running, `signal-stop!` + exit 0 (no deps/config loading)
- **Start path**: deps check → config load → `write-pid!` → notify → start pipeline → block
- **Shutdown hook**: `remove-pid!` first, then stop pipeline, print text, notify "Recording stopped."
- **Failure path**: `remove-pid!` before exit 1 on pipeline failure

## Key changes
- Added `[ok-voice.toggle :as toggle]` require
- Toggle check is first action in `-main` for fast stop path
- PID written before pipeline start to minimize race window
- "Recording stopped." notification added to shutdown hook

## Verification
All namespaces load correctly with toggle integration.
