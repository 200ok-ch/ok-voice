# ok-voice

`ok-voice` is a toggle-driven voice-to-text application for Linux on X11. The
first invocation starts recording. The second stops recording, sends the WAV
file to an OpenAI-compatible Whisper endpoint, and pastes the returned text
into the window that was active when recording started.

Transcription is performed as one standard `POST /v1/audio/transcriptions`
request after recording stops. The application does not use the OpenAI
Realtime API or WebSockets.

## Requirements

- [Babashka](https://babashka.org/)
- `parecord` from PulseAudio utilities
- `xdotool`
- `xclip`
- `notify-send` from libnotify
- An X11 desktop session
- An OpenAI-compatible audio transcription endpoint and Bearer credential

On Debian-derived systems, the system dependencies are typically available
from `pulseaudio-utils`, `xdotool`, `xclip`, and `libnotify-bin`.

## Configuration

Create `~/.config/ok-voice/config.yaml` with mode `600`:

```yaml
api-url: "https://whisper-turbo.twohundredok.com/v1/audio/transcriptions"
api-key: "your-api-key-here"
model: "large-v3-turbo"
allow-insecure-http: false
```

`api-url` must be the complete transcription endpoint, including
`/v1/audio/transcriptions`. The request uses the OpenAI multipart fields
`file`, `model`, and `response_format=json`, with Bearer authentication.

The following environment variables are used when the corresponding config
value is absent:

| Config | Environment | Default |
| --- | --- | --- |
| `api-url` | `OPENAI_API_URL` | `https://api.openai.com/v1/audio/transcriptions` |
| `api-key` | `OPENAI_API_KEY` | none |
| `model` | `OPENAI_MODEL` | `whisper-1` |

Remote plain-HTTP endpoints are rejected because they expose the credential
and recorded audio. HTTPS is recommended. Localhost HTTP is allowed; another
HTTP endpoint requires `allow-insecure-http: true`.

Never commit the real user configuration or an API key. The tracked
`resources/config.example.yaml` contains placeholders only.

## Usage

Run the application once to start recording:

```sh
bb -m ok-voice.core
```

Run the same command again to stop and transcribe. Notifications indicate the
recording, transcription, completion, and error states. Temporary WAV and
runtime state files are removed when processing completes.

For xbindkeys:

```text
"cd ~/src/200ok/ok-voice/ && bb -m ok-voice.core"
    F9
```

The Babashka task is also available as `bb ok-voice`.

## Text insertion

The transcript is transferred as UTF-8 through the X11 clipboard rather than
being typed as synthetic character keypresses. X11 keyboard events cannot
represent arbitrary Unicode reliably; direct `xdotool type` input can corrupt
characters such as German umlauts.

`ok-voice` restores the previous textual clipboard after pasting. It uses
`Shift+Insert` for normal X11 applications and `Ctrl+Shift+V` for Kitty,
selected from the target window's X11 class. `xdotool` remains responsible
only for restoring window focus and sending the paste shortcut.

## Development checks

```sh
clj-kondo --lint src --fail-level warning
git diff --check
```
