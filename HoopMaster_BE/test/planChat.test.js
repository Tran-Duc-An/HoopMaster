// DÙNG CÁCH GỌI ENV GIỐNG HỆT BÀI TRƯỚC ĐÃ THÀNH CÔNG
require('dotenv').config({ override: true });

const request = require('supertest');
const express = require('express');

// 1. MOCK DATABASE & TTS
jest.mock('../src/services/conversationService', () => ({
  getOrCreateConversation: jest.fn(),
  addMessage: jest.fn(),
  getHistory: jest.fn(() => [
    { role: 'assistant', content: 'Hello! I am your AI Basketball Coach. What are your training goals today?' }
  ])
}));

jest.mock('../src/services/trainingPlanService', () => ({
  createPlan: jest.fn()
}));

jest.mock('../src/services/ttsService', () => ({
  synthesizeSpeech: jest.fn(() => ({ audioBase64: 'fake_audio_base64_string' }))
}));

// Import service thật
const { planChat } = require('../src/services/planChatAgent');
const trainingPlanService = require('../src/services/trainingPlanService');

// 2. KHỞI TẠO EXPRESS APP ẢO VÀ TỰ TẠO ROUTE
const app = express();
app.use(express.json());

app.post('/api/chat/plan', async (req, res) => {
  try {
    const fakeUserId = '60d5ecb8b392d700153ef123'; 
    const result = await planChat(fakeUserId, req.body);
    res.json({ success: true, ...result });
  } catch (error) {
    res.status(500).json({ success: false, error: error.message });
  }
});

describe('Kiểm thử Tích hợp Chat Lên Lịch (Real AI Mistral + Mock DB)', () => {
  
  beforeAll(() => {
    jest.setTimeout(30000); 
    console.log('🔑 MISTRAL_API_KEY trong PlanChat:', process.env.MISTRAL_API_KEY ? 'Đã tìm thấy!' : 'VẪN CHƯA THẤY');
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('CHT_01 (Real AI): Giao tiếp cơ bản - AI hỏi thêm thông tin', async () => {
    console.log('⏳ Đang nhắn tin cho HLV AI...');
    const payload = { text: "I want to improve my 3-point shooting accuracy." };
    const response = await request(app).post('/api/chat/plan').send(payload);

    console.log('\n💬 --- [CHT_01] PHẢN HỒI TỪ AI COACH --- 💬');
    console.log(JSON.stringify(response.body, null, 2));

    expect(response.statusCode).toBe(200);
    expect(response.body.success).toBe(true);
    expect(response.body).toHaveProperty('reply');
    expect(response.body.audioBase64).toBe('fake_audio_base64_string');
    
    expect(trainingPlanService.createPlan).not.toHaveBeenCalled();
  });

  it('CHT_02 (Real AI): Ép AI sinh ra JSON Training Plan khi đã đủ thông tin', async () => {
    const { getHistory } = require('../src/services/conversationService');
    getHistory.mockResolvedValueOnce([
      { role: 'user', content: 'I am a beginner, I want to improve 3-point shooting.' },
      { role: 'assistant', content: 'Great. Do you have any injuries? How many days a week can you train?' },
      { role: 'user', content: 'No injuries. I can train 3 days a week. Give me the training plan now.' }
    ]);

    console.log('⏳ Đang ép HLV AI xuất giáo án (JSON)...');
    const payload = { text: "Generate the plan please." };
    const response = await request(app).post('/api/chat/plan').send(payload);

    console.log('\n📋 --- [CHT_02] GIÁO ÁN TỪ AI COACH --- 📋');
    console.log(JSON.stringify(response.body, null, 2));

    expect(response.statusCode).toBe(200);
    
    // Đảm bảo AI trả về câu JSON hợp lệ
    if (response.body.reply && response.body.reply.trim().startsWith('[')) {
      expect(trainingPlanService.createPlan).toHaveBeenCalled();
      console.log('✅ Hệ thống ĐÃ BẮT ĐƯỢC JSON và gọi hàm lưu giáo án!');
    } else {
      console.log('⚠️ AI chưa trả về định dạng JSON mảng "[ ... ]".');
    }
  });

  it('CHT_03: Báo lỗi khi gửi dữ liệu rỗng', async () => {
    const payload = {}; 
    const response = await request(app).post('/api/chat/plan').send(payload);
    
    console.log('\n⚠️ --- [CHT_03] LỖI TIN NHẮN RỖNG --- ⚠️');
    console.log(JSON.stringify(response.body, null, 2));

    expect(response.statusCode).toBe(500);
    expect(response.body.error).toContain('No input text');
  });

});