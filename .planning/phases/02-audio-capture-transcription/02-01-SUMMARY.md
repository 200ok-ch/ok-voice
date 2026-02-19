# Plan 02-01 Summary: PipeWire Audio Capture Module

PCM16 24kHz mono audio capture via pw-record subprocess with base64-encoded chunking for OpenAI Realtime API.

## What was done
- Created audio.clj with pw-record subprocess management (start!/stop!)
- Implemented chunked reading: 4800 bytes raw (100ms at 24kHz 16-bit mono) to 6400 chars base64
- Provided stream-chunks! loop function for callers to run in a future
- Module is stateless: caller manages process lifecycle and threading

## Files created
- src/ok_voice/audio.clj

## Public API
- `(audio/start!)` -- spawns pw-record with PCM16 24kHz mono, returns process object
- `(audio/stop! proc)` -- destroys process tree cleanly, returns nil
- `(audio/read-chunk proc)` -- reads one 100ms chunk, returns base64 string or nil
- `(audio/stream-chunks! proc callback)` -- blocking loop calling callback per chunk

## Deviations from Plan

None -- plan executed exactly as written.

## Verification results

```
Module loaded successfully
start! defined: true
stop! defined: true
read-chunk defined: true
stream-chunks! defined: true
Base64 of 4800 zero bytes: 6400 chars
All verifications passed
```

Note: Full pw-record integration test requires PipeWire audio stack (not available in sandboxed environment). Module loads, all functions are defined, and base64 encoding produces correct output size.

## Decisions

- Kept module completely stateless per plan: no atoms, no notifications, no device selection
- chunk-size computed as constant (4800 bytes = 24000 Hz * 1 ch * 2 bytes * 0.1s)

## Commit
- 8d8c2d9: feat(02-01): add PipeWire audio capture module

## Duration: ~1 min

## Self-Check: PASSED
