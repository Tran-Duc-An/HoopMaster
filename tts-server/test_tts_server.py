import requests
import json

def test_local_tts(
    text: str,
    intent: str = "neutral",
    emphasis_words: list = None,
    output_filename: str = "test_output.wav"
):
    """
    Hàm giả lập request từ Node.js gửi đến Local TTS Server.
    """
    if emphasis_words is None:
        emphasis_words = []

    # Endpoint mặc định của server theo file Node.js của bạn
    url = "http://localhost:8000/api/v1/tts"

    # Payload giống hệt cấu trúc gửi từ generateLocalTTS (Node.js)
    payload = {
        "text": text,
        "intent": intent,
        "emphasis_words": emphasis_words,
        "format": "wav",
        # Các trường dưới đây có thể bỏ trống để server dùng default, 
        # nhưng mình liệt kê ra để bạn dễ hình dung.
        "voice": None,
        "engine": None, 
        "language": None 
    }

    headers = {
        "Content-Type": "application/json"
    }

    print(f"🚀 Đang gửi request tới {url}...")
    print(f"📦 Payload: {json.dumps(payload, indent=2, ensure_ascii=False)}")

    try:
        response = requests.post(url, json=payload, headers=headers, timeout=60)
        
        # Kiểm tra xem request có thành công không
        response.raise_for_status()

        # Nếu thành công, server Python của bạn trả về file audio (bytes)
        # Tiến hành lưu ra file để bạn có thể nghe thử
        with open(output_filename, "wb") as f:
            f.write(response.content)
            
        print(f"✅ Thành công! Đã lưu file âm thanh tại: {output_filename}")
        
    except requests.exceptions.RequestException as e:
        print(f"❌ Lỗi khi kết nối tới TTS Server: {e}")
        if hasattr(e, 'response') and e.response is not None:
            print(f"Chi tiết lỗi từ server: {e.response.text}")

# ==========================================
# CHẠY TEST THỰC TẾ
# ==========================================
if __name__ == "__main__":
    # Test case 1: Cảm xúc nghiêm ngặt, có nhấn mạnh từ "higher" và "stronger"
    test_text = "Keep your elbow higher, and throw the ball stronger!"
    
    test_local_tts(
        text=test_text,
        intent="strict",
        emphasis_words=["higher", "stronger"],
        output_filename="test_strict_emphasis.wav"
    )

    # Test case 2: Cảm xúc bình thường, không nhấn mạnh (để nghe so sánh)
    test_local_tts(
        text=test_text,
        intent="neutral",
        emphasis_words=[],
        output_filename="test_neutral_normal.wav"
    )