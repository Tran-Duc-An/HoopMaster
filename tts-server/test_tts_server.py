import httpx

TTS_URL = "http://localhost:8000/api/v1/tts"


def test_tts(text, intent, audio_format="wav", emphasis_words=None):
    payload = {
        "text": text,
        "intent": intent,
        "format": audio_format,
        "emphasis_words": emphasis_words or [],
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
    test_tts("Lower your stance and focus.", "strict", emphasis_words=["lower", "focus"])
    test_tts(
        "Great effort! Raise your elbow higher and keep going.",
        "cheerful",
        emphasis_words=["higher"],
    )
    test_tts("Keep it steady.", "neutral")
