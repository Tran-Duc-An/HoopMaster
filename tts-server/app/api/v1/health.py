from fastapi import APIRouter

from app.api.deps import TTSServiceDep
from app.schemas.health import HealthResponse

router = APIRouter(tags=["health"])


@router.get("/healthz", response_model=HealthResponse)
def healthz(tts_service: TTSServiceDep) -> HealthResponse:
    return HealthResponse(status="ok", model_count=tts_service.voice_count())


@router.get("/readyz", response_model=HealthResponse)
def readyz(tts_service: TTSServiceDep) -> HealthResponse:
    return HealthResponse(status="ready", model_count=tts_service.voice_count())
