// Thiết lập biến môi trường ẢO để test nhanh hơn
process.env.SESSION_TIMEOUT_MS = '5000';    // Timeout sau 5 giây (thay vì 5 phút)
process.env.MAX_FRAMES_BUFFER = '3';        // Buffer tối đa 3 frame (thay vì 100)
process.env.FEEDBACK_COOLDOWN_MS = '2000';  // Cooldown 2 giây (thay vì 3 giây)

const sessionService = require('../src/services/sessionService');

describe('Kiểm thử Đơn vị: Người gác cổng Hệ thống (sessionService)', () => {

  // Dọn dẹp tất cả session sau mỗi bài test
  afterEach(() => {
    // Hack nhỏ để lấy toàn bộ session map và clear
    const socketId = 'test_user';
    sessionService.deleteSession(socketId);
    jest.useRealTimers(); 
  });

  describe('1. Quản lý Khởi tạo và Lưu trữ', () => {
    
    it('SES_01: Tạo mới và lấy đúng session của User', () => {
      const socketId = 'user_vip_001';
      
      const session = sessionService.getSession(socketId);
      expect(session.socketId).toBe(socketId);
      expect(session.frameBuffer).toEqual([]);
      expect(session.sessionStats.totalFrames).toBe(0);

      sessionService.incrementStats(socketId, 'totalFrames');
      const sameSession = sessionService.getSession(socketId);
      expect(sameSession.sessionStats.totalFrames).toBe(1);
    });
  });

  describe('2. Quản lý Bộ nhớ (Chống tràn RAM)', () => {
    
    it('SES_02: Giới hạn Frame Buffer nghiêm ngặt', () => {
      const socketId = 'user_buffer_test';
      sessionService.getSession(socketId);

      // Gửi 4 frame liên tục (MAX_FRAMES_BUFFER = 3)
      sessionService.addFrameToBuffer(socketId, { id: 'frame_1' });
      sessionService.addFrameToBuffer(socketId, { id: 'frame_2' });
      sessionService.addFrameToBuffer(socketId, { id: 'frame_3' });
      sessionService.addFrameToBuffer(socketId, { id: 'frame_4' }); 

      const session = sessionService.getSession(socketId);
      
      // Buffer size KHÔNG ĐƯỢC vượt quá 3
      expect(session.frameBuffer.length).toBe(3);
      
      // frame_1 phải bị đẩy ra ngoài
      expect(session.frameBuffer[0].id).toBe('frame_2');
      expect(session.frameBuffer[2].id).toBe('frame_4');
    });
  });

  describe('3. Quản lý Thời gian (Chống Spam & Dọn rác)', () => {
    
    it('SES_03: Chống Spam AI bằng Cooldown', () => {
      jest.useFakeTimers(); 
      
      const socketId = 'user_spammer';
      sessionService.getSession(socketId);

      // Lần đầu tiên: Có thể gửi
      expect(sessionService.canSendFeedback(socketId)).toBe(true);

      // Cập nhật thời gian
      sessionService.updateFeedbackTime(socketId);

      // Ngay lập tức sau đó: KHÔNG ĐƯỢC gửi
      expect(sessionService.canSendFeedback(socketId)).toBe(false);

      // Tua nhanh thời gian qua 2.5 giây (Cooldown là 2s)
      jest.advanceTimersByTime(2500);

      // Lúc này đã an toàn
      expect(sessionService.canSendFeedback(socketId)).toBe(true);
    });
  });
});