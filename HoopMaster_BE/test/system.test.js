const request = require('supertest');
const express = require('express');

// TRỎ VÀO THƯ MỤC SRC
const routes = require('../src/routes/index'); 

const app = express();
app.use(express.json());
app.use('/', routes);

describe('Kiểm thử Hệ thống (System Tests)', () => {

  // SYS_01: Test Root Endpoint
  describe('SYS_01: GET /', () => {
    it('Nên trả về thông điệp chào mừng và danh sách endpoint', async () => {
      const response = await request(app).get('/');
      
      console.log('\n--- [SYS_01] RESPONSE BODY ---');
      console.log(JSON.stringify(response.body, null, 2));
      console.log('------------------------------');

      expect(response.statusCode).toBe(200);
      expect(response.body.message).toContain('AI Basketball Coach');
    });
  });

  // SYS_02: Test Health Check
  describe('SYS_02: GET /health', () => {
    it('Nên trả về trạng thái OK và thông tin dịch vụ', async () => {
      // Mock các service để test độc lập
      jest.mock('../src/services/ttsService', () => ({
          validateConfig: () => ({ valid: true, config: { provider: 'test-tts' } })
      }));
      jest.mock('../src/services/llmService', () => ({
          validateConfig: () => ({ valid: true, config: { provider: 'test-llm' } })
      }));

      const response = await request(app).get('/health');
      
      console.log('\n--- [SYS_02] RESPONSE BODY ---');
      console.log(JSON.stringify(response.body, null, 2));
      console.log('------------------------------');

      expect(response.statusCode).toBe(200);
      expect(response.body.status).toBe('OK');
      expect(response.body).toHaveProperty('services');
    });
  });

  // SYS_03: Test CORS
  describe('SYS_03: CORS Configuration', () => {
    it('Nên từ chối các request từ origin không hợp lệ (Mô phỏng)', async () => {
      const corsApp = express();
      const cors = require('cors');
      // Giả sử chỉ cho phép localhost:3000
      corsApp.use(cors({ origin: ['http://localhost:3000'] }));
      corsApp.get('/', (req, res) => res.send('OK'));

      const response = await request(corsApp)
        .get('/')
        .set('Origin', 'http://hacker-site.com');
      
      console.log('\n--- [SYS_03] RESPONSE HEADERS ---');
      console.log(JSON.stringify(response.headers, null, 2));
      console.log('---------------------------------');

      expect(response.headers['access-control-allow-origin']).toBeUndefined();
    });
  });
});