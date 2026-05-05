require('dotenv').config({ override: true });
const request = require('supertest');
const express = require('express');
const feedbackRoutes = require('../src/routes/feedbackRoutes');

const app = express();
app.use(express.json());
app.use('/api/feedback', feedbackRoutes);

describe('Kiểm thử Tích hợp Phản hồi AI (100% REAL, NO MOCK)', () => {
  // Lưu lại API key gốc hiện tại (nếu có) để không làm hỏng các file khác
  const originalApiKey = process.env.MISTRAL_API_KEY;

  beforeAll(() => {
    // Tăng thời gian chờ lên 30s vì gọi AI thật rất lâu
    jest.setTimeout(30000);
  });

  afterAll(() => {
    // Test xong thì dọn dẹp, trả lại API Key cũ
    process.env.MISTRAL_API_KEY = originalApiKey;
  });

  it('FBK_01 (Real): Nhận phản hồi từ AI thật (Yêu cầu có MISTRAL_API_KEY đúng)', async () => {
    if (!process.env.MISTRAL_API_KEY) {
      console.warn('⚠️ Cảnh báo: Bạn chưa có MISTRAL_API_KEY trong .env. Test này sẽ tự động chuyển sang Fallback.');
    }

    const shotData = {
      avgElbowAngle: 90,
      avgKneeAngle: 120,
      tone: 'cheerful'
    };

    console.log('⏳ Đang gọi thẳng lên Mistral AI...');
    const response = await request(app).post('/api/feedback/shot').send(shotData);

    console.log('\n✅ --- [FBK_01] KẾT QUẢ TỪ AI THẬT --- ✅');
    console.log(JSON.stringify(response.body, null, 2));

    expect(response.statusCode).toBe(200);
    expect(response.body.success).toBe(true);
    expect(response.body).toHaveProperty('feedback');

    // Nếu có key thật, kết quả trả về KHÔNG ĐƯỢC LÀ model fallback
    if (process.env.MISTRAL_API_KEY && process.env.MISTRAL_API_KEY !== 'invalid_key') {
      expect(response.body.metadata?.model).not.toBe('rule_based_fallback');
    }
  });

  it('FBK_02 (Real Fallback): Ép hệ thống dùng Fallback bằng cách nhập sai API Key', async () => {
    // THỦ THUẬT: Cố tình tráo API Key thành chữ tào lao để Mistral từ chối (Lỗi 401)
    process.env.MISTRAL_API_KEY = 'fake_invalid_key_to_force_error';

    // Gửi dữ liệu góc khuỷu tay hẹp (<80) để xem Fallback có bắt đúng bệnh không
    const shotData = {
      avgElbowAngle: 75,
      avgKneeAngle: 130,
      tone: 'strict'
    };

    console.log('⏳ Đang cố tình gọi lỗi để thử thách cơ chế Fallback...');
    const response = await request(app).post('/api/feedback/shot').send(shotData);

    console.log('\n⚠️ --- [FBK_02] KẾT QUẢ TỪ FALLBACK THẬT --- ⚠️');
    console.log(JSON.stringify(response.body, null, 2));

    expect(response.statusCode).toBe(200); // Dù rớt AI vẫn phải là 200 nhờ Fallback tốt
    expect(response.body.success).toBe(true);
    
    // Khẳng định 100% nó đã nhảy vào Fallback
    expect(response.body.metadata.model).toBe('rule_based_fallback');
    expect(response.body.feedback.length).toBeGreaterThan(5);
    
    // Trả lại key cũ
    process.env.MISTRAL_API_KEY = originalApiKey;
  });

});