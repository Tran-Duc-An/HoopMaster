from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "HoopMaster TTS"
    api_v1_prefix: str = "/api/v1"
    default_language: str = "en"
    default_voice: str = "en_coach_male"
    default_format: str = "wav"
    default_engine: str = "auto"
    allow_gpu: bool = True
    preload_models: bool = True
    model_dir: str = "models"
    cache_dir: str = "cache"
    voices_path: str = "config/voices.yaml"
    max_text_chars: int = 500
    chunk_size: int = 65536
    audio_cache_ttl_seconds: int = 86400
    audio_cache_max_entries: int = 200

    model_config = SettingsConfigDict(
        env_file=".env",
        env_prefix="TTS_",
        case_sensitive=False,
    )


@lru_cache
def get_settings() -> Settings:
    return Settings()
