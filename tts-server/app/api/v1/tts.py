from typing import Annotated

from fastapi import APIRouter, HTTPException, Query
from fastapi.responses import StreamingResponse

from app.api.deps import SettingsDep, TTSServiceDep
from app.core.errors import (
    AudioFormatError,
    EngineNotAvailableError,
    InvalidRequestError,
    TTSException,
    VoiceNotFoundError,
)
from app.schemas.tts import TTSRequest
from app.utils.audio import iter_bytes

router = APIRouter(prefix="/tts", tags=["tts"])


@router.post("")
def synthesize_tts(
    payload: TTSRequest,
    tts_service: TTSServiceDep,
    settings: SettingsDep,
    audio_format: Annotated[str | None, Query()] = None,
) -> StreamingResponse:
    if audio_format:
        payload = payload.model_copy(update={"format": audio_format.lower()})
    try:
        result = tts_service.synthesize(payload)
    except VoiceNotFoundError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    except AudioFormatError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except InvalidRequestError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except EngineNotAvailableError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    except TTSException as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc
    headers = result.headers()
    return StreamingResponse(
        iter_bytes(result.audio_bytes, settings.chunk_size),
        media_type=result.media_type,
        headers=headers,
    )
