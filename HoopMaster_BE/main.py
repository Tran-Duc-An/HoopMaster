import requests

API_KEY = "AIzaSyARFmOgWFrTyP0ekhBICHl5yYptTjlIeY4"
ENDPOINT = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key={API_KEY}"

headers = {
    "Content-Type": "application/json"
}

data = {
    "contents": [
        {
            "role": "user",
            "parts": [
                {"text": "Nói một câu chào bằng tiếng Việt"}
            ]
        }
    ]
}

response = requests.post(ENDPOINT, headers=headers, json=data)
print(response.status_code)
print(response.json())