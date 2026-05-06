from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Any

import yaml

from app.core.errors import VoiceNotFoundError


@dataclass(frozen=True)
class PiperConfig:
    voice: str
    model_url: str
    config_url: str


@dataclass(frozen=True)
class CoquiConfig:
    model_name: str
    speaker_id: int | None = None
    language: str | None = None


@dataclass(frozen=True)
class VoiceSpec:
    voice_id: str
    engine: str
    language: str
    locale: str | None
    gender: str | None
    style: str | None
    piper: PiperConfig | None = None
    coqui: CoquiConfig | None = None


class VoiceRegistry:
    def __init__(
        self, voices: dict[str, VoiceSpec], defaults: dict[str, Any]
    ) -> None:
        self._voices = voices
        self._defaults = defaults

    @classmethod
    def from_file(cls, path: str | Path) -> "VoiceRegistry":
        path_obj = Path(path)
        data = yaml.safe_load(path_obj.read_text(encoding="utf-8"))
        defaults = data.get("defaults", {})
        voices = {}
        for voice_id, raw in data.get("voices", {}).items():
            engine = raw.get("engine")
            piper = None
            coqui = None
            if engine == "piper":
                piper = PiperConfig(
                    voice=raw["piper"]["voice"],
                    model_url=raw["piper"]["model_url"],
                    config_url=raw["piper"]["config_url"],
                )
            if engine == "coqui":
                coqui = CoquiConfig(
                    model_name=raw["coqui"]["model_name"],
                    speaker_id=raw["coqui"].get("speaker_id"),
                    language=raw["coqui"].get("language"),
                )
            voices[voice_id] = VoiceSpec(
                voice_id=voice_id,
                engine=engine,
                language=raw.get("language", "en"),
                locale=raw.get("locale"),
                gender=raw.get("gender"),
                style=raw.get("style"),
                piper=piper,
                coqui=coqui,
            )
        return cls(voices=voices, defaults=defaults)

    def get(self, voice_id: str) -> VoiceSpec:
        if voice_id not in self._voices:
            raise VoiceNotFoundError(f"Unknown voice: {voice_id}")
        return self._voices[voice_id]

    def resolve_default(self, language: str | None) -> VoiceSpec:
        voice_id = self._defaults.get("voice")
        by_lang = self._defaults.get("voice_by_language", {})
        if language:
            lang_key = language.lower()
            if lang_key in by_lang:
                voice_id = by_lang[lang_key]
        if not voice_id:
            raise VoiceNotFoundError("No default voice configured")
        return self.get(voice_id)

    def list(self) -> list[VoiceSpec]:
        return list(self._voices.values())
