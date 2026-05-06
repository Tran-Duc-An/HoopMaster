from fastapi import FastAPI

from app.api.legacy import legacy_router
from app.api.v1.router import api_router
from app.core.config import get_settings
from app.core.lifespan import build_lifespan
from app.core.logging import configure_logging


def create_app() -> FastAPI:
    configure_logging()
    settings = get_settings()
    app = FastAPI(
        title=settings.app_name,
        version="0.1.0",
        lifespan=build_lifespan(),
    )
    app.include_router(api_router, prefix=settings.api_v1_prefix)
    app.include_router(legacy_router)
    return app


app = create_app()
