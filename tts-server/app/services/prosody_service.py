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
    for segment_text, is_emphasis in segments:
        profile = base_profile
        if is_emphasis:
            profile = ProsodyProfile(
                rate=base_profile.rate * 0.98,
                pitch=base_profile.pitch + 2.0,
                volume=base_profile.volume * 1.1,
            )
        results.append(
            TextSegment(
                text=segment_text, profile=profile, is_emphasis=is_emphasis
            )
        )
    return results


def _split_by_emphasis(
    text: str, emphasis_words: list[str]
) -> list[tuple[str, bool]]:
    cleaned = [word.strip() for word in emphasis_words if word.strip()]
    if not cleaned:
        return [(text, False)]
    escaped = [re.escape(word) for word in cleaned]
    pattern = re.compile(r"(" + "|".join(escaped) + r")", re.IGNORECASE)
    parts = pattern.split(text)
    emphasis_set = {word.lower() for word in cleaned}
    segments = []
    for part in parts:
        if not part:
            continue
        is_emphasis = part.strip().lower() in emphasis_set
        segments.append((part, is_emphasis))
    return _merge_punctuation_segments(segments)


def _merge_punctuation_segments(
    segments: list[tuple[str, bool]],
) -> list[tuple[str, bool]]:
    merged: list[tuple[str, bool]] = []
    for text, is_emphasis in segments:
        if not text.strip():
            if merged:
                prev_text, prev_emphasis = merged[-1]
                merged[-1] = (prev_text + text, prev_emphasis)
            continue
        if re.search(r"\w", text) is None:
            if merged:
                prev_text, prev_emphasis = merged[-1]
                merged[-1] = (prev_text + text, prev_emphasis)
            else:
                merged.append((text, is_emphasis))
            continue
        merged.append((text, is_emphasis))
    return merged
