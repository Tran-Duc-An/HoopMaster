from pydantic import BaseModel


class VoiceInfo(BaseModel):
    id: str
    engine: str
    language: str
    locale: str | None = None
    gender: str | None = None
    style: str | None = None
