# tts-server

Local text-to-speech server with FastAPI. Default engine is Piper for low
latency, with optional Coqui TTS support.

## Quick Start

Install dependencies:

```bash
uv sync
```

Start the development server:

```bash
uv run fastapi dev
```

Open http://localhost:8000/docs

## System Requirements

- ffmpeg must be available in PATH for mp3/ogg output.

## API

Generate speech:

```bash
curl -X POST http://localhost:8000/api/v1/tts \
	-H "Content-Type: application/json" \
	-d '{"text":"Raise elbow higher","intent":"up","format":"wav","emphasis_words":["higher"]}' \
	--output output.wav
```

List voices:

```bash
curl http://localhost:8000/api/v1/voices
```

Compatibility route:

```bash
curl -X POST http://localhost:8000/tts \
	-H "Content-Type: application/json" \
	-d '{"text":"Keep your elbow higher","intent":"up"}' \
	--output output.wav
```

## Voices

Edit config/voices.yaml to change the default voices. Models are downloaded
to models/ on first use.

Default voices:

- English: en_US-ryan-high
- Vietnamese: vi_VN-vais1000-medium
