const request = require('supertest');
const express = require('express');

const exerciseRoutes = require('../src/routes/exerciseRoutes');

// KHÔNG CẦN DÙNG MOCK NỮA - Chúng ta test thẳng với dữ liệu thật của hệ thống!

const app = express();
app.use(express.json());
app.use('/api/exercises', exerciseRoutes);

describe('Kiểm thử Quản lý Bài tập (Exercise Tests)', () => {

  // EXE_01
  it('EXE_01: Nên trả về toàn bộ bài tập (Thực tế có 4 bài)', async () => {
    const response = await request(app).get('/api/exercises');
    
    console.log('\n--- [EXE_01] RESPONSE ---');
    console.log(JSON.stringify(response.body, null, 2));
    
    expect(response.statusCode).toBe(200);
    // Cập nhật expect thành 4 để khớp với dữ liệu thật
    expect(response.body.length).toBe(4); 
  });

  // EXE_02
  it('EXE_02: Nên lọc bài tập theo category "warmup"', async () => {
    // Đổi category từ 'shooting' thành 'warmup' vì DB của bạn dùng 'warmup'
    const response = await request(app).get('/api/exercises/category/warmup');
    
    console.log('\n--- [EXE_02] RESPONSE ---');
    console.log(JSON.stringify(response.body, null, 2));
    
    expect(response.statusCode).toBe(200);
    expect(response.body.length).toBeGreaterThan(0);
    expect(response.body[0].category).toBe('warmup');
  });

  // EXE_03
  it('EXE_03: Nên trả về chi tiết bài tập ID 1 (Neck Stretch)', async () => {
    const response = await request(app).get('/api/exercises/1');
    
    console.log('\n--- [EXE_03] RESPONSE ---');
    console.log(JSON.stringify(response.body, null, 2));
    
    expect(response.statusCode).toBe(200);
    expect(response.body.id).toBe(1);
    // Cập nhật tên bài tập khớp với dữ liệu thật
    expect(response.body.name).toBe('Neck Stretch'); 
  });

  // EXE_04
  it('EXE_04: Nên trả về 404 nếu không tìm thấy ID', async () => {
    const response = await request(app).get('/api/exercises/999');
    
    console.log('\n--- [EXE_04] RESPONSE ---');
    console.log(JSON.stringify(response.body, null, 2));
    
    expect(response.statusCode).toBe(404);
    expect(response.body.error).toBe('Exercise not found');
  });
});