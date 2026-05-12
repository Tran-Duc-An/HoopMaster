const User = require('../models/userModel');
const { addMessage, getHistory, normalizeSessionId } = require('./conversationService');
const { synthesizeSpeech } = require('./ttsService');
const exerciseService = require('./exerciseService');
const trainingPlanService = require('./trainingPlanService');
const { callMistralAPI } = require('./mistralService');

const REQUIRED_FIELDS = ['goals', 'level', 'weeklyAvailability'];
const MIN_PERSONAL_EXERCISES = 1;
const MAX_PERSONAL_EXERCISES = 2;

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

function safeNormalizeSessionId(sessionId) {
  if (typeof normalizeSessionId === 'function') {
    return normalizeSessionId(sessionId);
  }
  if (!sessionId || typeof sessionId !== 'string') return 'default';
  const trimmed = sessionId.trim();
  return trimmed || 'default';
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
  // Default to mild - don't ask for details
  return 'mild';
}

function extractAvailability(text) {
  const match = text.match(
    /(\d+)\s*(day|days|session|sessions|buoi|buổi|buoi tap|buổi tập|ngay|ngày|lan|lần)(?:\s*(per|\/)\s*week|\s*mot\s*tuan|\s*mỗi\s*tuần)?/i
  );
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

function getProfileClarifications(profile) {
  // Don't ask detailed injury questions - accept simple mentions like "knee" at face value
  return [];
}

function buildQuestion(missingFields, clarifications = []) {
  const first = missingFields[0];
  if (first === 'goals') return 'What is your main goal: shooting accuracy, strength, mobility, warm-up, or injury prevention?';
  if (first === 'level') return 'What is your current level: beginner, intermediate, or advanced?';
  if (first === 'weeklyAvailability') return 'How many sessions can you train per week, and about how many minutes per session?';
  return 'Please share your goals, level, and schedule so I can build the right plan for you.';
}

function buildFollowUpPrompt(profile, missingFields, clarifications, latestUserMessage) {
  return `You are a basketball rehab-aware coach assistant.

Goal:
Generate EXACTLY ONE concise follow-up question in English to collect missing planning info.

Rules:
1) Return only one question sentence (no markdown, no bullets).
2) Ask for only the highest-priority missing detail.
3) Keep under 28 words.
4) Tone: supportive and practical.
5) DO NOT ask for injury details like severity, side, or painful movements. Accept simple mentions like "knee" or "ankle" as-is.

Latest user message:
${latestUserMessage || ''}

Current profile snapshot:
${JSON.stringify(profile, null, 2)}

Missing fields:
${JSON.stringify(missingFields)}

Clarifications needed:
${JSON.stringify(clarifications)}
`;
}

async function buildQuestionWithLlm(profile, missingFields, clarifications, latestUserMessage) {
  const fallback = buildQuestion(missingFields, clarifications);
  try {
    const prompt = buildFollowUpPrompt(profile, missingFields, clarifications, latestUserMessage);
    const rawResponse = await callMistralAPI(prompt);
    const candidate = (rawResponse || '').replace(/[`"]/g, '').trim();
    if (!candidate) return fallback;
    if (!candidate.endsWith('?')) {
      return `${candidate.replace(/[.!]+$/, '')}?`;
    }
    return candidate;
  } catch (error) {
    console.error('[PlanningChat] LLM follow-up question failed:', error.message);
    return fallback;
  }
}

function getDefaultExerciseIds() {
  const defaultPlan = exerciseService.getDefaultPlan();
  return new Set((defaultPlan?.exercises || []).map(ex => Number(ex.exerciseId)).filter(Number.isFinite));
}

function selectExercises(profile, options = {}) {
  const catalog = exerciseService.getAllExercises();
  const excludeExerciseIds = options.excludeExerciseIds || new Set();
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
    .filter(exercise => !excludeExerciseIds.has(Number(exercise.id)))
    .filter(exercise => !(avoidStrength && exercise.category === 'strength'))
    .sort((a, b) => {
      const aIndex = categoryPriority.indexOf(a.category);
      const bIndex = categoryPriority.indexOf(b.category);
      return (aIndex === -1 ? 99 : aIndex) - (bIndex === -1 ? 99 : bIndex);
    });

  return ordered.slice(0, MAX_PERSONAL_EXERCISES).map((exercise, index) => ({
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
  if (exercise.category === 'warmup') return 'Prepares joints and muscles before the main workout.';
  if (exercise.category === 'stretching') return 'Improves mobility and helps reduce muscle tightness.';
  if (exercise.category === 'strength' && goals.includes('shooting_accuracy')) {
    return 'Builds upper-body strength to support a more stable shooting form.';
  }
  if (exercise.category === 'strength') return 'Builds foundational strength for basketball movement.';
  return 'Aligned with your current training goals.';
}

function buildSafetyNote(exercise, injuries) {
  if (!injuries.length) return 'Stop immediately if you feel unusual pain.';
  if (exercise.category === 'strength' && injuries.some(area => ['wrist', 'shoulder'].includes(area))) {
    return 'Reduce range of motion or skip this exercise if wrist or shoulder pain appears.';
  }
  if (injuries.includes('knee') && exercise.category !== 'stretching') {
    return 'Keep knees aligned with toes and avoid deep ranges if pain increases.';
  }
  return `Adjust intensity carefully due to your injury history: ${injuries.join(', ')}.`;
}

function toTitleCase(text) {
  return text
    .split('_')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}

function buildSpecificPlanTitle(profile) {
  const level = profile.level && profile.level !== 'unknown' ? toTitleCase(profile.level) : 'Personalized';
  const primaryGoal = profile.goals?.[0] ? toTitleCase(profile.goals[0]) : 'Basketball Development';
  const daysPerWeek = profile.weeklyAvailability || 3;
  const duration = profile.sessionDurationMinutes || 30;
  return `${level} ${primaryGoal} Program - ${daysPerWeek} Days/Week (${duration} min/session)`;
}

function buildPlanPayload(userId, profile) {
  const primaryGoal = profile.goals?.[0] || 'general_training';
  const injuries = (profile.injuries || []).filter(injury => injury.active !== false).map(injury => injury.area);
  const title = buildSpecificPlanTitle(profile);
  const defaultExerciseIds = getDefaultExerciseIds();
  return {
    userId,
    source: 'personalized',
    status: 'draft',
    title,
    description: `Focused ${toTitleCase(primaryGoal)} plan generated from your profile, schedule, and injury constraints.`,
    goal: primaryGoal,
    injuryConstraints: injuries,
    exercises: selectExercises(profile, { excludeExerciseIds: defaultExerciseIds }),
    schedule: {
      daysPerWeek: profile.weeklyAvailability,
      sessionDurationMinutes: profile.sessionDurationMinutes || 30
    },
    createdBy: 'agent',
    metadata: { profileSnapshot: profile }
  };
}

function extractJsonObject(rawText = '') {
  const trimmed = rawText.trim();
  if (!trimmed) return null;

  const fencedMatch = trimmed.match(/```(?:json)?\s*([\s\S]*?)\s*```/i);
  const jsonCandidate = fencedMatch ? fencedMatch[1] : trimmed;

  try {
    return JSON.parse(jsonCandidate);
  } catch (_) {
    const start = jsonCandidate.indexOf('{');
    const end = jsonCandidate.lastIndexOf('}');
    if (start >= 0 && end > start) {
      return JSON.parse(jsonCandidate.slice(start, end + 1));
    }
    throw new Error('LLM output is not valid JSON');
  }
}

function buildExerciseCatalogSummary(catalog) {
  return catalog.map(ex => ({
    id: ex.id,
    name: ex.name,
    category: ex.category,
    pose: ex.pose,
    defaultSets: ex.target?.sets || (ex.category === 'strength' ? 3 : 2),
    defaultReps: ex.target?.reps || ex.count || null,
    defaultDuration: ex.duration || null,
    description: ex.description
  }));
}

function coercePositiveInteger(value, fallback) {
  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed < 1) return fallback;
  return parsed;
}

function buildPlanningPrompt(profile, catalog, defaultExerciseIds) {
  const catalogSummary = buildExerciseCatalogSummary(catalog);
  return `You are an expert basketball strength and skill coach.

Task:
Generate ONE personalized basketball training plan in ENGLISH and return STRICT JSON ONLY.

User profile:
${JSON.stringify(profile, null, 2)}

Exercise catalog (you must only use these exercises):
${JSON.stringify(catalogSummary, null, 2)}

Rules:
1) Output JSON only (no markdown, no extra text).
2) Use ONLY 1 or 2 exercises from the catalog.
3) Never use exercises with IDs in this default set: ${JSON.stringify([...defaultExerciseIds])}.
4) You MUST prioritize user injury safety in both selection and safetyNotes. If injuries include knee/ankle, avoid deep knee-flexion stress; if wrist/shoulder, avoid high-load upper-body pushing.
3) Keep structure exactly:
{
  "title": "string",
  "description": "string",
  "goal": "string",
  "exercises": [
    {
      "exerciseId": number,
      "sets": number,
      "reps": number|null,
      "duration": "string|null",
      "reason": "string",
      "safetyNotes": "string"
    }
  ]
}
5) Title must be specific (not generic), include level + primary goal + frequency.
6) All text must be English.
7) Respect injury constraints in safetyNotes and selection.
8) goal should be one of: shooting_accuracy, strength, mobility, warmup, conditioning, general_training.`;
}

function normalizeLlmPlan(userId, profile, llmPlan, catalog) {
  if (!llmPlan || typeof llmPlan !== 'object') {
    throw new Error('LLM plan payload is empty');
  }

  const catalogById = new Map(catalog.map(ex => [Number(ex.id), ex]));
  const rawExercises = Array.isArray(llmPlan.exercises) ? llmPlan.exercises.slice(0, MAX_PERSONAL_EXERCISES) : [];
  if (rawExercises.length === 0) {
    throw new Error('LLM returned no exercises');
  }

  const usedIds = new Set();
  const normalizedExercises = rawExercises
    .map((item) => {
      const exerciseId = Number(item.exerciseId);
      const exercise = catalogById.get(exerciseId);
      if (!exercise || usedIds.has(exerciseId)) return null;
      usedIds.add(exerciseId);

      const defaultSets = exercise.target?.sets || (exercise.category === 'strength' ? 3 : 2);
      const defaultReps = exercise.target?.reps || exercise.count;
      const resolvedReps = item.reps === null ? null : coercePositiveInteger(item.reps, defaultReps);

      return {
        exerciseId: exercise.id,
        name: exercise.name,
        category: exercise.category,
        pose: exercise.pose,
        description: exercise.description,
        sets: coercePositiveInteger(item.sets, defaultSets),
        reps: exercise.duration ? undefined : resolvedReps,
        duration: typeof item.duration === 'string' && item.duration.trim()
          ? item.duration.trim()
          : (exercise.duration || undefined),
        reason: typeof item.reason === 'string' && item.reason.trim()
          ? item.reason.trim()
          : buildExerciseReason(exercise, profile),
        safetyNotes: typeof item.safetyNotes === 'string' && item.safetyNotes.trim()
          ? item.safetyNotes.trim()
          : buildSafetyNote(
            exercise,
            (profile.injuries || []).filter(injury => injury.active !== false).map(injury => injury.area)
          )
      };
    })
    .filter(Boolean)
    .map((exercise, index) => ({ ...exercise, order: index + 1 }));

  if (normalizedExercises.length < MIN_PERSONAL_EXERCISES) {
    throw new Error('LLM returned insufficient valid exercises');
  }

  const primaryGoal = profile.goals?.[0] || 'general_training';
  const injuries = (profile.injuries || []).filter(injury => injury.active !== false).map(injury => injury.area);

  return {
    userId,
    source: 'personalized',
    status: 'draft',
    title: typeof llmPlan.title === 'string' && llmPlan.title.trim()
      ? llmPlan.title.trim()
      : buildSpecificPlanTitle(profile),
    description: typeof llmPlan.description === 'string' && llmPlan.description.trim()
      ? llmPlan.description.trim()
      : `Focused ${toTitleCase(primaryGoal)} plan generated from your profile, schedule, and injury constraints.`,
    goal: typeof llmPlan.goal === 'string' && llmPlan.goal.trim() ? llmPlan.goal.trim() : primaryGoal,
    injuryConstraints: injuries,
    exercises: normalizedExercises,
    schedule: {
      daysPerWeek: profile.weeklyAvailability,
      sessionDurationMinutes: profile.sessionDurationMinutes || 30
    },
    createdBy: 'agent',
    metadata: {
      profileSnapshot: profile,
      generation: { mode: 'llm_mistral' }
    }
  };
}

async function buildPlanPayloadWithLlm(userId, profile) {
  const defaultExerciseIds = getDefaultExerciseIds();
  const catalog = exerciseService
    .getAllExercises()
    .filter(exercise => !defaultExerciseIds.has(Number(exercise.id)));
  if (!catalog.length) throw new Error('Exercise catalog is empty');

  const prompt = buildPlanningPrompt(profile, catalog, defaultExerciseIds);
  const rawResponse = await callMistralAPI(prompt);
  const llmPlan = extractJsonObject(rawResponse);
  return normalizeLlmPlan(userId, profile, llmPlan, catalog);
}

async function buildSessionProfile(userId, sessionId, currentUserText) {
  const history = await getHistory(userId, 'planning', 100, sessionId);
  const sessionUserMessages = history
    .filter(message => message.role === 'user')
    .map(message => message.content || '');

  if (currentUserText && sessionUserMessages.length === 0) {
    sessionUserMessages.push(currentUserText);
  }

  return sessionUserMessages.reduce((profile, content) => {
    const patch = extractProfilePatch(content);
    return mergeProfile(profile, patch);
  }, {});
}

async function planningChat(userId, { text, audioBase64, sessionId }) {
  const startedAt = Date.now();
  const normalizedSessionId = safeNormalizeSessionId(sessionId);
  const userText = text || '';
  if (!userText && !audioBase64) throw new Error('No input text or audio');
  if (audioBase64 && !text) throw new Error('Audio input is not supported yet');

  console.log(`[PlanningChat] Start userId=${userId} sessionId=${normalizedSessionId} textLength=${userText.length} hasAudio=${Boolean(audioBase64)}`);
  let user = await User.findById(userId);
  if (!user) {
    // Auto-create user if not exists (planning chat can work without explicit registration)
    console.log(`[PlanningChat] User not found, creating anonymous user: ${userId}`);
    user = await User.create({
      _id: userId,
      username: `user_${userId}`,
      email: `${userId}@hoopmaster.local`,
      password: 'auto_generated_' + userId,
      name: `Player ${userId.slice(0, 8)}`,
      tone: 'neutral'
    });
    console.log(`[PlanningChat] Anonymous user created: ${userId}`);
  }

  await addMessage(userId, 'planning', 'user', userText, normalizedSessionId);
  console.log(`[PlanningChat] Saved user message userId=${userId} sessionId=${normalizedSessionId}`);

  if (isConfirmIntent(userText)) {
    console.log(`[PlanningChat] Confirm intent detected userId=${userId}`);
    const latestDraft = await trainingPlanService.getLatestDraftPlan(userId);
    if (latestDraft) {
      console.log(`[PlanningChat] Found latest draft planId=${latestDraft._id} userId=${userId}`);
      const result = await confirmPlanningPlan(userId, latestDraft._id);
      await addMessage(userId, 'planning', 'assistant', result.reply, normalizedSessionId);
      const ttsResult = await synthesizeSpeech(result.reply, user.tone || 'neutral');
      console.log(`[PlanningChat] Confirm flow completed userId=${userId} planId=${latestDraft._id} elapsedMs=${Date.now() - startedAt}`);
      return {
        ...result,
        audioBase64: ttsResult.audioBase64
      };
    }
    console.log(`[PlanningChat] Confirm intent but no draft found userId=${userId}`);
  }

  const mergedProfile = await buildSessionProfile(userId, normalizedSessionId, userText);
  console.log(`[PlanningChat] Built session profile userId=${userId} sessionId=${normalizedSessionId}`);

  const missingFields = getMissingFields(mergedProfile);
  const clarifications = getProfileClarifications(mergedProfile);
  if (missingFields.length > 0 || clarifications.length > 0) {
    const requiredItems = [...missingFields, ...clarifications];
    console.log(`[PlanningChat] Missing profile fields userId=${userId} fields=${requiredItems.join(',')}`);
    const reply = await buildQuestionWithLlm(mergedProfile, missingFields, clarifications, userText);
    await addMessage(userId, 'planning', 'assistant', reply, normalizedSessionId);
    const ttsResult = await synthesizeSpeech(reply, user.tone || 'neutral');
    console.log(`[PlanningChat] Question response sent userId=${userId} elapsedMs=${Date.now() - startedAt}`);
    return {
      type: 'question',
      reply,
      audioBase64: ttsResult.audioBase64,
      collectedProfile: mergedProfile,
      missingFields: requiredItems,
      planDraft: null
    };
  }

  let planPayload;
  try {
    console.log(`[PlanningChat] LLM invocation status=START userId=${userId}`);
    planPayload = await buildPlanPayloadWithLlm(userId, mergedProfile);
    console.log(`[PlanningChat] LLM invocation status=SUCCESS userId=${userId} exercises=${planPayload.exercises.length}`);
  } catch (llmError) {
    console.error(`[PlanningChat] LLM invocation status=FAILED userId=${userId}:`, llmError.message);
    planPayload = buildPlanPayload(userId, mergedProfile);
    console.log(`[PlanningChat] Fallback rule-based plan generated userId=${userId} exercises=${planPayload.exercises.length}`);
  }

  console.log(`[PlanningChat] Creating draft plan userId=${userId} exercises=${planPayload.exercises.length}`);
  const plan = await trainingPlanService.createPlan(userId, planPayload);
  console.log(`[PlanningChat] Draft plan created userId=${userId} planId=${plan._id} status=${plan.status}`);
  const reply = `I created a specific draft plan: "${plan.title}". If it looks good, confirm to save it as your active plan.`;
  await addMessage(userId, 'planning', 'assistant', reply, normalizedSessionId);
  const ttsResult = await synthesizeSpeech(reply, user.tone || 'neutral');
  console.log(`[PlanningChat] Draft response sent userId=${userId} elapsedMs=${Date.now() - startedAt}`);

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
  console.log(`[PlanningChat] Activating plan userId=${userId} planId=${planId}`);
  const plan = await trainingPlanService.activatePlan(userId, planId);
  console.log(`[PlanningChat] Plan activated userId=${userId} planId=${planId} status=${plan.status}`);
  return {
    type: 'saved_plan',
    reply: 'Your personalized plan has been saved and set as active.',
    plan
  };
}

async function getPlanningHistory(userId, limit = 10, sessionId = 'default') {
  return getHistory(userId, 'planning', limit, safeNormalizeSessionId(sessionId));
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
