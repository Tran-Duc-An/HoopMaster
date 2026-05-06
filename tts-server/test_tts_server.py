import httpx

TTS_URL = "http://localhost:8000/api/v1/tts"


def test_tts(text, intent, audio_format="wav"):
    payload = {
        "text": text,
        "intent": intent,
        "format": audio_format,
    }
    with httpx.stream("POST", TTS_URL, json=payload, timeout=60.0) as response:
        if response.status_code == 200:
            output_path = f"test_output.{audio_format}"
            with open(output_path, "wb") as file_handle:
                for chunk in response.iter_bytes():
                    if chunk:
                        file_handle.write(chunk)
            print(
                "Saved",
                output_path,
                "content-type=",
                response.headers.get("Content-Type"),
            )
        else:
            print("Error:", response.status_code, response.text)


if __name__ == "__main__":
    test_tts("Raise elbow higher and speed up!", "up")
    test_tts("Relax and slow down.", "down")
    test_tts("Focus on the movement.", "focus")
    test_tts("Keep it steady.", "neutral")
