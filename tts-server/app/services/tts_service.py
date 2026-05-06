from __future__ import annotations

from dataclasses import dataclass

from pydub import AudioSegment

from app.core.config import Settings
from app.core.errors import (
    AudioFormatError,
    EngineNotAvailableError,
    InvalidRequestError,
)
from app.engines.base import SynthesisRequest, TTSEngine
from app.engines.coqui_engine import CoquiEngine
from app.engines.piper_engine import PiperEngine
from app.schemas.tts import SUPPORTED_FORMATS, TTSRequest
from app.schemas.voices import VoiceInfo
from app.services.prosody_service import build_segments
from app.services.text_normalize import normalize_text
from app.storage.audio_cache import AudioCache
from app.storage.model_store import ModelStore
from app.storage.voice_registry import VoiceRegistry
from app.utils.audio import apply_prosody, export_segment, wav_bytes_to_segment
from app.utils.hashing import compute_hash

MEDIA_TYPES = {
    "wav": "audio/wav",
    "mp3": "audio/mpeg",
    "ogg": "audio/ogg",
}


@dataclass(frozen=True)
class TTSResult:
    audio_bytes: bytes
    media_type: str
    sample_rate: int
    voice_id: str
    audio_format: str
    engine: str

    def headers(self) -> dict[str, str]:
        return {
            "X-Sample-Rate": str(self.sample_rate),
            "X-Voice": self.voice_id,
            "X-Audio-Format": self.audio_format,
            "X-Engine": self.engine,
        }


class TTSService:
    def __init__(
        self,
        settings: Settings,
        voice_registry: VoiceRegistry,
        model_store: ModelStore,
        audio_cache: AudioCache,
    ) -> None:
        self._settings = settings
        self._voice_registry = voice_registry
        self._model_store = model_store
        self._audio_cache = audio_cache
        self._piper_engine = PiperEngine(model_store)
        self._coqui_engine = self._maybe_coqui_engine()

    def _maybe_coqui_engine(self) -> CoquiEngine | None:
        try:
            return CoquiEngine(self._settings.allow_gpu)
        except EngineNotAvailableError:
            return None

    def preload_all(self) -> None:
        for voice in self._voice_registry.list():
            if voice.engine == "piper":
                self._model_store.get_piper_voice(voice)
            if voice.engine == "coqui" and self._coqui_engine:
                self._coqui_engine._get_model(voice)

    def voice_count(self) -> int:
        return len(self._voice_registry.list())

    def list_voices(self) -> list[VoiceInfo]:
        return [
            VoiceInfo(
                id=voice.voice_id,
                engine=voice.engine,
                language=voice.language,
                locale=voice.locale,
                gender=voice.gender,
                style=voice.style,
            )
            for voice in self._voice_registry.list()
        ]

    def synthesize(self, payload: TTSRequest) -> TTSResult:
        text = normalize_text(payload.text)
        if not text:
            raise InvalidRequestError("Text is empty")
        if len(text) > self._settings.max_text_chars:
            raise InvalidRequestError("Text is too long")
        voice = self._resolve_voice(payload)
        engine = self._select_engine(payload, voice)
        audio_format = payload.format or self._settings.default_format
        if audio_format not in SUPPORTED_FORMATS:
            raise AudioFormatError("Unsupported format")
        language = (
            payload.language
            or voice.language
            or self._settings.default_language
        )
        cache_key = compute_hash(
            {
                "text": text,
                "voice": voice.voice_id,
                "engine": engine.name,
                "language": language,
                "intent": payload.intent,
                "emphasis_words": payload.emphasis_words,
                "rate": payload.rate,
                "pitch": payload.pitch,
                "volume": payload.volume,
                "format": audio_format,
            }
        )
        cached = self._audio_cache.get(cache_key)
        if cached:
            return TTSResult(
                audio_bytes=cached["audio"],
                media_type=cached["media_type"],
                sample_rate=cached["sample_rate"],
                voice_id=voice.voice_id,
                audio_format=audio_format,
                engine=engine.name,
            )
        segments = build_segments(
            text=text,
            intent=payload.intent,
            emphasis_words=payload.emphasis_words,
            rate=payload.rate,
            pitch=payload.pitch,
            volume=payload.volume,
        )
        combined: AudioSegment | None = None
        for index, segment in enumerate(segments):
            segment_request = SynthesisRequest(
                text=segment.text,
                voice=voice,
                language=language,
                rate=segment.profile.rate,
                pitch=segment.profile.pitch,
                volume=segment.profile.volume,
            )
            result = engine.synthesize(segment_request)
            audio = wav_bytes_to_segment(result.audio_bytes)
            rate_value = 1.0 if engine.supports_rate else segment.profile.rate
            audio = apply_prosody(
                audio,
                rate=rate_value,
                pitch_semitones=segment.profile.pitch,
                volume=segment.profile.volume,
            )
            if combined is None:
                combined = audio
                continue
            if segment.is_emphasis or segments[index - 1].is_emphasis:
                combined += AudioSegment.silent(
                    duration=40, frame_rate=combined.frame_rate
                )
            combined += audio
        if combined is None:
            raise InvalidRequestError("No audio produced")
        try:
            audio_bytes = export_segment(combined, audio_format)
        except Exception as exc:
            raise AudioFormatError(
                "Failed to encode audio. Ensure ffmpeg is installed for mp3/ogg."
            ) from exc
        media_type = MEDIA_TYPES[audio_format]
        self._audio_cache.set(
            cache_key,
            {
                "audio": audio_bytes,
                "media_type": media_type,
                "sample_rate": combined.frame_rate,
            },
        )
        return TTSResult(
            audio_bytes=audio_bytes,
            media_type=media_type,
            sample_rate=combined.frame_rate,
            voice_id=voice.voice_id,
            audio_format=audio_format,
            engine=engine.name,
        )

    def _resolve_voice(self, payload: TTSRequest):
        if payload.voice:
            return self._voice_registry.get(payload.voice)
        language = payload.language or self._settings.default_language
        language_key = language.split("-")[0].split("_")[0]
        return self._voice_registry.resolve_default(language_key)

    def _select_engine(self, payload: TTSRequest, voice) -> TTSEngine:
        engine_name = (payload.engine or self._settings.default_engine).lower()
        if engine_name == "auto":
            engine_name = voice.engine
        if engine_name == "piper":
            if voice.engine != "piper":
                raise InvalidRequestError(
                    "Voice does not support the piper engine"
                )
            return self._piper_engine
        if engine_name == "coqui":
            if self._coqui_engine is None:
                raise EngineNotAvailableError("Coqui engine is not available")
            if voice.engine != "coqui":
                raise InvalidRequestError(
                    "Voice does not support the coqui engine"
                )
            return self._coqui_engine
        raise InvalidRequestError("Unknown engine")
