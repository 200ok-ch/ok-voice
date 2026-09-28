# ok-voice

Press a hotkey, speak, and paste the transcription at your cursor. `ok-voice`
is a toggle-driven voice-to-text application for Linux on X11 and Wayland
using a configurable OpenAI-compatible Whisper endpoint.

- **Toggle mode**: first press starts recording, second press stops
- **Batch transcription**: uploads one completed WAV through the standard
  `/v1/audio/transcriptions` API; no Realtime API or WebSocket
- **Automatic language detection**: no language configuration required
- **Unicode output**: transfers UTF-8 through the desktop clipboard

## Requirements

- [Babashka](https://babashka.org/)
- `parecord` from PulseAudio utilities
- `notify-send` from libnotify
- On X11: `xdotool`, `xclip`, and `xbindkeys` or another hotkey manager
- On Wayland: `wl-clipboard`, `ydotool`, a running `ydotoold`, and a desktop
  hotkey binding
- An OpenAI-compatible transcription endpoint and Bearer credential

On Debian/Ubuntu with X11:

```sh
sudo apt install pulseaudio-utils xdotool xclip libnotify-bin xbindkeys
```

On Debian/Ubuntu with Wayland:

```sh
sudo apt install pulseaudio-utils wl-clipboard ydotool libnotify-bin
```

The user running `ok-voice` must be able to access the `ydotoold` socket.
`ydotool` honors `YDOTOOL_SOCKET` when set and otherwise uses its standard
socket under `$XDG_RUNTIME_DIR`, with `/tmp` as a fallback.

## Setup

Create the configuration directory and copy the example:

```sh
mkdir -p ~/.config/ok-voice
cp resources/config.example.yaml ~/.config/ok-voice/config.yaml
chmod 600 ~/.config/ok-voice/config.yaml
```

Edit `~/.config/ok-voice/config.yaml`:

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

## Hotkey

Add a binding to `~/.xbindkeysrc`:

```text
"cd ~/src/200ok/ok-voice/ && bb -m ok-voice.core"
    F9
```

Change the checkout path and key as needed, then reload xbindkeys:

```sh
xbindkeys --poll-rc
```

## Usage

Run the application once to start recording:

```sh
bb -m ok-voice.core
```

Run the same command again to stop and transcribe. The Babashka task is also
available as `bb ok-voice`. Notifications indicate recording, transcription,
completion, and error states.

## How it works

1. The first invocation records 16 kHz mono audio from the default PulseAudio
   input into a temporary WAV file.
2. The second invocation writes a runtime stop request.
3. The recording process uploads the WAV to the configured
   OpenAI-compatible endpoint and reads the JSON `text` response.
4. The focused window receives the transcript through the UTF-8 clipboard.
5. Temporary WAV and runtime state files are removed.

## Text insertion

The transcript is transferred through the desktop clipboard rather than typed
as synthetic character keypresses. Character key events cannot represent
arbitrary Unicode reliably and can corrupt characters such as German umlauts.

`ok-voice` restores the previous textual clipboard after pasting. On X11, it
uses `Shift+Insert` for normal applications and `Ctrl+Shift+V` for Kitty,
selected from the target window's X11 class; `xdotool` restores window focus
and sends the shortcut. On Wayland, `wl-clipboard` negotiates an available text
MIME type and `ydotool` sends `Shift+Insert` to the focused window.

## Development

```sh
clj-kondo --lint src --fail-level warning
git diff --check
```

## Vibe coded

This project is 100% vibe coded with
[GSD](https://github.com/btheroux/get-shit-done). The full planning artifacts
are in the [`.planning/`](.planning/) folder.

## License

AGPL-3.0 - see [LICENSE](LICENSE) for details.
