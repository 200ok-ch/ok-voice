# Pitfalls Research

**Domain:** Linux voice-to-text (CLI, X11/i3, OpenAI Realtime API, xdotool)
**Researched:** 2026-02-19
**Confidence:** HIGH (verified with official docs and community reports)

---

## Critical Pitfalls

Mistakes that cause rewrites or major issues.

### Pitfall 1: Audio Format Mismatch with OpenAI Realtime API

**What goes wrong:**
Sending audio in the wrong format causes garbled transcription, "gibberish" output, or complete failure. The API is strict about format requirements.

**Why it happens:**
Linux audio systems (ALSA, PulseAudio, PipeWire) each have different default formats, sample rates, and channel configurations. Developers assume "audio is audio" without verifying the exact specs OpenAI expects.

**Consequences:**
- Transcription produces nonsense ("gibberish")
- API may accept audio but produce wrong results
- Debugging is difficult because error messages are often unclear

**How to avoid:**
- Use **PCM16 format** (16-bit signed little-endian)
- Use **24kHz sample rate** for Realtime API (note: 16kHz for transcription-only endpoint)
- Use **mono channel** (single channel, not stereo)
- Verify with `arecord --format=S16_LE --rate=24000 --channels=1 test.wav`
- Base64-encode the raw PCM bytes for WebSocket transmission

**Warning signs:**
- Transcription output looks like random words or nonsense
- Transcription works for some inputs but not others
- Audio sounds correct when played back locally

**Phase to address:** Phase 1 (MVP - Audio Capture)

