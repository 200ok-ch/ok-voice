# AGENTS.md - ok-voice Codebase Guide

## Project Overview

ok-voice is a Babashka application for voice-to-text transcription using a configurable OpenAI-compatible Whisper endpoint. It records a WAV through PulseAudio, uploads it after the user stops recording, and pastes the UTF-8 transcript into the original X11 window.

## Build/Lint/Test Commands

### Running the Application
```bash
bb ok-voice          # Run the main application
bb run ok-voice      # Alternative task syntax
```

### Development
```bash
bb -e "(require '[ok-voice.core :as core])"   # Quick REPL evaluation
bb -f src/ok_voice/core.clj                   # Run a specific file
```

### Checking Dependencies
```bash
bb print-deps                                 # Print classpath and dependencies
bb tasks                                      # List available tasks
```

### Testing
Currently no test suite is configured. When adding tests:
```bash
bb test                                       # Run all tests (when configured)
```

### Linting (if clj-kondo is available)
```bash
clj-kondo --lint src                          # Lint all source files
clj-kondo --lint src/ok_voice/core.clj        # Lint single file
```

## Code Style Guidelines

### Namespace Structure
- Namespace names use lowercase with hyphens: `ok-voice.core`, `ok-voice.transcription`
- Directory structure mirrors namespace: `src/ok_voice/core.clj`
- All source files live in `src/ok_voice/`

### Imports/Requires
```clojure
(ns ok-voice.example
  (:require [babashka.process :as p]           ; alias is shortened name
            [clojure.string :as str]           ; common abbreviations: str, io
            [clojure.java.io :as io]
   [ok-voice.notify :as notify]))     ; local namespaces use full name
  (:import [java.net URI]))           ; Java interop imports
```

- Group requires alphabetically within each category
- Use `:as` for aliases consistently
- Use `:refer` sparingly, only for very common functions (e.g., `[sh]`)
- Import Java classes at the end with `:import`

### Naming Conventions

| Type | Convention | Example |
|------|-----------|----------|
| Functions | kebab-case | `start-audio!`, `read-chunk` |
| Side-effecting functions | trailing `!` | `write-pid!`, `stop!`, `connect!` |
| Predicates | trailing `?` | `process-alive?`, `ready?` |
| Private vars | `^:private` metadata | `(def ^:private sample-rate 24000)` |
| Constants | `^:private` with descriptive names | `(def ^:private api-url "...")` |
| Atoms/refs | descriptive names | `text-buffer`, `pipeline` |

### Formatting

- Indent with 2 spaces
- Function arguments aligned when breaking lines:
```clojure
(defn transcribe!
  [api-url api-key model file]
  (let [boundary (str "ok-voice-" (UUID/randomUUID))
        body (multipart-body file model boundary)]
    ...))
```

- Collection literals: align map keys, one pair per line for 3+ entries
- Thread-first `->` for data transformations
- Thread-last `->>` for sequence operations

### Type Hints
Use type hints for performance-critical Java interop:
```clojure
(defn- encode-base64 [^bytes ba]
  (.encodeToString encoder ba))

(defn read-chunk [audio-proc]
  (let [in ^java.io.InputStream (:out audio-proc)]
    ...))
```

### Error Handling

1. **Fatal errors**: Use `System/exit 1` with user notification:
```clojure
(when error-condition
  (notify/error "ok-voice" "Error message")
  (System/exit 1))
```

2. **Recoverable errors**: Return nil or use try/catch:
```clojure
(try
  (let [result (sh "which" cmd)]
    (zero? (:exit result)))
  (catch Exception _
    false))
```

3. **Silent failures**: Use catch-all for cleanup operations:
```clojure
(catch java.io.IOException _
  nil)
```

4. **Debug logging**: Write to stderr with binding:
```clojure
(binding [*out* *err*]
  (println "[error]" msg))
```

### Documentation

- Add docstrings for public functions:
```clojure
(defn transcribe!
  "Uploads a completed WAV file to an OpenAI-compatible transcription endpoint."
  [api-url api-key model file]
  ...)
```

- Include parameter descriptions for complex functions
- Document return values and nil handling

### Runtime State

- Keep runtime coordination files under `$XDG_RUNTIME_DIR`, with `/tmp` as a fallback
- The second invocation writes a stop request for the recording process
- Remove PID, stop-request, and temporary WAV files in `finally` blocks:
```clojure
(finally
  (audio/stop! recording)
  (audio/delete! recording)
  (toggle/remove-pid!))
```

### Process/External Command Handling

- Use `babashka.process/sh` for synchronous commands
- Use `babashka.process/process` for async/streaming processes
- Always clean up child processes: `p/destroy-tree proc`
- Check exit codes: `(zero? (:exit result))`

### File Organization

Each module should have a single responsibility:
- `core.clj` - Main entry point and orchestration
- `config.clj` - Configuration loading and validation
- `audio.clj` - Audio capture via parecord
- `transcription.clj` - OpenAI-compatible multipart HTTP transcription
- `text.clj` - UTF-8 clipboard transfer and application-aware paste
- `notify.clj` - Desktop notifications
- `toggle.clj` - Process management (single instance)
- `deps.clj` - Dependency checking

### Key Dependencies

- `babashka.process` - Shell command execution
- `clj-yaml.core` - YAML parsing for config
- `cheshire.core` - JSON encoding/decoding
- `java.net.http` - Standard HTTP client
- `clojure.java.io` - File operations
- `clojure.string` - String utilities
