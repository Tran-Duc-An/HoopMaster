from __future__ import annotations

import math
import os
from pathlib import Path
from io import BytesIO
from typing import Iterator

import numpy as np
import soundfile as sf
from pydub import AudioSegment


_ffmpeg_binary = os.getenv("FFMPEG_BINARY")
if _ffmpeg_binary and Path(_ffmpeg_binary).exists():
    AudioSegment.converter = _ffmpeg_binary


def numpy_to_wav_bytes(samples: np.ndarray, sample_rate: int) -> bytes:
    if samples.dtype != np.float32:
        samples = samples.astype(np.float32)
    buffer = BytesIO()
    sf.write(buffer, samples, sample_rate, format="WAV", subtype="PCM_16")
    return buffer.getvalue()


def wav_bytes_to_segment(wav_bytes: bytes) -> AudioSegment:
    return AudioSegment.from_file(BytesIO(wav_bytes), format="wav")


def export_segment(segment: AudioSegment, fmt: str) -> bytes:
    buffer = BytesIO()
    segment.export(buffer, format=fmt)
    return buffer.getvalue()


def apply_prosody(
    segment: AudioSegment,
    rate: float,
    pitch_semitones: float,
    volume: float,
) -> AudioSegment:
    adjusted = segment
    if volume != 1.0:
        gain_db = 20 * math.log10(max(volume, 0.01))
        adjusted = adjusted + gain_db
    if pitch_semitones != 0.0:
        adjusted = _pitch_shift(adjusted, pitch_semitones)
    if rate != 1.0:
        adjusted = _change_speed(adjusted, rate)
    return adjusted


def _pitch_shift(segment: AudioSegment, semitones: float) -> AudioSegment:
    if semitones == 0.0:
        return segment
    original_rate = segment.frame_rate
    new_rate = int(original_rate * (2.0 ** (semitones / 12.0)))
    shifted = segment._spawn(
        segment.raw_data, overrides={"frame_rate": new_rate}
    )
    return shifted.set_frame_rate(original_rate)


def _change_speed(segment: AudioSegment, rate: float) -> AudioSegment:
    if rate == 1.0:
        return segment
    original_rate = segment.frame_rate
    new_rate = int(original_rate * rate)
    shifted = segment._spawn(
        segment.raw_data, overrides={"frame_rate": new_rate}
    )
    return shifted.set_frame_rate(original_rate)


def iter_bytes(data: bytes, chunk_size: int) -> Iterator[bytes]:
    for offset in range(0, len(data), chunk_size):
        yield data[offset : offset + chunk_size]
