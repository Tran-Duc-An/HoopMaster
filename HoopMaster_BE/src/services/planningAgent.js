const User = require('../models/userModel');
const { addMessage, getHistory } = require('./conversationService');
const { synthesizeSpeech } = require('./ttsService');
const exerciseService = require('./exerciseService');
const trainingPlanService = require('./trainingPlanService');

const REQUIRED_FIELDS = ['goals', 'injuries', 'level', 'weeklyAvailability'];

const GOAL_KEYWORDS = [
  { key: 'shooting_accuracy', words: ['shoot', 'shot', 'shooting', '3-point', 'three point', 'nem', 'ném', 'accuracy'] },
  { key: 'strength', words: ['strength', 'strong', 'power', 'the luc', 'thể lực', 'tang luc', 'tăng lực'] },
  { key: 'mobility', words: ['mobility', 'flexible', 'linh hoat', 'linh hoạt', 'stretch', 'stiff'] },
  { key: 'warmup', words: ['warmup', 'warm up', 'khoi dong', 'khởi động'] },
  { key: 'conditioning', words: ['endurance', 'conditioning', 'stamina', 'cardio', 'suc ben', 'sức bền'] }
];

const INJURY_KEYWORDS = [
  { area: 'knee', words: ['knee', 'goi', 'gối'] },
  { area: 'ankle', words: ['ankle', 'co chan', 'cổ chân'] },
  { area: 'shoulder', words: ['shoulder', 'vai'] },
  { area: 'wrist', words: ['wrist', 'co tay', 'cổ tay'] },
  { area: 'back', words: ['back', 'lung', 'lưng'] },
  { area: 'hip', words: ['hip', 'hong', 'hông'] }
];

const LEVEL_KEYWORDS = [
  { level: 'beginner', words: ['beginner', 'newbie', 'new', 'moi', 'mới', 'bat dau', 'bắt đầu'] },
  { level: 'intermediate', words: ['intermediate', 'medium', 'trung binh', 'trung bình'] },
  { level: 'advanced', words: ['advanced', 'pro', 'gioi', 'giỏi', 'nang cao', 'nâng cao'] }
];

function normalizeText(text = '') {
  return text.toString().trim().toLowerCase();
}

function isConfirmIntent(text = '') {
  const normalized = normalizeText(text);
  return [
    'ok',
    'okay',
    'confirm',
    'save',
    'save it',
    'activate',
    'agree',
    'yes',
    'dong y',
    'luu',
    'luu giao an',
    'xac nhan',
    'chot'
  ].some(keyword => normalized.includes(keyword));
}

function hasAny(text, words) {
  return words.some(word => text.includes(word));
}

function unique(values) {
  return [...new Set(values.filter(Boolean))];
}

function extractGoals(text) {
  return GOAL_KEYWORDS
    .filter(item => hasAny(text, item.words))
    .map(item => item.key);
}

function extractLevel(text) {
  const match = LEVEL_KEYWORDS.find(item => hasAny(text, item.words));
  return match?.level;
}

function extractInjuries(text) {
  if (/(no injury|no injuries|khong chan thuong|không chấn thương|khong dau|không đau)/i.test(text)) {
    return [];
  }

  return INJURY_KEYWORDS
    .filter(item => hasAny(text, item.words))
    .map(item => ({
      area: item.area,
      severity: extractSeverity(text),
      notes: '',
      active: true
    }));
}

function extractSeverity(text) {
  if (/(severe|bad|nang|nặng|rat dau|rất đau)/i.test(text)) return 'severe';
  if (/(moderate|medium|vua|vừa)/i.test(text)) return 'moderate';
  if (/(mild|light|nhe|nhẹ)/i.test(text)) return 'mild';
  return 'unknown';
}

function extractAvailability(text) {
  const match = text.match(/(\d+)\s*(day|days|buoi|buổi|ngay|ngày|lan|lần)/i);
  if (!match) return undefined;
  const value = Number(match[1]);
  if (!Number.isInteger(value) || value < 1 || value > 7) return undefined;
  return value;
}

function extractDuration(text) {
  const match = text.match(/(\d+)\s*(minute|minutes|min|phut|phút)/i);
  if (!match) return undefined;
  const value = Number(match[1]);
  if (!Number.isInteger(value) || value < 5 || value > 180) return undefined;
  return value;
}

function extractEquipment(text) {
  const equipment = [];
  if (/(ball|basketball|bong|bóng)/i.test(text)) equipment.push('basketball');
  if (/(hoop|rim|ro|rổ)/i.test(text)) equipment.push('hoop');
  if (/(dumbbell|ta tay|tạ tay)/i.test(text)) equipment.push('dumbbells');
  if (/(band|resistance band|day khang luc|dây kháng lực)/i.test(text)) equipment.push('resistance_band');
  if (/(no equipment|khong dung cu|không dụng cụ)/i.test(text)) return [];
  return equipment;
}

