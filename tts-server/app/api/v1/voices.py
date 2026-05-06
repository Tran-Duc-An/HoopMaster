from fastapi import APIRouter

from app.api.deps import TTSServiceDep
from app.schemas.voices import VoiceInfo

router = APIRouter(prefix="/voices", tags=["voices"])


@router.get("", response_model=list[VoiceInfo])
def list_voices(tts_service: TTSServiceDep) -> list[VoiceInfo]:
    return tts_service.list_voices()
