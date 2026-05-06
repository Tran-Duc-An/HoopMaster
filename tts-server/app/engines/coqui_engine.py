from __future__ import annotations

import inspect

from app.core.errors import EngineNotAvailableError, ModelLoadError
from app.engines.base import SynthesisRequest, SynthesisResult, TTSEngine
from app.storage.voice_registry import VoiceSpec
from app.utils.audio import numpy_to_wav_bytes


class CoquiEngine(TTSEngine):
    name = "coqui"

    def __init__(self, allow_gpu: bool) -> None:
        try:
            import torch
            from TTS.api import TTS
        except Exception as exc:
            raise EngineNotAvailableError("Coqui TTS is not installed") from exc
        self._torch = torch
        self._tts_type = TTS
        self._allow_gpu = allow_gpu
        self._models: dict[str, object] = {}

    def _device(self) -> str:
        if self._allow_gpu and self._torch.cuda.is_available():
            return "cuda"
        return "cpu"

    def _get_model(self, voice_spec: VoiceSpec):
        coqui_cfg = voice_spec.coqui
        if coqui_cfg is None:
            raise ModelLoadError("Missing coqui config for voice")
        if coqui_cfg.model_name in self._models:
            return self._models[coqui_cfg.model_name]
        try:
            tts = self._tts_type(
                model_name=coqui_cfg.model_name, progress_bar=False
            )
            tts = tts.to(self._device())
        except Exception as exc:
            raise ModelLoadError("Failed to load Coqui model") from exc
        self._models[coqui_cfg.model_name] = tts
        return tts

    def synthesize(self, request: SynthesisRequest) -> SynthesisResult:
        tts = self._get_model(request.voice)
        coqui_cfg = request.voice.coqui
        if coqui_cfg is None:
            raise ModelLoadError("Missing coqui config for voice")
        try:
            kwargs = {"text": request.text}
            language = coqui_cfg.language or request.language
            if language:
                kwargs["language"] = language
            if coqui_cfg.speaker_id is not None:
                signature = inspect.signature(tts.tts)
                if "speaker_idx" in signature.parameters:
                    kwargs["speaker_idx"] = coqui_cfg.speaker_id
                elif "speaker" in signature.parameters:
                    kwargs["speaker"] = coqui_cfg.speaker_id
            wav = tts.tts(**kwargs)
        except Exception as exc:
            raise ModelLoadError("Coqui synthesis failed") from exc
        sample_rate = getattr(tts.synthesizer, "output_sample_rate", None)
        if sample_rate is None:
            sample_rate = getattr(tts.synthesizer, "sample_rate", 22050)
        wav_bytes = numpy_to_wav_bytes(wav, sample_rate)
        return SynthesisResult(
            audio_bytes=wav_bytes,
            sample_rate=sample_rate,
            sample_width=2,
            channels=1,
        )