function mergeProfile(existing = {}, incoming = {}) {
  const hasExistingInjuries = Array.isArray(existing.injuries);
  const existingInjuries = hasExistingInjuries ? existing.injuries : [];
  const incomingInjuries = Array.isArray(incoming.injuries) ? incoming.injuries : undefined;

  return {
    level: incoming.level || existing.level || 'unknown',
    goals: unique([...(existing.goals || []), ...(incoming.goals || [])]),
    injuries: incomingInjuries !== undefined
      ? mergeInjuries(existingInjuries, incomingInjuries)
      : (hasExistingInjuries ? existingInjuries : undefined),
    equipment: unique([...(existing.equipment || []), ...(incoming.equipment || [])]),
    weeklyAvailability: incoming.weeklyAvailability || existing.weeklyAvailability,
    sessionDurationMinutes: incoming.sessionDurationMinutes || existing.sessionDurationMinutes,
    updatedAt: new Date()
  };
}

function mergeInjuries(existing, incoming) {
  if (incoming.length === 0) return [];
  const byArea = new Map(existing.map(injury => [injury.area, injury]));
  incoming.forEach(injury => byArea.set(injury.area, { ...byArea.get(injury.area), ...injury }));
  return [...byArea.values()];
}

function extractProfilePatch(text) {
  const normalized = normalizeText(text);
  const patch = {
    goals: extractGoals(normalized),
    equipment: extractEquipment(normalized)
  };

  const level = extractLevel(normalized);
  if (level) patch.level = level;

  const injuries = extractInjuries(normalized);
  if (injuries.length > 0 || /(no injury|no injuries|khong chan thuong|không chấn thương)/i.test(normalized)) {
    patch.injuries = injuries;
  }

  const weeklyAvailability = extractAvailability(normalized);
  if (weeklyAvailability) patch.weeklyAvailability = weeklyAvailability;

  const sessionDurationMinutes = extractDuration(normalized);
  if (sessionDurationMinutes) patch.sessionDurationMinutes = sessionDurationMinutes;

  return patch;
}

function getMissingFields(profile) {
  return REQUIRED_FIELDS.filter(field => {
    if (field === 'goals') return !profile.goals || profile.goals.length === 0;
    if (field === 'injuries') return !Array.isArray(profile.injuries);
    if (field === 'level') return !profile.level || profile.level === 'unknown';
    if (field === 'weeklyAvailability') return !profile.weeklyAvailability;
    return !profile[field];
  });
}

function buildQuestion(missingFields) {
  const first = missingFields[0];
  if (first === 'goals') return 'Mục tiêu chính của bạn là gì: cải thiện form ném, tăng thể lực, tăng linh hoạt, hay khởi động/phòng chấn thương?';
  if (first === 'injuries') return 'Bạn có tiền sử chấn thương hoặc vùng đang đau nào không? Ví dụ: gối, cổ chân, vai, cổ tay, lưng. Nếu không có, hãy nói rõ là không có chấn thương.';
  if (first === 'level') return 'Trình độ hiện tại của bạn là beginner, intermediate hay advanced?';
  if (first === 'weeklyAvailability') return 'Bạn có thể tập bao nhiêu buổi mỗi tuần và mỗi buổi khoảng bao nhiêu phút?';
  return 'Bạn cho mình thêm mục tiêu, chấn thương, trình độ và lịch tập để mình lập plan phù hợp nhé.';
}

function selectExercises(profile) {
  const catalog = exerciseService.getAllExercises();
  const activeInjuries = (profile.injuries || []).filter(injury => injury.active !== false).map(injury => injury.area);
  const goals = profile.goals || [];

  const categoryPriority = [];
  if (goals.includes('warmup') || activeInjuries.length > 0) categoryPriority.push('warmup', 'stretching');
  if (goals.includes('mobility')) categoryPriority.push('mobility', 'stretching', 'warmup');
  if (goals.includes('strength') || goals.includes('conditioning')) categoryPriority.push('strength');
  if (goals.includes('shooting_accuracy')) categoryPriority.push('warmup', 'strength', 'stretching');
  if (categoryPriority.length === 0) categoryPriority.push('warmup', 'strength', 'mobility', 'stretching');

  const avoidStrength = activeInjuries.some(area => ['wrist', 'shoulder'].includes(area));
  const ordered = catalog
    .filter(exercise => !(avoidStrength && exercise.category === 'strength'))
    .sort((a, b) => {
      const aIndex = categoryPriority.indexOf(a.category);
      const bIndex = categoryPriority.indexOf(b.category);
      return (aIndex === -1 ? 99 : aIndex) - (bIndex === -1 ? 99 : bIndex);
    });

  return ordered.slice(0, 4).map((exercise, index) => ({
    exerciseId: exercise.id,
    name: exercise.name,
    category: exercise.category,
    pose: exercise.pose,
    description: exercise.description,
    sets: exercise.category === 'strength' ? 3 : 2,
    reps: exercise.duration ? undefined : exercise.count,
    duration: exercise.duration || (exercise.category === 'stretching' ? '20s each side' : undefined),
    reason: buildExerciseReason(exercise, profile),
    safetyNotes: buildSafetyNote(exercise, activeInjuries),
    order: index + 1
  }));
}

