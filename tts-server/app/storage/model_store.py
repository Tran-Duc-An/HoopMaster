from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from urllib.parse import urlparse

from app.core.device import detect_device
from app.core.errors import EngineNotAvailableError, ModelLoadError
from app.storage.voice_registry import VoiceSpec
from app.utils.files import download_file


@dataclass(frozen=True)
class PiperPaths:
    model_path: Path
    config_path: Path


class ModelStore:
    def __init__(self, base_dir: str, allow_gpu: bool) -> None:
        self.base_dir = Path(base_dir)
        self.base_dir.mkdir(parents=True, exist_ok=True)
        self.device_info = detect_device(allow_gpu)
        self._piper_cache: dict[str, object] = {}
        self._piper_type = None
        self._piper_config_type = None

    def _ensure_piper_imports(self) -> None:
        if self._piper_type is not None:
            return
        try:
            from piper import PiperVoice, SynthesisConfig
        except Exception as exc:
            raise EngineNotAvailableError("piper-tts is not installed") from exc
        self._piper_type = PiperVoice
        self._piper_config_type = SynthesisConfig

    def _piper_paths(self, voice_spec: VoiceSpec) -> PiperPaths:
        voice_cfg = voice_spec.piper
        if voice_cfg is None:
            raise ModelLoadError("Missing piper config for voice")
        voice_dir = self.base_dir / "piper" / voice_cfg.voice
        voice_dir.mkdir(parents=True, exist_ok=True)
        model_name = Path(urlparse(voice_cfg.model_url).path).name
        config_name = Path(urlparse(voice_cfg.config_url).path).name
        model_path = voice_dir / model_name
        config_path = voice_dir / config_name
        if not model_path.exists():
            download_file(voice_cfg.model_url, model_path)
        if not config_path.exists():
            download_file(voice_cfg.config_url, config_path)
        return PiperPaths(model_path=model_path, config_path=config_path)

    def get_piper_voice(self, voice_spec: VoiceSpec):
        self._ensure_piper_imports()
        if voice_spec.voice_id in self._piper_cache:
            return self._piper_cache[voice_spec.voice_id]
        paths = self._piper_paths(voice_spec)
        try:
            voice = self._piper_type.load(
                str(paths.model_path),
                use_cuda=self.device_info.onnx_gpu,
            )
        except Exception as exc:
            raise ModelLoadError("Failed to load piper voice") from exc
        self._piper_cache[voice_spec.voice_id] = voice
        return voice

    def create_piper_config(self, **kwargs):
        self._ensure_piper_imports()
        return self._piper_config_type(**kwargs)
