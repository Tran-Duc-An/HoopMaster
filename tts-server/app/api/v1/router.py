from fastapi import APIRouter

from app.api.v1 import health, tts, voices

api_router = APIRouter()
api_router.include_router(health.router)
api_router.include_router(tts.router)
api_router.include_router(voices.router)
