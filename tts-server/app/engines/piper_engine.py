from __future__ import annotations

from io import BytesIO
import wave

from app.core.errors import ModelLoadError
from app.engines.base import SynthesisRequest, SynthesisResult, TTSEngine
from app.storage.model_store import ModelStore


class PiperEngine(TTSEngine):
    name = "piper"
    supports_rate = True

    def __init__(self, model_store: ModelStore) -> None:
        self._model_store = model_store

    def synthesize(self, request: SynthesisRequest) -> SynthesisResult:
        voice = self._model_store.get_piper_voice(request.voice)
        length_scale = 1.0 / request.rate if request.rate > 0 else 1.0
        syn_config = self._model_store.create_piper_config(
            length_scale=length_scale,
            noise_scale=0.667,
            noise_w_scale=0.8,
            normalize_audio=True,
        )
        buffer = BytesIO()
        try:
            with wave.open(buffer, "wb") as wav_file:
                voice.synthesize_wav(
                    request.text, wav_file, syn_config=syn_config
                )
        except Exception as exc:
            raise ModelLoadError("Piper synthesis failed") from exc
        wav_bytes = buffer.getvalue()
        with wave.open(BytesIO(wav_bytes), "rb") as wav_reader:
            sample_rate = wav_reader.getframerate()
            sample_width = wav_reader.getsampwidth()
            channels = wav_reader.getnchannels()
        return SynthesisResult(
            audio_bytes=wav_bytes,
            sample_rate=sample_rate,
            sample_width=sample_width,
            channels=channels,
        )
