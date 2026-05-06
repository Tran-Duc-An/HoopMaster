from typing import Annotated

from fastapi import Depends, Request

from app.core.config import Settings
from app.services.tts_service import TTSService


def get_settings(request: Request) -> Settings:
    return request.app.state.settings


def get_tts_service(request: Request) -> TTSService:
    return request.app.state.tts_service


SettingsDep = Annotated[Settings, Depends(get_settings)]
TTSServiceDep = Annotated[TTSService, Depends(get_tts_service)]