**Sources:**
- [OpenAI Realtime API Reference](https://platform.openai.com/docs/api-reference/realtime) - HIGH confidence
- [Microsoft Azure Realtime Audio Guide](https://learn.microsoft.com/en-us/azure/ai-foundry/openai/how-to/realtime-audio) - HIGH confidence
- [OpenAI Community: Audio Format Issues](https://community.openai.com/t/low-and-slow-audio-from-realtime-api-how-to-properly-audio-format/1011061) - MEDIUM confidence

---

### Pitfall 2: xdotool Unicode/Character Encoding Failures

**What goes wrong:**
Transcribed text with special characters (accents, emoji, non-ASCII) either fails silently or produces garbage characters. Text appears corrupted in the target application.

**Why it happens:**
xdotool's `type` command has poor Unicode support. It sends keystrokes based on the current keyboard layout, not direct character insertion. Multi-byte sequences cause "Invalid multi-byte sequence encountered" errors.

**Consequences:**
- Non-English text fails completely
- Punctuation and special symbols may be wrong or missing
- Application appears broken for international users

**How to avoid:**
1. **Preferred: Use clipboard paste instead of `type`**
   ```bash
   echo -n "$text" | xclip -selection clipboard
   xdotool key --window <window> ctrl+shift+v
   ```
2. **If must use `type`:**
   - Add delay: `xdotool type --delay 10 "$text"`
   - Test extensively with your target character set
   - Consider `ydotool` as a Wayland-compatible alternative

**Warning signs:**
- "Invalid multi-byte sequence" error from xdotool
- Characters appear wrong or missing in target app
- Works in some apps but not others

**Phase to address:** Phase 2 (Text Insertion)

**Sources:**
- [xdotool GitHub Issue #154](https://github.com/jordansissel/xdotool/issues/154) - HIGH confidence
- [AskUbuntu: xdotool Unicode characters](https://askubuntu.com/questions/591686/how-to-make-xdotool-type-unicode-characters) - MEDIUM confidence
- [Unix.SE: xdotool weird characters](https://unix.stackexchange.com/questions/713752) - MEDIUM confidence

---

### Pitfall 3: xdotool Race Condition - First Characters Lost

**What goes wrong:**
When typing via xdotool, the first few characters are lost or appear in the wrong window. This happens intermittently and is frustrating to debug.

**Why it happens:**
- Window focus takes time to actually switch
- xdotool may start typing before the target app is fully ready to receive input
- Some applications have focus-change animations or delays

**Consequences:**
- User sees incomplete text: "Hello world" becomes "lo world"
- Text appears in wrong application if focus changed
- Inconsistent behavior makes debugging difficult

**How to avoid:**
```bash
# 1. Get target window
WINDOW=$(xdotool getactivewindow)

# 2. Ensure focus
xdotool windowfocus --sync $WINDOW
sleep 0.1  # Small delay for focus to settle

# 3. Type with delay between characters
xdotool type --delay 12 --window $WINDOW "$text"
```

**Warning signs:**
- First character or first few characters missing
- Text sometimes goes to wrong app
- Works fine when tested manually but fails in production

**Phase to address:** Phase 2 (Text Insertion)

**Sources:**
- [Linux Mint Forums: xdotool hotkey issues](https://forums.linuxmint.com/viewtopic.php?t=452313) - MEDIUM confidence
- [StackOverflow: xdotool focus issues](https://stackoverflow.com/questions/12026953) - MEDIUM confidence

---

### Pitfall 4: WebSocket Connection Drops Mid-Session

**What goes wrong:**
The WebSocket connection to OpenAI's Realtime API silently fails or "freezes" during active use. Audio is being sent but no transcription comes back.

**Why it happens:**
- Network instability causes silent disconnects
- OpenAI has session time limits (15 minutes max)
- TCP timeouts behave differently across platforms
- No built-in health check/heartbeat in some WebSocket libraries

**Consequences:**
- User speaks but nothing happens
- App appears "frozen" with no feedback
- Lost audio data that wasn't transcribed

**How to avoid:**
1. Implement **exponential backoff reconnection**
2. Add **application-level heartbeat/ping**
3. **Queue audio locally** during reconnection attempts
4. Show **clear error state** in UI (desktop notification)
5. Handle the 15-minute session limit with graceful reconnection

```python
# Pseudocode for robust reconnection
async def connect_with_retry():
    for attempt in range(MAX_RETRIES):
        try:
            ws = await connect(REALTIME_URL)
            return ws
        except Exception as e:
            delay = min(2 ** attempt, 30)  # Exponential backoff, max 30s
            await asyncio.sleep(delay)
    raise ConnectionError("Max retries exceeded")
```

**Warning signs:**
- No server events received despite sending audio
- Connection "feels" stuck
- Last successful transcription was a while ago

**Phase to address:** Phase 1 (MVP - WebSocket Management)

**Sources:**
- [OpenAI Community: WebSocket stops receiving](https://community.openai.com/t/realtime-api-bug-websocket-stops-receiving-messages-mid-session/1374520) - HIGH confidence
- [Latent.Space: Realtime API Missing Manual](https://www.latent.space/p/realtime-api) - HIGH confidence
- [StackOverflow: WebSocket reconnection](https://stackoverflow.com/questions/22431751) - MEDIUM confidence

---

### Pitfall 5: Linux Audio Stack Fragmentation (ALSA/PulseAudio/PipeWire)

**What goes wrong:**
Audio capture works on your machine but fails on other Linux setups. Users report "no microphone detected" or silence despite microphone being present.

**Why it happens:**
Linux has multiple audio systems:
- **ALSA**: Kernel-level, direct hardware access
- **PulseAudio**: Desktop sound server (older, still common)
- **PipeWire**: Modern replacement (becoming standard)

Each requires different APIs and configuration. Hardcoding one approach limits compatibility.

**Consequences:**
- App works on Ubuntu but not Fedora
- Works with USB mic but not built-in laptop mic
- Device enumeration fails on some systems

**How to avoid:**
1. **Use PipeWire if available** (it provides PulseAudio compatibility)
2. **Detect and adapt** to available audio system
3. **Test on multiple distros** (Ubuntu, Fedora, Arch)
4. **Provide clear error messages** when no audio device found
5. **Consider using PulseAudio API** (works on PulseAudio AND PipeWire)

```bash
# Check what's available
pactl info 2>/dev/null && echo "PulseAudio/PipeWire available"
arecord -l 2>/dev/null && echo "ALSA available"
```

**Warning signs:**
- Audio works in some apps but not yours
- Error messages about "device not found"
- Works with one microphone but not another

**Phase to address:** Phase 1 (MVP - Audio Capture)

**Sources:**
- [Reddit: ALSA vs PulseAudio vs PipeWire 2025](https://www.reddit.com/r/linuxaudio/comments/1jkvwb6) - MEDIUM confidence
- [ArchWiki: PulseAudio Troubleshooting](https://wiki.archlinux.org/title/PulseAudio/Troubleshooting) - HIGH confidence
- [XDA: PipeWire simplifies Linux audio](https://www.xda-developers.com/goodbye-to-linux-audio-headaches-pipewire-simplifies-everything/) - MEDIUM confidence

---

## Moderate Pitfalls

Mistakes that cause delays or technical debt.

### Pitfall 6: Not Clearing Audio Buffer on Reconnection

**What goes wrong:**
After reconnecting to the Realtime API, old audio from the previous session is sent, causing confusion or errors.

**Prevention:**
- Clear local audio buffer before establishing new connection
- Start fresh audio capture on each new session
- Don't queue audio indefinitely during disconnection

**Phase to address:** Phase 1

---

### Pitfall 7: API Key Security in Config File

**What goes wrong:**
API key is stored in plaintext config file, potentially committed to git or visible to other users on shared system.

**Prevention:**
- Use file permissions: `chmod 600 ~/.config/ok-voice/config`
- Add to `.gitignore`
- Consider environment variable support as fallback
- Warn user if config has wrong permissions

**Phase to address:** Phase 1 (Config)

---

### Pitfall 8: Toggle State Getting Out of Sync

**What goes wrong:**
The app thinks it's recording but isn't, or vice versa. User presses hotkey but nothing happens (or wrong thing happens).

**Why it happens:**
- Error during state transition leaves app in inconsistent state
- Multiple rapid hotkey presses race condition
- Crash during recording leaves state persisted as "recording"

**Prevention:**
- Use immutable state machine for toggle
- Always reset to known state on startup
- Add timeout for recording (auto-stop after N minutes)
- Provide visible indicator of current state

**Phase to address:** Phase 1 (State Management)

---

### Pitfall 9: OpenAI Transcription Quality Issues

**What goes wrong:**
Transcription accuracy is poor - wrong words, language detection failures, or text that doesn't match speech.

**Why it happens:**
- Background noise interferes with recognition
- Language auto-detection picks wrong language
- Speaking too fast or unclear

**Prevention:**
- Provide option to set language explicitly (disable auto-detect)
- Consider noise reduction preprocessing
- Test with user's typical speaking style
- Accept that occasional errors are normal for ASR

**Phase to address:** Phase 2 (Polish)

**Sources:**
- [OpenAI Community: Transcription language detection](https://community.openai.com/t/realtime-transcription-issue/1150994) - MEDIUM confidence

---

### Pitfall 10: No User Feedback During Recording

**What goes wrong:**
User doesn't know if the app is recording, waiting, or errored. They speak and nothing happens, or they speak twice.

**Prevention:**
- Desktop notification when recording starts: "Recording..."
- Desktop notification when recording stops: "Transcribed: [first 50 chars]"
- Clear error notification if something fails
- Consider audio indicator (beep) if desktop notifications not available

**Phase to address:** Phase 1 (UX)

---

## Minor Pitfalls

Mistakes that cause annoyance but are fixable.

### Pitfall 11: xdotool Not Installed / Not in PATH

**What goes wrong:**
App fails to start or fails silently because xdotool isn't available.

**Prevention:**
- Check for xdotool at startup
- Provide clear error message with install instructions
- Consider bundling or documenting dependencies

**Phase to address:** Phase 0 (Setup)

---

### Pitfall 12: Special Character Handling in Shell

**What goes wrong:**
Transcribed text with quotes, backslashes, or shell special characters breaks when passed to xdotool via shell.

**Prevention:**
- Use proper escaping/quoting
- Pass text via stdin or environment variable instead of command line
- Test with edge cases: `"hello 'world' $test \`backtick\`"`

**Phase to address:** Phase 2

---

## Integration Gotchas

Common mistakes when connecting to external services.

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| OpenAI Realtime API | Sending audio before session is ready | Wait for `session.created` event before streaming |
| OpenAI Realtime API | Not handling all error event types | Handle `error` events with proper error codes |
| OpenAI Realtime API | Expecting immediate transcription | Transcription is async; may lag a few seconds |
| xdotool | Assuming X11 always available | Check for X11, fail gracefully on Wayland |
| xdotool | Typing too fast | Use `--delay` option (10-20ms per char) |
| PulseAudio | Assuming default device is correct | Let user configure or auto-detect best input |

---

## Performance Traps

Patterns that work at small scale but fail as usage grows.

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| Unbounded audio buffer | Memory grows, latency increases | Ring buffer with fixed size | Long recording sessions |
| No audio chunking | Large WebSocket frames, lag | Send in small chunks (100ms worth) | Slow networks |
| Synchronous xdotool call | UI freezes during typing | Use async/background process | Long transcriptions |

---

## Security Mistakes

Domain-specific security issues beyond general web security.

| Mistake | Risk | Prevention |
|---------|------|------------|
| API key in git | Key leaked, billing fraud | Add config to .gitignore, warn on commit |
| Config world-readable | Other users can read key | chmod 600 on config file |
| No input validation on transcription | xdotool command injection | Sanitize text before passing to shell/xdotool |

---

## UX Pitfalls

Common user experience mistakes in this domain.

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| No visual/audio feedback | User doesn't know if recording | Desktop notifications + optional beep |
| No error messages | Silent failures frustrate users | Show error notification with actionable message |
| Long startup delay | Feels broken | Show "Connecting..." notification during init |
| Latency visible in text | Feels unresponsive | Show "..." placeholder while transcribing |

---

## "Looks Done But Isn't" Checklist

Things that appear complete but are missing critical pieces.

- [ ] **Audio Capture:** Often missing proper device detection — verify with `pactl list sources`
- [ ] **Transcription:** Often missing error handling for API failures — test with invalid API key
- [ ] **Text Insertion:** Often missing Unicode handling — test with "café", "日本語", emoji
- [ ] **State Management:** Often missing crash recovery — kill app mid-recording, verify clean restart
- [ ] **Network:** Often missing offline handling — test with network disconnected
- [ ] **Config:** Often missing permission checks — verify config file is 600

---

## Recovery Strategies

When pitfalls occur despite prevention, how to recover.

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Audio format wrong | LOW | Fix format spec, redeploy |
| xdotool Unicode fail | MEDIUM | Switch to clipboard approach |
| WebSocket disconnect | LOW | Reconnection logic already in place |
| State out of sync | LOW | Restart app (auto-recover on startup) |
| Wrong audio device | LOW | User reconfigures, or auto-detect on next start |
| API key invalid | LOW | Clear error notification, user updates config |

---

## Pitfall-to-Phase Mapping

How roadmap phases should address these pitfalls.

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| Audio Format Mismatch | Phase 1 (Audio) | Test with real mic, verify transcription |
| xdotool Unicode | Phase 2 (Text) | Test with accented chars, emoji |
| xdotool Race Condition | Phase 2 (Text) | Test rapid focus changes |
| WebSocket Disconnect | Phase 1 (Network) | Kill network mid-session, verify recovery |
| Audio Stack Fragmentation | Phase 1 (Audio) | Test on Ubuntu, Fedora, Arch |
| API Key Security | Phase 1 (Config) | Verify .gitignore, file permissions |
| Toggle State Sync | Phase 1 (State) | Rapid hotkey presses, crash recovery |
| No User Feedback | Phase 1 (UX) | Verify notifications appear |
| xdotool Not Installed | Phase 0 (Setup) | Test on fresh VM |

---

## Sources

### HIGH Confidence (Official Documentation)
- [OpenAI Realtime API Reference](https://platform.openai.com/docs/api-reference/realtime)
- [OpenAI Realtime Transcription Guide](https://developers.openai.com/api/docs/guides/realtime-transcription)
- [Microsoft Azure: GPT Realtime Audio](https://learn.microsoft.com/en-us/azure/ai-foundry/openai/how-to/realtime-audio)
- [ArchWiki: PulseAudio Troubleshooting](https://wiki.archlinux.org/title/PulseAudio/Troubleshooting)

### HIGH Confidence (Expert Analysis)
- [Latent.Space: OpenAI Realtime API - The Missing Manual](https://www.latent.space/p/realtime-api)
- [xdotool GitHub Issues](https://github.com/jordansissel/xdotool/issues)

### MEDIUM Confidence (Community Reports)
- [OpenAI Community: Transcription Errors](https://community.openai.com/t/realtime-api-transcription-errors/975221)
- [OpenAI Community: WebSocket Stops Receiving](https://community.openai.com/t/realtime-api-bug-websocket-stops-receiving-messages-mid-session/1374520)
- [Reddit: Linux Audio 2025](https://www.reddit.com/r/linuxaudio/comments/1jkvwb6/alsa_vs_pulseaudio_vs_jack_vs_pipewire/)
- [AskUbuntu: xdotool Unicode](https://askubuntu.com/questions/591686/how-to-make-xdotool-type-unicode-characters)
- [StackOverflow: WebSocket Reconnection](https://stackoverflow.com/questions/22431751/websocket-how-to-automatically-reconnect-after-it-dies)

### LOW Confidence (Single Source / Community Discussion)
- Various forum posts and GitHub issues cited inline

---
*Pitfalls research for: ok-voice (Linux voice-to-text)*
*Researched: 2026-02-19*
