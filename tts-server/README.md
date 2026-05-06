# HoopMaster TTS Server

A high-performance, asynchronous Text-To-Speech (TTS) server built with **FastAPI**. This service is designed to provide real-time, expressive audio feedback for basketball training sessions within the HoopMaster ecosystem.

## 🛠 Tech Stack

*   **Runtime:** Python 3.10 / 3.11 (3.11 is highly recommended).
*   **Web Framework:** FastAPI (Asynchronous native).
*   **Package Management:** [uv](https://github.com/astral-sh/uv) by Astral.
*   **TTS Engines:**
    *   **Piper:** Extremely low-latency, ONNX-based local TTS.
    *   **Coqui / StyleTTS2:** High-quality voice synthesis with expressive prosody.
*   **Audio Backend:** FFmpeg.

## 📋 System Prerequisites

### 1. FFmpeg Installation
FFmpeg is mandatory for audio stream encoding and format conversion.
*   **Windows:** Download from [gyan.dev](https://www.gyan.dev/ffmpeg/builds/), extract, and add the `bin` folder to your **System PATH**.
*   **Linux/WSL2:** Run `sudo apt update && sudo apt install ffmpeg`.

### 2. Python Version
Ensure you are using **Python 3.11**. Versions 3.12+ currently have compatibility issues with `onnxruntime` and `coqui-tts` dependencies.

## 🚀 Getting Started

### 1. Environment Setup
We use `uv` for lightning-fast dependency management. If you don't have it: `pip install uv`.

```bash
# Navigate to the server folder
cd tts-server

# Create virtual environment
uv venv --python 3.11

# Activate the environment
# Windows (PowerShell):
.venv\Scripts\activate
# Linux/WSL2:
source .venv/bin/activate

# Sync dependencies
uv sync
```

### 2. Critical Patch for Model Downloads
To prevent `302 Found` errors when the server automatically downloads models from Hugging Face during startup, ensure `app/utils/files.py` is configured to follow redirects:

```python
# app/utils/files.py
import httpx

with httpx.stream("GET", url, timeout=60.0, follow_redirects=True) as response:
    response.raise_for_status()
    # ... logic ...
```

### 3. Running the Server
Launch the server in development mode:

```bash
uv run fastapi dev
```
*Note: On the first run, the server will download the default voice models (~100MB+). This may take a few minutes depending on your connection.*

## 🔌 API Usage

Once the server is up, visit `http://127.0.0.1:8000/docs` for the interactive Swagger UI.

### Endpoint: `POST /v1/tts/generate`
Generates audio with the ability to emphasize specific words.

**Request Payload:**
```json
{
  "text": "Raise your elbow higher and snap your wrist.",
  "voice_id": "en_US-ryan-high",
  "emphasis_words": ["higher", "snap"],
  "output_format": "wav"
}
```

**Features:**
*   **Chunked Streaming:** The server uses `StreamingResponse`, allowing the app to start playing audio while it is still being generated.
*   **Prosody Control:** Uses `emphasis_words` to dynamically adjust the pitch and volume of critical coaching cues.

## 🔧 Troubleshooting

*   **isort connection errors:** In VS Code, ensure your Python Interpreter is set to the `.venv` inside the `tts-server` folder. Run `uv add isort` if the extension continues to fail.
*   **VIRTUAL_ENV mismatch:** If `uv` gives a warning, run `deactivate` and then re-activate the environment specifically from within the `tts-server` directory.
*   **Hugging Face 302 Error:** Ensure `follow_redirects=True` is added to your `httpx` client calls in the utility services.

## 📂 Project Structure

```text
tts-server/
├── app/
│   ├── core/           # Lifespan management (Preloading models)
│   ├── services/       # TTS Engine wrappers (Piper/Coqui)
│   ├── storage/        # Model file store and disk cache
│   ├── utils/          # Networking and audio processing utilities
│   └── main.py         # App initialization
├── models/             # Local cache for downloaded .onnx models
├── pyproject.toml      # Dependency definitions
└── README.md
```

---
*Maintained by the HoopMaster Development Team.*
