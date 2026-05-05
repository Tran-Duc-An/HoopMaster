const { personalizeChat } = require('../src/services/personalizeService');
const conversationAgent = require('../src/services/conversationAgent');
const trainingPlanService = require('../src/services/trainingPlanService');

// Mock dependencies
jest.mock('../src/services/conversationAgent');
jest.mock('../src/services/trainingPlanService');

describe('Kiểm thử Đơn vị: Cá nhân hóa Giáo án (personalizeService)', () => {
  const mockUserId = 'user_pro';

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('PERS_01: Chat bình thường (không có JSON giáo án)', async () => {
    conversationAgent.aiConversation.mockResolvedValue({
      reply: 'Bạn tập bóng rổ lâu chưa?',
      audioBase64: 'audio_1'
    });

    const result = await personalizeChat(mockUserId, { text: 'Tôi là người mới' });

    expect(result.reply).toBe('Bạn tập bóng rổ lâu chưa?');
    // Đảm bảo KHÔNG gọi hàm tạo giáo án
    expect(trainingPlanService.createPlan).not.toHaveBeenCalled();
  });

  it('PERS_02: Tự động lưu giáo án khi AI trả về định dạng JSON', async () => {
    const mockJsonPlan = [
      { name: 'Ném phạt', description: 'Tập trung vào khuỷu tay', sets: 3, reps: 10 }
    ];
    
    conversationAgent.aiConversation.mockResolvedValue({
      reply: JSON.stringify(mockJsonPlan),
      audioBase64: 'audio_plan'
    });

    await personalizeChat(mockUserId, { text: 'Lập cho tôi giáo án' });

    // Kiểm tra xem hệ thống có tự động parse JSON và lưu vào DB không
    expect(trainingPlanService.createPlan).toHaveBeenCalledWith(
      mockUserId,
      expect.objectContaining({
        title: 'Personalized Plan',
        exercises: mockJsonPlan
      })
    );
  });
});