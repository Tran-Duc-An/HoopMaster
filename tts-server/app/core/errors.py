class TTSException(Exception):
    pass


class VoiceNotFoundError(TTSException):
    pass


class EngineNotAvailableError(TTSException):
    pass


class AudioFormatError(TTSException):
    pass


class InvalidRequestError(TTSException):
    pass


class ModelLoadError(TTSException):
    pass
