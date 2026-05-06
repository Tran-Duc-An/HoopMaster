from pydantic import BaseModel, Field, field_validator

SUPPORTED_FORMATS = {"wav", "mp3", "ogg"}
SUPPORTED_ENGINES = {"auto", "piper", "coqui"}


class TTSRequest(BaseModel):
    text: str = Field(min_length=1, max_length=2000)
    voice: str | None = None
    language: str | None = None
    engine: str | None = None
    format: str | None = None
    intent: str | None = "neutral"
    emphasis_words: list[str] = Field(default_factory=list)
    rate: float | None = Field(default=None, ge=0.5, le=1.5)
    pitch: float | None = Field(default=None, ge=-6.0, le=6.0)
    volume: float | None = Field(default=None, ge=0.5, le=1.5)

    @field_validator("format")
    @classmethod
    def normalize_format(cls, value: str | None) -> str | None:
        if value is None:
            return value
        normalized = value.lower()
        if normalized not in SUPPORTED_FORMATS:
            raise ValueError("Unsupported format")
        return normalized

    @field_validator("engine")
    @classmethod
    def normalize_engine(cls, value: str | None) -> str | None:
        if value is None:
            return value
        normalized = value.lower()
        if normalized not in SUPPORTED_ENGINES:
            raise ValueError("Unsupported engine")
        return normalized
