# Feature Landscape: Linux Voice-to-Text CLI Tools

**Domain:** Linux voice dictation/voice-to-text applications
**Researched:** 2026-02-19
**Confidence:** HIGH (verified from official GitHub repos and documentation)

## Table Stakes (Users Expect These)

Features users assume exist. Missing these = product feels incomplete.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| **Hotkey-triggered recording** | Core workflow: press key → speak → text appears | LOW | ok-voice delegates to xbindkeys, provides toggle mode |
| **Text insertion at cursor** | The entire point of voice typing | MEDIUM | xdotool for X11, wtype/ydotool for Wayland |
| **Offline/local processing option** | Privacy concerns, no internet dependency | HIGH | Users expect privacy-first design |
| **Multi-language support** | Global user base | MEDIUM | Auto-detect or explicit language selection |
| **Configuration file** | API keys, preferences, model selection | LOW | YAML/TOML config in ~/.config/ |
| **Basic error feedback** | Know when things fail | LOW | Desktop notifications (notify-send) |
| **Auto-detect language** | Don't force users to switch manually | MEDIUM | Whisper supports this natively |

### Table Stakes Analysis

For ok-voice specifically:
- **Hotkey handling:** Already delegated to xbindkeys ✓
- **Text insertion:** Using xdotool ✓
- **Offline processing:** Using OpenAI Realtime API (cloud) - may need local fallback
- **Multi-language:** Need to implement auto-detect
- **Config file:** Need to implement
- **Error feedback:** Desktop notifications ✓ (noted in project context)

## Differentiators (Competitive Advantage)

Features that set the product apart. Not expected, but valued.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| **Real-time/live transcription** | See words appear as you speak, not after | HIGH | OpenAI Realtime API provides this - major advantage |
| **Push-to-talk vs toggle mode** | Natural workflow, no accidental activations | LOW | ok-voice uses toggle (press to start, press to stop) |
| **Recording feedback** | Audio/visual cues that recording is active | MEDIUM | Sound, tray icon change, or visual indicator |
| **Spoken punctuation** | Say "period" → ".", "new line" → "\n" | MEDIUM | Significant UX improvement |
| **Word replacements/custom dictionary** | Correct common misrecognitions | MEDIUM | Domain-specific vocabulary |
| **Low latency** | Sub-second response feels magical | HIGH | OpenAI Realtime API provides this |
| **Systemd daemon/autostart** | Always ready, no manual launch | LOW | Run in background from login |
| **High accuracy models** | Whisper large, GPT-4o-transcribe | MEDIUM | Cloud API gives best accuracy without local GPU |
| **Minimal UI** | Non-intrusive, stays out of way | LOW | ok-voice already targets this |
| **X11-specific optimization** | Works reliably on X11/i3 without Wayland complexity | LOW | Focused scope, avoid Wayland headaches |

### Differentiator Analysis for ok-voice

**Key advantage: OpenAI Realtime Transcription API**
- Real-time streaming transcription (words appear as you speak)
- Low latency (~300-500ms end-to-end)
- High accuracy without local GPU
- This is a MAJOR differentiator vs offline tools

**Recommended priorities:**
1. Lean into real-time transcription advantage
2. Add recording feedback (audio beep on start/stop)
3. Add spoken punctuation support
4. Systemd daemon for "always ready" experience

## Anti-Features (Commonly Requested, Often Problematic)

Features to explicitly NOT build. Common mistakes in this domain.

| Anti-Feature | Why Requested | Why Problematic | Alternative |
|--------------|---------------|-----------------|-------------|
| **Built-in hotkey handling** | "Self-contained" appeal | Duplicates system functionality, conflicts, inconsistent behavior | Delegate to xbindkeys/sxhkd/compositor keybindings |
| **Full GUI interface** | "Easier to use" | Against project philosophy, maintenance burden, not needed for CLI tool | Minimal UI: notifications only, config file for settings |
| **Always-on listening** | "Hands-free" appeal | Privacy concerns, false triggers, constant CPU/battery drain | Push-to-talk/toggle mode gives user control |
| **Account/billing system** | Revenue potential | Massive complexity, authentication, security, compliance | BYOK (bring your own key) - user provides API key |
| **Cross-platform support (Wayland)** | "Broader audience" | Wayland has different text injection (wtype/ydotool), more complexity | Stay focused on X11, let others solve Wayland |
| **File transcription mode** | "Transcribe recordings" | Different use case, different UX, scope creep | Focus on real-time dictation only |
| **Voice commands (beyond punctuation)** | "Control the computer" | Complex, unreliable, different product category | Focus on text input only |

### Anti-Feature Rationale for ok-voice

1. **No built-in hotkey handling** - xbindkeys already does this well. Reimplementing is scope creep.
2. **No GUI** - Project explicitly states "no GUI". CLI + notifications is the design.
3. **No always-on** - Toggle mode gives user explicit control, avoids privacy concerns.
4. **No account system** - BYOK with config file. Zero infrastructure needed.
5. **X11 only** - Wayland support requires ydotool/wtype and dealing with compositor differences. Stay focused.

## Feature Dependencies