function buildExerciseReason(exercise, profile) {
  const goals = profile.goals || [];
  if (exercise.category === 'warmup') return 'Chuẩn bị khớp và cơ trước khi vào bài chính.';
  if (exercise.category === 'stretching') return 'Tăng linh hoạt và giảm căng cơ sau hoặc trước buổi tập nhẹ.';
  if (exercise.category === 'strength' && goals.includes('shooting_accuracy')) {
    return 'Tăng sức mạnh thân trên để giữ form ném ổn định hơn.';
  }
  if (exercise.category === 'strength') return 'Tăng nền tảng thể lực cho vận động bóng rổ.';
  return 'Phù hợp với mục tiêu tập luyện hiện tại.';
}

function buildSafetyNote(exercise, injuries) {
  if (!injuries.length) return 'Dừng lại nếu thấy đau bất thường.';
  if (exercise.category === 'strength' && injuries.some(area => ['wrist', 'shoulder'].includes(area))) {
    return 'Giảm biên độ hoặc bỏ bài này nếu cổ tay/vai đau.';
  }
  if (injuries.includes('knee') && exercise.category !== 'stretching') {
    return 'Giữ gối thẳng hàng với mũi chân, không ép sâu nếu đau.';
  }
  return `Điều chỉnh nhẹ vì bạn có tiền sử: ${injuries.join(', ')}.`;
}

function buildPlanPayload(userId, profile) {
  const primaryGoal = profile.goals?.[0] || 'general_training';
  const injuries = (profile.injuries || []).filter(injury => injury.active !== false).map(injury => injury.area);
  return {
    userId,
    source: 'personalized',
    status: 'draft',
    title: 'Personalized Basketball Training Plan',
    description: 'Generated from planning chat based on goals, level, injuries, and availability.',
    goal: primaryGoal,
    injuryConstraints: injuries,
    exercises: selectExercises(profile),
    schedule: {
      daysPerWeek: profile.weeklyAvailability,
      sessionDurationMinutes: profile.sessionDurationMinutes || 30
    },
    createdBy: 'agent',
    metadata: { profileSnapshot: profile }
  };
}

async function planningChat(userId, { text, audioBase64 }) {
  const userText = text || '';
  if (!userText && !audioBase64) throw new Error('No input text or audio');
  if (audioBase64 && !text) throw new Error('Audio input is not supported yet');

  const user = await User.findById(userId);
  if (!user) throw new Error('User not found');

  await addMessage(userId, 'planning', 'user', userText);

  if (isConfirmIntent(userText)) {
    const latestDraft = await trainingPlanService.getLatestDraftPlan(userId);
    if (latestDraft) {
      const result = await confirmPlanningPlan(userId, latestDraft._id);
      await addMessage(userId, 'planning', 'assistant', result.reply);
      const ttsResult = await synthesizeSpeech(result.reply, user.tone || 'neutral');
      return {
        ...result,
        audioBase64: ttsResult.audioBase64
      };
    }
  }

  const profilePatch = extractProfilePatch(userText);
  const mergedProfile = mergeProfile(user.trainingProfile || {}, profilePatch);
  user.trainingProfile = mergedProfile;
  user.updatedAt = new Date();
  await user.save();

  const missingFields = getMissingFields(mergedProfile);
  if (missingFields.length > 0) {
    const reply = buildQuestion(missingFields);
    await addMessage(userId, 'planning', 'assistant', reply);
    const ttsResult = await synthesizeSpeech(reply, user.tone || 'neutral');
    return {
      type: 'question',
      reply,
      audioBase64: ttsResult.audioBase64,
      collectedProfile: mergedProfile,
      missingFields,
      planDraft: null
    };
  }

  const planPayload = buildPlanPayload(userId, mergedProfile);
  const plan = await trainingPlanService.createPlan(userId, planPayload);
  const reply = 'Mình đã tạo một giáo án cá nhân hóa dạng draft. Nếu bạn đồng ý, hãy xác nhận để lưu làm giáo án active cho lần tập sau.';
  await addMessage(userId, 'planning', 'assistant', reply);
  const ttsResult = await synthesizeSpeech(reply, user.tone || 'neutral');

  return {
    type: 'plan_draft',
    reply,
    audioBase64: ttsResult.audioBase64,
    collectedProfile: mergedProfile,
    missingFields: [],
    planDraft: plan
  };
}

async function confirmPlanningPlan(userId, planId) {
  const plan = await trainingPlanService.activatePlan(userId, planId);
  return {
    type: 'saved_plan',
    reply: 'Giáo án cá nhân hóa đã được lưu và đặt làm active.',
    plan
  };
}

async function getPlanningHistory(userId, limit = 10) {
  return getHistory(userId, 'planning', limit);
}

module.exports = {
  planningChat,
  confirmPlanningPlan,
  getPlanningHistory,
  extractProfilePatch,
  getMissingFields,
  buildPlanPayload,
  isConfirmIntent
};
