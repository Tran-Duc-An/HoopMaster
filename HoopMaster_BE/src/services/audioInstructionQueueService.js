const ttsService = require('./ttsService');

const queues = new Map();

const DEFAULTS = {
  minGapMs: parseInt(process.env.AUDIO_QUEUE_MIN_GAP_MS, 10) || 350,
  minDurationMs: parseInt(process.env.AUDIO_QUEUE_MIN_DURATION_MS, 10) || 1200,
  charMs: parseInt(process.env.AUDIO_QUEUE_CHAR_MS, 10) || 75,
  baseMs: parseInt(process.env.AUDIO_QUEUE_BASE_MS, 10) || 900,
  maxQueueSize: parseInt(process.env.AUDIO_QUEUE_MAX_SIZE, 10) || 8
};

function createAudioQueueState(options = {}) {
  return {
    items: [],
    processing: false,
    currentDedupeKey: null,
    lastEmitAt: 0,
    clearedAt: 0,
    options: { ...DEFAULTS, ...options }
  };
}

function getAudioQueueState(socketId) {
  if (!queues.has(socketId)) {
    queues.set(socketId, createAudioQueueState());
  }
  return queues.get(socketId);
}

function resetAudioQueue(socketId) {
  const state = createAudioQueueState();
  queues.set(socketId, state);
  return state;
}

function clearAudioQueue(socketId, reason = 'manual_clear') {
  const state = getAudioQueueState(socketId);
  state.items = [];
  state.currentDedupeKey = null;
  state.clearedAt = Date.now();
  state.clearReason = reason;
  return state;
}

async function enqueueAudioInstruction(socketId, instruction, emitCallback, options = {}) {
  if (!socketId || !instruction?.text || typeof emitCallback !== 'function') return null;

  const state = getAudioQueueState(socketId);
  state.options = { ...state.options, ...options };
  state.emitCallback = emitCallback;

  const item = normalizeInstruction(instruction);

  if (item.priority === 'high' || options.clearBeforeEnqueue) {
    state.items = state.items.filter((queued) => queued.priority === 'high');
  }

  if (item.dedupeKey && hasDuplicate(state, item.dedupeKey)) {
    return state;
  }

  if (state.items.length >= state.options.maxQueueSize) {
    const firstLowPriority = state.items.findIndex((queued) => queued.priority !== 'high');
    if (firstLowPriority >= 0) {
      state.items.splice(firstLowPriority, 1);
    } else {
      state.items.shift();
    }
  }

  state.items.push(item);

  if (!state.processing) {
    processAudioQueue(socketId, emitCallback).catch((error) => {
      console.error('[AudioQueue] Failed to process queue:', error.message);
      const latest = getAudioQueueState(socketId);
      latest.processing = false;
    });
  }

  return state;
}

async function processAudioQueue(socketId, emitCallback) {
  const state = getAudioQueueState(socketId);
  if (state.processing) return state;

  state.processing = true;

  while (state.items.length > 0) {
    const item = state.items.shift();
    state.currentDedupeKey = item.dedupeKey || null;

    const startedAt = Date.now();
    const ttsResult = await ttsService.synthesizeSpeech(item.text, item.tone);
    const payload = buildAudioPayload(item, ttsResult, startedAt);
    const targetEmit = typeof state.emitCallback === 'function' ? state.emitCallback : emitCallback;

    targetEmit(item.event, payload);
    state.lastEmitAt = Date.now();

    const durationMs = estimateAudioDurationMs(item.text, ttsResult.audioBase64, state.options);
    await delay(durationMs + state.options.minGapMs);
  }

  state.processing = false;
  state.currentDedupeKey = null;
  return state;
}

function estimateAudioDurationMs(text, audioBase64 = '', options = {}) {
  const config = { ...DEFAULTS, ...options };
  const byText = String(text || '').length * config.charMs + config.baseMs;
  const byAudio = estimateFromAudioSize(audioBase64);
  return Math.max(config.minDurationMs, byText, byAudio);
}

function buildAudioPayload(instruction, ttsResult, now = Date.now()) {
  const metadata = {
    ...(instruction.metadata || {}),
    tone: instruction.tone,
    emphasisWords: ttsResult?.metadata?.emphasisWords || [],
    audioQueued: true,
    timestamp: instruction.metadata?.timestamp || now
  };

  const payload = {
    type: instruction.type,
    text: instruction.text,
    audioBase64: ttsResult?.audioBase64 || '',
    metadata
  };

  if (instruction.angles) payload.angles = instruction.angles;
  if (instruction.stats) payload.stats = instruction.stats;
  if (instruction.exerciseId !== undefined) payload.exerciseId = instruction.exerciseId;
  if (instruction.timestamp) payload.timestamp = instruction.timestamp;

  return payload;
}

function normalizeInstruction(instruction) {
  return {
    event: instruction.event || 'audio_feedback',
    type: instruction.type || 'feedback',
    text: instruction.text,
    tone: normalizeTone(instruction.tone),
    angles: instruction.angles,
    stats: instruction.stats,
    exerciseId: instruction.exerciseId,
    priority: instruction.priority || 'normal',
    dedupeKey: instruction.dedupeKey || `${instruction.type || 'feedback'}:${instruction.text}`,
    metadata: instruction.metadata || {},
    timestamp: instruction.timestamp
  };
}

function normalizeTone(tone) {
  return ['strict', 'cheerful', 'neutral'].includes(tone) ? tone : 'neutral';
}

function hasDuplicate(state, dedupeKey) {
  if (state.currentDedupeKey === dedupeKey) return true;
  return state.items.some((item) => item.dedupeKey === dedupeKey);
}

function estimateFromAudioSize(audioBase64 = '') {
  if (!audioBase64 || typeof audioBase64 !== 'string') return 0;
  const raw = audioBase64.includes(',') ? audioBase64.split(',').pop() : audioBase64;
  const byteLength = Math.floor(raw.length * 0.75);
  if (byteLength < 1000) return 0;
  return Math.min(10000, Math.max(1200, Math.floor(byteLength / 16)));
}

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

module.exports = {
  createAudioQueueState,
  getAudioQueueState,
  resetAudioQueue,
  clearAudioQueue,
  enqueueAudioInstruction,
  processAudioQueue,
  estimateAudioDurationMs,
  buildAudioPayload
};
