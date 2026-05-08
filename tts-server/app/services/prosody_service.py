from __future__ import annotations

from dataclasses import dataclass
import re


@dataclass(frozen=True)
class ProsodyProfile:
    rate: float
    pitch: float
    volume: float


@dataclass(frozen=True)
class TextSegment:
    text: str
    profile: ProsodyProfile
    is_emphasis: bool
    # Stores which emphasis keyword triggered this segment.
    # Used to apply keyword-specific prosody adjustments.
    emphasis_word: str | None = None


INTENT_PROFILES = {
    "neutral": ProsodyProfile(rate=1.0, pitch=0.0, volume=1.0),
    "strict": ProsodyProfile(rate=1.08, pitch=-1.0, volume=1.05),
    "cheerful": ProsodyProfile(rate=1.12, pitch=1.5, volume=1.15),
    "up": ProsodyProfile(rate=1.05, pitch=2.0, volume=1.1),
    "down": ProsodyProfile(rate=0.95, pitch=-2.0, volume=0.9),
    "focus": ProsodyProfile(rate=0.98, pitch=1.0, volume=1.05),
}


def build_segments(
    text: str,
    intent: str | None,
    emphasis_words: list[str],
    rate: float | None,
    pitch: float | None,
    volume: float | None,
) -> list[TextSegment]:
    base_profile = INTENT_PROFILES.get(
        intent or "neutral", INTENT_PROFILES["neutral"]
    )
    base_profile = ProsodyProfile(
        rate=rate if rate is not None else base_profile.rate,
        pitch=pitch if pitch is not None else base_profile.pitch,
        volume=volume if volume is not None else base_profile.volume,
    )
    segments = _split_by_emphasis(text, emphasis_words)
    results = []
    for segment_text, is_emphasis, emphasis_word in segments:
        profile = base_profile
        if is_emphasis:
            profile = _apply_emphasis(base_profile, emphasis_word)
        results.append(
            TextSegment(
                text=segment_text,
                profile=profile,
                is_emphasis=is_emphasis,
                emphasis_word=emphasis_word if is_emphasis else None,
            )
        )
    return results


def _split_by_emphasis(
    text: str, emphasis_words: list[str]
) -> list[tuple[str, bool, str | None]]:
    cleaned = [word.strip() for word in emphasis_words if word.strip()]
    if not cleaned:
        return [(text, False, None)]
    escaped = [re.escape(word) for word in cleaned]
    pattern = re.compile(r"(" + "|".join(escaped) + r")", re.IGNORECASE)
    parts = pattern.split(text)
    emphasis_set = {word.lower() for word in cleaned}
    segments = []
    for part in parts:
        if not part:
            continue
        is_emphasis = part.strip().lower() in emphasis_set
        segments.append((part, is_emphasis, part.strip().lower() if is_emphasis else None))
    return _merge_punctuation_segments(segments)


def _merge_punctuation_segments(
    segments: list[tuple[str, bool, str | None]],
) -> list[tuple[str, bool, str | None]]:
    merged: list[tuple[str, bool, str | None]] = []
    for text, is_emphasis, emphasis_word in segments:
        if not text.strip():
            if merged:
                prev_text, prev_is_emphasis, prev_emphasis_word = merged[-1]
                merged[-1] = (
                    prev_text + text,
                    prev_is_emphasis,
                    prev_emphasis_word,
                )
            continue
        if re.search(r"\w", text) is None:
            if merged:
                prev_text, prev_is_emphasis, prev_emphasis_word = merged[-1]
                merged[-1] = (
                    prev_text + text,
                    prev_is_emphasis,
                    prev_emphasis_word,
                )
            else:
                merged.append((text, is_emphasis, emphasis_word))
            continue
        merged.append((text, is_emphasis, emphasis_word))
    return merged


def _apply_emphasis(base_profile: ProsodyProfile, emphasis_word: str | None) -> ProsodyProfile:
    """
    Apply subtle keyword-specific prosody adjustments to avoid sounding robotic.

    Notes:
    - `pitch` is interpreted as semitone shift in `app/utils/audio.py`.
    - `rate` is a multiplier applied via speed change (except where the engine
      does its own handling; we still keep it for consistency).
    - `volume` is a multiplier applied as gain in dB.
    """
    if not emphasis_word:
        return base_profile

    word = emphasis_word.lower().strip()

    # Keep changes within a safe band so TTS doesn't overshoot.
    def clamp(val: float, lo: float, hi: float) -> float:
        return max(lo, min(hi, val))

    pitch = base_profile.pitch
    rate = base_profile.rate
    volume = base_profile.volume

    # Directional/pitch emphasis
    if word in {"higher", "up"}:
        pitch += 2.4
    elif word in {"lower", "down"}:
        pitch -= 2.4
    # Strength/loudness emphasis (stronger/loud concepts)
    elif word in {"strong", "stronger", "power"}:
        volume *= 1.18
        pitch += 0.6  # keep pitch change small; focus is on intensity/loudness
    # Tempo emphasis
    elif word in {"fast", "faster"}:
        rate *= 1.07
        pitch += 0.4
    elif word in {"slow", "slower"}:
        rate *= 0.93
        pitch -= 0.4
    # Soft / relax emphasis
    elif word in {"soft", "softer", "relax"}:
        volume *= 0.88
        pitch -= 0.7
        rate *= 0.98
    # Focus emphasis (subtle to sound natural)
    elif word in {"focus"}:
        volume *= 1.05
        pitch += 0.8
        rate *= 1.00
    else:
        # Fallback: keep a small generic emphasis.
        volume *= 1.08
        pitch += 1.2
        rate *= 0.99

    # Slightly slow down during emphasis to “land” the word (works well for most keywords).
    rate *= 0.98

    pitch = clamp(pitch, -4.0, 4.0)
    rate = clamp(rate, 0.85, 1.25)
    volume = clamp(volume, 0.7, 1.35)

    return ProsodyProfile(rate=rate, pitch=pitch, volume=volume)
