import io
import numpy as np
import soundfile as sf
import librosa
from fastapi import FastAPI, HTTPException
from fastapi.responses import StreamingResponse, JSONResponse
from pydantic import BaseModel
from typing import List
from kokoro import KPipeline
import base64
import re

app = FastAPI()

print("Đang tải Kokoro Pipeline...")
pipeline = KPipeline(lang_code='a')
print("Đã tải xong!")

# 1. Định nghĩa cấu trúc Chunk
class TTSChunk(BaseModel):
    text: str
    pitch: float = 0.0  # Đơn vị: Semitones (bước dịch tone)
    speed: float = 1.0

class TTSRequest(BaseModel):
    chunks: List[TTSChunk]
    voice: str = 'af_heart'

@app.post("/tts/chunks")
async def text_to_speech_chunks(req: TTSRequest):
    all_audio = []
    sr = 24000 # Sample rate mặc định của Kokoro
    
    try:
        for chunk in req.chunks:
            if not chunk.text.strip():
                continue
                
            # Sinh âm thanh bằng Kokoro cho đoạn text này
            generator = pipeline(chunk.text, voice=req.voice, speed=chunk.speed)
            chunk_audio_parts = [audio for _, _, audio in generator if audio is not None]
            
            if chunk_audio_parts:
                combined_chunk = np.concatenate(chunk_audio_parts)
                
                # 2. Xử lý bẻ tone (Pitch Shift) bằng librosa nếu pitch khác 0
                if chunk.pitch != 0.0:
                    combined_chunk = librosa.effects.pitch_shift(
                        y=combined_chunk, 
                        sr=sr, 
                        n_steps=chunk.pitch # Bước dịch (tương đương semitones)
                    )
                    
                all_audio.append(combined_chunk)
                
        if not all_audio:
            raise HTTPException(status_code=500, detail="Không thể tạo âm thanh.")
            
        # Nối tất cả các chunk lại với nhau
        final_audio = np.concatenate(all_audio)
        
        # Tạo file WAV trong bộ nhớ
        buffer = io.BytesIO()
        sf.write(buffer, final_audio, sr, format='wav')
        buffer.seek(0)
        
        return StreamingResponse(buffer, media_type="audio/wav")
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# Map intent sang pitch mặc định nếu không có chunk đặc biệt
INTENT_PITCH = {
    'up': 2.0,        # +2 semitone
    'down': -2.0,     # -2 semitone
    'focus': 1.0,     # +1 semitone
    'neutral': 0.0
}

# Các từ khóa và mức pitch tương ứng (giống BE)
HIGHLIGHT_KEYWORDS = [
    { 'word': r'cao hơn|vươn cao|higher', 'pitch': 2.0 },
    { 'word': r'thấp hơn|hạ thấp|lower', 'pitch': -2.0 },
    { 'word': r'nhanh|nhanh lên|tăng tốc|faster|fast|nhanh chóng', 'pitch': 1.666 },
    { 'word': r'chậm|chậm lại|giảm tốc|slower|slow|từ từ', 'pitch': -1.666 },
    { 'word': r'mạnh|mạnh hơn|mạnh mẽ|power|strong|stronger', 'pitch': 1.333 },
    { 'word': r'nhẹ|nhẹ nhàng|nhẹ hơn|soft|softer', 'pitch': -1.333 },
    { 'word': r'focus|tập trung', 'pitch': 1.0 },
    { 'word': r'relax|thả lỏng', 'pitch': -1.0 }
]

@app.post("/tts")
async def text_to_speech_simple(payload: dict):
    text = payload.get('text', '')
    intent = payload.get('intent', 'neutral')
    voice = payload.get('voice', 'af_heart')
    if not text:
        return JSONResponse(status_code=400, content={"detail": "Missing text"})

    # Tách chunk dựa trên highlight keywords
    segments = [{ 'text': text, 'pitch': None }]
    for kw in HIGHLIGHT_KEYWORDS:
        new_segments = []
        for seg in segments:
            if seg['pitch'] is not None:
                new_segments.append(seg)
            else:
                last = 0
                for m in re.finditer(kw['word'], seg['text'], re.IGNORECASE):
                    if m.start() > last:
                        new_segments.append({ 'text': seg['text'][last:m.start()], 'pitch': None })
                    new_segments.append({ 'text': m.group(0), 'pitch': kw['pitch'] })
                    last = m.end()
                if last < len(seg['text']):
                    new_segments.append({ 'text': seg['text'][last:], 'pitch': None })
        segments = new_segments
    # Lọc bỏ đoạn rỗng
    segments = [s for s in segments if s['text'].strip()]

    # Map pitch cho từng chunk
    chunks = []
    for seg in segments:
        pitch = seg['pitch']
        if pitch is None:
            pitch = INTENT_PITCH.get(intent, 0.0)
        chunks.append({ 'text': seg['text'], 'pitch': pitch, 'speed': 1.0 })

    # Gọi lại logic /tts/chunks nội bộ
    req = TTSRequest(chunks=chunks, voice=voice)
    # Tái sử dụng logic xử lý audio
    response = await text_to_speech_chunks(req)
    if isinstance(response, StreamingResponse):
        # Đọc dữ liệu WAV từ StreamingResponse
        audio_bytes = b''
        async for chunk in response.body_iterator:
            audio_bytes += chunk
        audio_b64 = base64.b64encode(audio_bytes).decode('utf-8')
        return { 'audioBase64': 'data:audio/wav;base64,' + audio_b64 }
    else:
        return response

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)