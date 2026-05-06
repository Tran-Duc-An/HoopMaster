from __future__ import annotations

from typing import Any

from diskcache import Cache


class AudioCache:
    def __init__(
        self, base_dir: str, ttl_seconds: int, max_entries: int
    ) -> None:
        self._cache = Cache(base_dir)
        self._ttl = ttl_seconds
        self._max_entries = max_entries

    def get(self, key: str) -> Any | None:
        return self._cache.get(key)

    def set(self, key: str, value: Any) -> None:
        self._cache.set(key, value, expire=self._ttl)
        if self._max_entries:
            self._cache.cull()

    def close(self) -> None:
        self._cache.close()
