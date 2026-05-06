from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.core.config import get_settings
from app.storage.audio_cache import AudioCache
from app.storage.model_store import ModelStore
from app.storage.voice_registry import VoiceRegistry
from app.services.tts_service import TTSService


def build_lifespan():
    @asynccontextmanager
    async def lifespan(app: FastAPI):
        settings = get_settings()
        voice_registry = VoiceRegistry.from_file(settings.voices_path)
        model_store = ModelStore(
            base_dir=settings.model_dir, allow_gpu=settings.allow_gpu
        )
        audio_cache = AudioCache(
            base_dir=settings.cache_dir,
            ttl_seconds=settings.audio_cache_ttl_seconds,
            max_entries=settings.audio_cache_max_entries,
        )
        tts_service = TTSService(
            settings=settings,
            voice_registry=voice_registry,
            model_store=model_store,
            audio_cache=audio_cache,
        )
        if settings.preload_models:
            tts_service.preload_all()
        app.state.settings = settings
        app.state.voice_registry = voice_registry
        app.state.model_store = model_store
        app.state.audio_cache = audio_cache
        app.state.tts_service = tts_service
        yield
        audio_cache.close()

    return lifespan