```
Configuration File (API key)
    └──required──> OpenAI Realtime API connection
                       └──required──> Real-time transcription
                                            └──enhances──> Low latency experience

Toggle Mode (xbindkeys trigger)
    └──requires──> Text insertion (xdotool)
    └──enhances──> Recording feedback (audio/notification)

Spoken Punctuation
    └──requires──> Text post-processing
    └──requires──> Punctuation mapping dictionary

Auto-detect Language
    └──requires──> OpenAI Realtime API language detection

Systemd Daemon
    └──requires──> Config file
    └──requires──> Proper signal handling (SIGTERM, SIGINT)
```

### Dependency Notes

- **Config file requires API key**: Without API key, tool cannot function
- **Toggle mode requires text insertion**: The whole point is text appears somewhere
- **Recording feedback enhances UX**: Not strictly required, but users expect confirmation
- **Systemd daemon requires graceful shutdown**: Must handle signals properly for clean restarts

## MVP Definition

### Launch With (v1)

Minimum viable product — what's needed to validate the concept.

- [x] **Toggle mode recording** — Press hotkey to start, press again to stop (via xbindkeys)
- [x] **OpenAI Realtime Transcription** — Streaming transcription, not batch
- [x] **Text insertion at cursor** — Via xdotool for X11
- [ ] **Configuration file** — Store API key, preferences
- [ ] **Basic error notifications** — notify-send when things fail
- [ ] **Auto-detect language** — Use OpenAI's language detection

### Add After Validation (v1.x)

Features to add once core is working.

- [ ] **Recording feedback** — Audio beep on start/stop, or visual indicator
- [ ] **Spoken punctuation** — "period" → ".", "comma" → ",", "new line" → "\n"
- [ ] **Systemd user service** — Autostart, always ready
- [ ] **Status indicator** — Tray icon or minimal indicator showing active/inactive
- [ ] **Custom word replacements** — Fix common misrecognitions

### Future Consideration (v2+)

Features to defer until product-market fit is established.

- [ ] **Local fallback model** — Whisper.cpp for offline operation when API unavailable
- [ ] **Multiple API providers** — Support Groq, Anthropic, or custom endpoints
- [ ] **Session logging** — Optional history of transcriptions
- [ ] **Voice activity detection** — Auto-stop when silence detected
- [ ] **CLI status output** — Rich terminal UI for status/feedback

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| Toggle mode + xdotool | HIGH | LOW | P1 ✓ |
| Config file (API key) | HIGH | LOW | P1 |
| Error notifications | MEDIUM | LOW | P1 |
| Auto-detect language | HIGH | LOW | P1 |
| Recording feedback (audio) | MEDIUM | LOW | P2 |
| Spoken punctuation | HIGH | MEDIUM | P2 |
| Systemd daemon | MEDIUM | LOW | P2 |
| Custom word replacements | MEDIUM | MEDIUM | P3 |
| Local fallback model | HIGH | HIGH | P3 |
| Voice activity detection | LOW | MEDIUM | P3 |

**Priority key:**
- P1: Must have for launch
- P2: Should have, add when possible
- P3: Nice to have, future consideration

## Competitor Feature Analysis

| Feature | VOXD | Voxtype | OpenWhispr | ok-voice (planned) |
|---------|------|---------|------------|-------------------|
| Engine | whisper.cpp | whisper.cpp | Whisper/Parakeet | OpenAI Realtime API |
| UI | CLI/GUI/Tray | CLI only | GUI (Electron) | CLI + notifications |
| Text output | ydotool | wtype/ydotool | xdotool/wtype | xdotool |
| Real-time transcription | No (batch) | No (batch) | No (batch) | **Yes** ✓ |
| Push-to-talk | Yes | Yes | Yes | Toggle mode |
| Recording feedback | Audio + notifications | Audio + notifications | Visual | Planned |
| Languages | 99+ | 99+ | 58+ | Via API |
| Offline option | Yes | Yes | Yes | Not yet |
| GPU acceleration | Yes | Yes | No | N/A (cloud) |
| AI post-processing | Yes (AIPP) | Yes (LLM pipe) | Yes (multi-provider) | Not planned |
| Systemd daemon | No (manual) | Yes | No (Electron) | Planned |
| Installation | pip/setup.sh | Binary/package | Electron app | Binary/script |

### Competitive Position for ok-voice

**Unique advantages:**
1. **Real-time transcription** - Only tool using OpenAI Realtime API for live dictation
2. **Minimal dependencies** - No GUI framework, no Python, no Electron
3. **Cloud-powered accuracy** - Best-in-class transcription without local GPU
4. **X11/i3 focused** - Purpose-built for this use case, not a cross-platform compromise

**Trade-offs:**
- Requires internet (no offline mode)
- Requires OpenAI API key
- X11 only (no Wayland)

## Sources

- **VOXD**: https://github.com/jakovius/voxd (GitHub README, Feb 2026)
- **Voxtype**: https://voxtype.io/ (Official site, Feb 2026)
- **Voxtype comparison**: https://voxtype.io/compare/nerd-dictation.html
- **Vosk CLI Dictation**: https://vosk.davalan.fr/ (Official site, 2025)
- **OpenWhispr**: https://github.com/OpenWhispr/openwhispr (GitHub README, Feb 2026)
- **Nerd Dictation**: https://github.com/ideasman42/nerd-dictation (GitHub)
- **Reddit discussions**: r/linux, r/archlinux voice dictation threads (2025-2026)

---
*Feature research for: Linux voice-to-text CLI tools*
*Researched: 2026-02-19*
