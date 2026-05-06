from pathlib import Path

import httpx


def download_file(url: str, dest: Path) -> None:
    dest.parent.mkdir(parents=True, exist_ok=True)
    with httpx.stream(
        "GET", url, timeout=60.0, follow_redirects=True
    ) as response:
        response.raise_for_status()
        with dest.open("wb") as file_handle:
            for chunk in response.iter_bytes():
                file_handle.write(chunk)
