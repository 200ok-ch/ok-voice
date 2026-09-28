# Phase 4 Research: Text Output

## Goal
Insert transcribed text at cursor position in the active X11 window.

## Approach: xdotool type vs clipboard paste

### xdotool type --file -
- Simulates keystrokes via XTest extension
- Uses XStringToKeysym → XKeysymToKeycode mapping
- **Problem**: Characters not in the current keymap are silently dropped
- Non-Latin scripts (German umlauts, CJK, Cyrillic, etc.) often fail
- Since ok-voice auto-detects language (CORE-05), non-ASCII is expected

### Clipboard paste (xclip + xdotool key ctrl+v)
- Set clipboard: `echo -n "text" | xclip -selection clipboard` (or pipe via stdin)
- Paste: `xdotool key --clearmodifiers ctrl+v`
- **Reliable for all Unicode** — clipboard is encoding-aware
- Works with all X11 applications that support ctrl+v paste
- Overwrites user's clipboard — acceptable trade-off (transcribed text in clipboard is useful)
- `--clearmodifiers` releases any held modifier keys before simulating ctrl+v

**Decision: Clipboard approach** — only reliable method for multi-language support.

## Focus handling

Trace through the toggle flow:
1. User typing in text editor, presses xbindkeys hotkey
2. xbindkeys spawns `bb -m ok-voice.core` (second invocation) — no window created
3. Second invocation sends SIGTERM to first instance, exits immediately
4. First instance's shutdown hook fires: stop pipeline → get text → **insert text** → notify
5. Text editor still has focus throughout — no window was created or focused

**No special focus handling needed** — the bb process is headless.

## Timing considerations

- xclip sets clipboard: ~1ms
- xdotool key ctrl+v: ~5ms
- Small safety delay (50ms) between set and paste for X11 event propagation
- Total: <100ms — imperceptible to user

## Dependencies

- `xdotool` — already in deps.clj
- `xclip` — NEW, must add to deps.clj (`apt install xclip`)

## Edge cases

- Empty transcription: no-op (don't paste empty string)
- Multiline text: clipboard handles newlines natively
- Very long text: clipboard has no practical size limit for text
- Application doesn't support ctrl+v: rare on X11, acceptable limitation
