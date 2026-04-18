import requests

API_KEY = "sk_da4339c0918a550194b24d175d75cec3a6b530b5d32a307b"
url = "https://api.elevenlabs.io/v1/text-to-speech/EXAVITQu4vr4xnSDxMaL/stream"
headers = {
    "xi-api-key": API_KEY,
    "Content-Type": "application/json"
}
data = {
    "text": "Xin chào, đây là test ElevenLabs API.",
    "voice_settings": {
        "stability": 0.5,
        "similarity_boost": 0.5
    }
}

response = requests.post(url, headers=headers, json=data)
print("Status:", response.status_code)
if response.status_code == 200:
    print("TTS thành công, nhận được audio.")
    with open("output.wav", "wb") as f:
        f.write(response.content)
else:
    print("Lỗi:", response.text)
