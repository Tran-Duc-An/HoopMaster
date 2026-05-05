const { aiConversation } = require('../src/services/conversationAgent');
const mistralService = require('../src/services/mistralService'); // Sửa thành mistralService
const ttsService = require('../src/services/ttsService');
const conversationService = require('../src/services/conversationService');

// Mock các dependencies
jest.mock('../src/services/mistralService'); // Sửa thành mistralService
jest.mock('../src/services/ttsService');
jest.mock('../src/services/conversationService');

describe('Kiểm thử Đơn vị: Điều phối Hội thoại (conversationAgent)', () => {
  const mockUserId = 'user123';
  const mockType = 'general';
  
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('AGENT_01: Quy trình hội thoại text chuẩn xác', async () => {
    // 1. Giả lập lịch sử hội thoại
    conversationService.getHistory.mockResolvedValue([
      { role: 'user', content: 'Chào AI' }
    ]);
    
    // 2. Giả lập AI trả lời (Gọi đúng qua mistralService)
    mistralService.callMistralAPI.mockResolvedValue('Chào bạn, tôi là HLV đây!');
    
    // 3. Giả lập Text-to-Speech
    ttsService.synthesizeSpeech.mockResolvedValue({ audioBase64: 'base64_audio_data' });

    const result = await aiConversation(mockUserId, mockType, { text: 'Hello' }, 'System Prompt');

    // Kiểm tra xem có lưu tin nhắn của User và Assistant vào DB không
    expect(conversationService.addMessage).toHaveBeenCalledTimes(2);
    expect(mistralService.callMistralAPI).toHaveBeenCalled();
    expect(ttsService.synthesizeSpeech).toHaveBeenCalledWith('Chào bạn, tôi là HLV đây!', 'focus');
    
    expect(result).toEqual({
      reply: 'Chào bạn, tôi là HLV đây!',
      audioBase64: 'base64_audio_data'
    });
  });

  it('AGENT_02: Báo lỗi nếu không có cả text và audio input', async () => {
    await expect(
      aiConversation(mockUserId, mockType, { text: '', audioBase64: '' })
    ).rejects.toThrow('No input text or audio');
  });
});