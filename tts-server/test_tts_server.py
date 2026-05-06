import requests

# Địa chỉ server TTS local
TTS_URL = 'http://localhost:8000/tts'

def test_tts(text, intent):
    payload = {
        'text': text,
        'intent': intent
    }
    response = requests.post(TTS_URL, json=payload)
    if response.status_code == 200:
        data = response.json()
        print('Kết quả:', data)
        # Nếu có audioBase64, lưu file thử
        if 'audioBase64' in data:
            base64_data = data['audioBase64'].split(',')[-1]
            with open('test_output.wav', 'wb') as f:
                import base64
                f.write(base64.b64decode(base64_data))
            print('Đã lưu file test_output.wav')
    else:
        print('Lỗi:', response.status_code, response.text)

if __name__ == '__main__':
    # Test các intent khác nhau
    test_tts('Hãy nhấn mạnh cao hơn và nhanh lên!', 'up')
    test_tts('Hãy thả lỏng và chậm lại.', 'down')
    test_tts('Tập trung vào động tác.', 'focus')
    test_tts('Bình thường thôi.', 'neutral')
