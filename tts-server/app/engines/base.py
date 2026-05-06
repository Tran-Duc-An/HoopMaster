from __future__ import annotations

from dataclasses import dataclass

from app.storage.voice_registry import VoiceSpec


@dataclass(frozen=True)
class SynthesisRequest:
    text: str
    voice: VoiceSpec
    language: str | None
    rate: float
    pitch: float
    volume: float


@dataclass(frozen=True)
class SynthesisResult:
    audio_bytes: bytes
    sample_rate: int
    sample_width: int
    channels: int


class TTSEngine:
    name = ""
    supports_rate = False

    def synthesize(self, request: SynthesisRequest) -> SynthesisResult:
        raise NotImplementedError
