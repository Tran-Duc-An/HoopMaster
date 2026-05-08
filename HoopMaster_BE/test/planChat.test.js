const { planningChat, extractProfilePatch } = require('../src/services/planningAgent');
const User = require('../src/models/userModel');
const trainingPlanService = require('../src/services/trainingPlanService');

jest.mock('../src/models/userModel');
jest.mock('../src/services/conversationService', () => ({
  addMessage: jest.fn(),
  getHistory: jest.fn(() => [])
}));
jest.mock('../src/services/ttsService', () => ({
  synthesizeSpeech: jest.fn(() => ({ audioBase64: 'fake_audio_base64' }))
}));
jest.mock('../src/services/trainingPlanService', () => ({
  createPlan: jest.fn(),
  getLatestDraftPlan: jest.fn(),
  activatePlan: jest.fn()
}));

function mockUser(profile = {}) {
  return {
    _id: 'user123',
    tone: 'neutral',
    trainingProfile: profile,
    save: jest.fn().mockResolvedValue(true)
  };
}

describe('planningAgent', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('asks a follow-up question when required planning fields are missing', async () => {
    User.findById.mockResolvedValue(mockUser());

    const result = await planningChat('user123', {
      text: 'I want to improve shooting.'
    });

    expect(result.type).toBe('question');
    expect(result.missingFields).toContain('injuries');
    expect(trainingPlanService.createPlan).not.toHaveBeenCalled();
  });

  it('creates a structured draft plan when enough profile information is present', async () => {
    User.findById.mockResolvedValue(mockUser());
    trainingPlanService.createPlan.mockResolvedValue({
      _id: 'draft1',
      status: 'draft',
      exercises: []
    });

    const result = await planningChat('user123', {
      text: 'I am a beginner, no injuries, I want shooting accuracy and strength, 3 days per week, 30 minutes.'
    });

    expect(result.type).toBe('plan_draft');
    expect(result.missingFields).toEqual([]);
    expect(trainingPlanService.createPlan).toHaveBeenCalledWith(
      'user123',
      expect.objectContaining({
        source: 'personalized',
        status: 'draft',
        goal: 'shooting_accuracy'
      })
    );
  });

  it('activates latest draft when user confirms by text', async () => {
    User.findById.mockResolvedValue(mockUser());
    trainingPlanService.getLatestDraftPlan.mockResolvedValue({ _id: 'draft1' });
    trainingPlanService.activatePlan.mockResolvedValue({
      _id: 'draft1',
      status: 'active'
    });

    const result = await planningChat('user123', { text: 'save it' });

    expect(result.type).toBe('saved_plan');
    expect(trainingPlanService.activatePlan).toHaveBeenCalledWith('user123', 'draft1');
  });

  it('throws when text and audio are both missing', async () => {
    await expect(planningChat('user123', {})).rejects.toThrow('No input text or audio');
  });

  it('parses "sessions per week" phrasing for weekly availability', () => {
    const patch = extractProfilePatch('3 sessions per week and 45mins per session');
    expect(patch.weeklyAvailability).toBe(3);
    expect(patch.sessionDurationMinutes).toBe(45);
  });
});
