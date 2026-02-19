# Plan 01-02 Summary: Config Loading & Dependency Validation

## What was done
- Created config.clj with YAML loading, API key extraction (with OPENAI_API_KEY env var fallback), and validation
- Created deps.clj with system dependency checking for pw-record, xdotool, and notify-send
- Created core.clj as application entry point that wires deps check, config load, and API key validation
- Created resources/config.example.yaml with example configuration
- Improved notify.clj with graceful stderr fallback when notify-send is unavailable

## Files created
- src/ok_voice/config.clj
- src/ok_voice/deps.clj
- src/ok_voice/core.clj
- resources/config.example.yaml

## Files modified
- src/ok_voice/notify.clj (added try/catch with stderr fallback)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Added clojure.string require to deps.clj**
- **Found during:** Task 2
- **Issue:** deps.clj used clojure.string/join but did not require the namespace
- **Fix:** Added `[clojure.string :as str]` to requires, used `str/join` alias
- **Files modified:** src/ok_voice/deps.clj

**2. [Rule 1 - Bug] Fixed blank string handling in get-api-key**
- **Found during:** Verification
- **Issue:** Empty string from OPENAI_API_KEY env var was treated as truthy, bypassing the "no API key" error
- **Fix:** Added not-blank helper that returns nil for nil/blank strings
- **Files modified:** src/ok_voice/config.clj

**3. [Rule 2 - Missing error handling] Added graceful fallback in notify.clj**
- **Found during:** Verification
- **Issue:** notify/error threw IOException when notify-send was not installed, preventing error messages from being shown
- **Fix:** Wrapped sh call in try/catch, falls back to printing to stderr with urgency prefix
- **Files modified:** src/ok_voice/notify.clj

## Verification results

```
Test 1 - Missing config exits 1:
  [critical] ok-voice: Config not found: /home/munen/.config/ok-voice/config.yaml
  EXIT: 1
  PASS

Test 2 - Valid config loads correctly:
  Config loaded: #ordered/map ([:api-key test-key-for-verification])
  PASS: load! reads YAML config correctly
  PASS: validate-api-key! returns key from config
  EXIT: 0
  PASS

Test 3 - Missing API key exits 1:
  [critical] ok-voice: No API key found. Set api-key in config or OPENAI_API_KEY env var.
  EXIT: 1
  PASS

Test 4 - API key from OPENAI_API_KEY env var:
  PASS: get-api-key falls back to OPENAI_API_KEY env var
  EXIT: 0
  PASS

Test 5 - check-cmd works correctly:
  PASS: check-cmd finds existing command (bb)
  PASS: check-cmd returns false for missing command
  EXIT: 0
  PASS

Test 6 - deps/check! reports missing dependencies:
  [critical] ok-voice: Missing dependencies:
    - PipeWire (install: pipewire)
    - xdotool (install: xdotool)
    - libnotify (install: libnotify)
  EXIT: 1
  PASS

Test 7 - Full bb -m ok-voice.core entry point:
  Correctly calls deps/check! first, exits 1 on missing deps
  PASS

Test 8 - Full flow (skipping deps check):
  [normal] ok-voice: ok-voice ready
  ok-voice ready
  EXIT: 0
  PASS

config.example.yaml: FOUND
```

## Commit
- cfa5b6d: feat(01-02): add config loading, dependency validation, and app entry point

## Status: COMPLETE
