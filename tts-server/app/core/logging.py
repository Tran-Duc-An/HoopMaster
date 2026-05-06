import logging
import os


def configure_logging(level: str | None = None) -> None:
    log_level = level or os.getenv("TTS_LOG_LEVEL", "INFO")
    logging.basicConfig(
        level=log_level,
        format="%(asctime)s %(levelname)s %(name)s - %(message)s",
    )
