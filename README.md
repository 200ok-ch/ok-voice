# ok-voice

Press a hotkey, speak, text appears at your cursor. Voice-to-text for Linux/X11 using OpenAI's Realtime Transcription API.

- **Toggle mode**: first press starts recording, second press stops
- **Types directly** into whichever window was focused when you started
- **Auto-detects language** — no configuration needed
- **Streams in real-time** — text appears as you speak

## Requirements

- [Babashka](https://github.com/babashka/babashka) (bb)
- PulseAudio (`parecord` from `pulseaudio-utils`)
- `xdotool`
- `xclip`
- `notify-send` (from `libnotify-bin` or `libnotify`)
- An [OpenAI API key](https://platform.openai.com/api-keys) with access to the Realtime API

On Debian/Ubuntu:

```sh
sudo apt install pulseaudio-utils xdotool xclip libnotify-bin
```

## Setup

1. **Create the config directory and file:**

```sh
mkdir -p ~/.config/ok-voice
cp resources/config.example.yaml ~/.config/ok-voice/config.yaml
```

2. **Add your OpenAI API key:**

```sh
$EDITOR ~/.config/ok-voice/config.yaml
```

Set `api-key` to your key:

```yaml
api-key: "sk-..."
```

Alternatively, set the `OPENAI_API_KEY` environment variable.

3. **Bind a hotkey with xbindkeys:**

Install xbindkeys if you don't have it:

```sh
sudo apt install xbindkeys
```

Add to `~/.xbindkeysrc`:

```
"bb --config /path/to/ok-voice/bb.edn ok-voice"
    Mod4 + v
```

Replace `/path/to/ok-voice` with the actual path to this repository. `Mod4 + v` is Super+V — change it to whatever you prefer.

Then reload xbindkeys:

```sh
xbindkeys --poll-rc
```

Now press your hotkey to start dictating and press it again to stop. The transcribed text will be typed into the window that was focused when you started.

## Usage

You can also run it directly from the project directory:

```sh
bb ok-voice
```

Run it once to start recording, run it again to stop. The second invocation signals the running instance to shut down gracefully.

## How it works

1. On first invocation, ok-voice captures audio from your default PulseAudio input device
2. Audio is streamed over a WebSocket to OpenAI's Realtime Transcription API
3. As transcription results arrive, text is typed into the original window via `xdotool`
4. On second invocation (or SIGTERM), recording stops and the process exits

## License

MIT
