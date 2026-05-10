const axios = require('axios');
const Bottleneck = require('bottleneck');
const { TTS_INTENTS } = require('../models/ruleModel');

const limiter = new Bottleneck({
  minTime: parseInt(process.env.TTS_MIN_INTERVAL_MS, 10) || 1000
});

const EMPHASIS_KEYWORDS = [
  'higher',
  'lower',
  'faster',
  'slower',
  'fast',
  'slow',
  'power',
  'strong',
  'stronger',
  'soft',
  'softer',
  'focus',
  'relax',
  'up',
  'down'
];

function getProvider(provider = process.env.TTS_PROVIDER || 'mock') {
  return provider.toLowerCase();
}

function textToSSML(text, intent = 'neutral', lang = 'en-US') {
  if (!text || typeof text !== 'string') return '';

  const intentConfig = TTS_INTENTS[intent] || TTS_INTENTS.neutral;
  const { pitch, rate, volume } = intentConfig.ssmlModifiers;
  const escapedText = escapeXML(text);

  return `<speak version="1.0" xml:lang="${lang}"><prosody pitch="${pitch}" rate="${rate}" volume="${volume}">${escapedText}</prosody></speak>`;
}

function escapeXML(text) {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;');
}

async function _generateAudioBase64(input, provider = getProvider()) {
  const normalized = normalizeInput(input);

  switch (getProvider(provider)) {
    case 'elevenlabs':
      return generateElevenLabsTTS(normalized);
    case 'google':
      return generateGoogleTTS(normalized.ssml || textToSSML(normalized.text, normalized.intent));
    case 'local':
    case 'kokoro':
      return generateLocalTTS(normalized);
    case 'mock':
    default:
      return generateMockAudioBase64(normalized.text);
  }
}

const generateAudioBase64 = limiter.wrap(_generateAudioBase64);

function normalizeInput(input) {
  if (typeof input === 'string') {
    return {
      text: input.replace(/<[^>]*>/g, ''),
      ssml: input,
      intent: 'neutral',
      emphasisWords: extractEmphasisWords(input, 'neutral')
    };
  }

  if (input && typeof input === 'object') {
    const text = input.text || '';
    const intent = input.intent || 'neutral';
    return {
      text,
      ssml: input.ssml,
      intent,
      emphasisWords: Array.isArray(input.emphasisWords)
        ? input.emphasisWords
        : extractEmphasisWords(text, intent),
      voice: input.voice,
      language: input.language,
      engine: input.engine,
      format: input.format,
      rate: input.rate,
      pitch: input.pitch,
      volume: input.volume
    };
  }

  return { text: '', ssml: '', intent: 'neutral', emphasisWords: [] };
}

function extractEmphasisWords(text = '', intent = 'neutral') {
  if (!['strict', 'cheerful'].includes(intent)) return [];
  if (!text || typeof text !== 'string') return [];
  const lowerText = text.toLowerCase();
  return EMPHASIS_KEYWORDS.filter(keyword => {
    const pattern = new RegExp(`\\b${escapeRegExp(keyword)}\\b`, 'i');
    return pattern.test(lowerText);
  });
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function generateMockAudioBase64(text = '') {
  const header = 'UklGRiQAAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQAAAAA=';
  return `data:audio/wav;base64,${header}`;
}

async function generateLocalTTS(input) {
  const normalized = normalizeInput(input);
  const endpoint = process.env.LOCAL_TTS_URL || 'http://localhost:8000/api/v1/tts';
  const audioFormat = normalized.format || process.env.LOCAL_TTS_FORMAT || 'wav';
  const response = await axios.post(
    endpoint,
    {
      text: normalized.text,
      intent: normalized.intent,
      emphasis_words: normalized.emphasisWords,
      format: audioFormat,
      voice: normalized.voice || process.env.LOCAL_TTS_VOICE || undefined,
      engine: normalized.engine || process.env.LOCAL_TTS_ENGINE || undefined,
      language: normalized.language || process.env.LOCAL_TTS_LANGUAGE || undefined,
      rate: normalized.rate,
      pitch: normalized.pitch,
      volume: normalized.volume
    },
    {
      timeout: parseInt(process.env.LOCAL_TTS_TIMEOUT_MS, 10) || 60000,
      headers: { 'Content-Type': 'application/json' },
      responseType: 'arraybuffer'
    }
  );

  const contentType = response.headers['content-type'] || mediaTypeForFormat(audioFormat);
  if (!response.data || response.data.byteLength === 0) {
    throw new Error('Local TTS response did not include audio bytes');
  }

  return `data:${contentType};base64,${Buffer.from(response.data).toString('base64')}`;
}

function mediaTypeForFormat(format) {
  const normalized = (format || 'wav').toLowerCase();
  if (normalized === 'mp3') return 'audio/mpeg';
  if (normalized === 'ogg') return 'audio/ogg';
  return 'audio/wav';
}

async function generateGoogleTTS(ssml) {
  const API_KEY = process.env.TTS_API_KEY;
  if (!API_KEY) throw new Error('Google TTS API key not configured');

  const response = await axios.post(
    `https://texttospeech.googleapis.com/v1/text:synthesize?key=${API_KEY}`,
    {
      input: { ssml },
      voice: {
        languageCode: process.env.TTS_LANGUAGE || 'en-US',
        name: process.env.GOOGLE_TTS_VOICE || 'en-US-Standard-B',
        ssmlGender: process.env.GOOGLE_TTS_GENDER || 'MALE'
      },
      audioConfig: {
        audioEncoding: 'MP3',
        pitch: 0,
        speakingRate: 1
      }
    },
    {
      timeout: parseInt(process.env.TTS_TIMEOUT_MS, 10) || 10000,
      headers: { 'Content-Type': 'application/json' }
    }
  );

  return `data:audio/mp3;base64,${response.data.audioContent}`;
}

function clampNumber(value, fallback, min, max) {
  const numeric = Number(value);
  if (!Number.isFinite(numeric)) return fallback;
  return Math.min(max, Math.max(min, numeric));
}

function elevenLabsFormatParam(format = process.env.ELEVENLABS_OUTPUT_FORMAT || 'mp3') {
  const normalized = String(format || 'mp3').toLowerCase();
  if (normalized === 'mp3') return 'mp3_44100_128';
  if (normalized === 'pcm') return 'pcm_44100';
  if (normalized === 'ulaw' || normalized === 'mulaw') return 'ulaw_8000';
  if (normalized.includes('_')) return normalized;
  return 'mp3_44100_128';
}

function mediaTypeForElevenLabsFormat(format = process.env.ELEVENLABS_OUTPUT_FORMAT || 'mp3') {
  const normalized = String(format || 'mp3').toLowerCase();
  if (normalized.startsWith('pcm')) return 'audio/pcm';
  if (normalized.startsWith('ulaw') || normalized.startsWith('mulaw')) return 'audio/basic';
  return 'audio/mpeg';
}

function parseRateMultiplier(value, fallback = 1) {
  if (typeof value === 'number') return clampNumber(value, fallback, 0.7, 1.2);
  if (typeof value !== 'string') return fallback;
  return clampNumber(value.replace(/[^\d.-]/g, ''), fallback, 0.7, 1.2);
}

function voiceSettingsForElevenLabs(input) {
  const intentConfig = TTS_INTENTS[input.intent] || TTS_INTENTS.neutral;
  const intentRate = intentConfig?.ssmlModifiers?.rate;
  const rate = parseRateMultiplier(input.rate, parseRateMultiplier(intentRate, 1));
  const volume = clampNumber(input.volume, 1, 0.5, 1.5);
  const styleDefault = input.intent === 'cheerful' || input.intent === 'up' ? 0.35 : 0;

  return {
    stability: clampNumber(process.env.ELEVENLABS_STABILITY, 0.5, 0, 1),
    similarity_boost: clampNumber(process.env.ELEVENLABS_SIMILARITY_BOOST, 0.75, 0, 1),
    style: clampNumber(process.env.ELEVENLABS_STYLE, styleDefault, 0, 1),
    use_speaker_boost: process.env.ELEVENLABS_USE_SPEAKER_BOOST !== 'false' || volume > 1.05,
    speed: rate
  };
}

function buildElevenLabsText(input) {
  const normalized = normalizeInput(input);
  const emphasized = emphasizeWords(normalized.text, normalized.emphasisWords);
  const intent = normalized.intent || 'neutral';

  if (intent === 'strict') return ensureTerminalPunctuation(emphasized, '.');
  if (intent === 'cheerful' || intent === 'up') return ensureTerminalPunctuation(emphasized, '!');
  if (intent === 'down' || intent === 'focus') return ensureTerminalPunctuation(emphasized, '.');
  return emphasized;
}

function emphasizeWords(text = '', emphasisWords = []) {
  const cleaned = [...new Set((emphasisWords || []).map(word => String(word).trim()).filter(Boolean))];
  if (!text || cleaned.length === 0) return text;

  return cleaned.reduce((result, word) => {
    const pattern = new RegExp(`\\b${escapeRegExp(word)}\\b`, 'gi');
    return result.replace(pattern, match => match.toUpperCase());
  }, text);
}

function ensureTerminalPunctuation(text, mark) {
  const trimmed = String(text || '').trim();
  if (!trimmed) return trimmed;
  if (/[.!?]$/.test(trimmed)) return trimmed;
  return `${trimmed}${mark}`;
}

function buildElevenLabsRequest(input) {
  const normalized = normalizeInput(input);
  const voiceId = normalized.voice || process.env.ELEVENLABS_VOICE_ID || 'ErXwobaYiN019PkySvjV';
  const format = normalized.format || process.env.ELEVENLABS_OUTPUT_FORMAT || 'mp3';
  const body = {
    text: buildElevenLabsText(normalized),
    model_id: normalized.engine || process.env.ELEVENLABS_MODEL || 'eleven_multilingual_v2',
    voice_settings: voiceSettingsForElevenLabs(normalized)
  };

  const languageCode = normalized.language || process.env.ELEVENLABS_LANGUAGE;
  if (languageCode) {
    body.language_code = languageCode.split(/[-_]/)[0];
  }

  return {
    voiceId,
    format,
    outputFormat: elevenLabsFormatParam(format),
    mediaType: mediaTypeForElevenLabsFormat(format),
    body
  };
}

async function generateElevenLabsTTS(input) {
  const API_KEY = process.env.TTS_API_KEY;
  if (!API_KEY) throw new Error('ElevenLabs API key not configured');

  const request = buildElevenLabsRequest(input);
  const response = await axios.post(
    `https://api.elevenlabs.io/v1/text-to-speech/${request.voiceId}?output_format=${request.outputFormat}`,
    request.body,
    {
      headers: {
        Accept: request.mediaType,
        'xi-api-key': API_KEY,
        'Content-Type': 'application/json'
      },
      responseType: 'arraybuffer',
      timeout: parseInt(process.env.TTS_TIMEOUT_MS, 10) || 10000
    }
  );

  return `data:${request.mediaType};base64,${Buffer.from(response.data).toString('base64')}`;
}

async function synthesizeSpeech(text, intent = 'neutral') {
  const startTime = Date.now();
  const provider = getProvider();
  const ssml = textToSSML(text, intent);
  const emphasisWords = extractEmphasisWords(text, intent);

  try {
    const audioBase64 = await generateAudioBase64({ text, intent, ssml, emphasisWords }, provider);
    return {
      success: true,
      audioBase64,
      ssml,
      metadata: {
        text,
        intent,
        emphasisWords,
        provider,
        generationTime: Date.now() - startTime,
        timestamp: new Date().toISOString()
      }
    };
  } catch (error) {
    console.error('[TTS] Synthesis failed:', error.message);
    return {
      success: false,
      audioBase64: generateMockAudioBase64(text),
      ssml,
      metadata: {
        text,
        intent,
        emphasisWords,
        provider: 'mock_fallback',
        requestedProvider: provider,
        error: error.message,
        timestamp: new Date().toISOString()
      }
    };
  }
}

async function batchSynthesize(requests) {
  if (!Array.isArray(requests) || requests.length === 0) return [];
  return Promise.all(requests.map(req => synthesizeSpeech(req.text, req.intent)));
}

function validateConfig() {
  const provider = getProvider();
  const errors = [];

  if (!['mock', 'local', 'kokoro', 'google', 'elevenlabs'].includes(provider)) {
    errors.push(`Unsupported TTS_PROVIDER: ${provider}`);
  }

  if (['google', 'elevenlabs'].includes(provider) && !process.env.TTS_API_KEY) {
    errors.push(`TTS_API_KEY required for provider: ${provider}`);
  }

  return {
    valid: errors.length === 0,
    errors,
    config: {
      provider,
      hasApiKey: !!process.env.TTS_API_KEY,
      localUrl: process.env.LOCAL_TTS_URL || 'http://localhost:8000/api/v1/tts',
      localFormat: process.env.LOCAL_TTS_FORMAT || 'wav',
      elevenLabsVoiceId: process.env.ELEVENLABS_VOICE_ID || 'ErXwobaYiN019PkySvjV',
      elevenLabsModel: process.env.ELEVENLABS_MODEL || 'eleven_multilingual_v2',
      elevenLabsFormat: process.env.ELEVENLABS_OUTPUT_FORMAT || 'wav'
    }
  };
}

module.exports = {
  textToSSML,
  generateAudioBase64,
  synthesizeSpeech,
  batchSynthesize,
  validateConfig,
  escapeXML,
  generateMockAudioBase64,
  extractEmphasisWords,
  buildElevenLabsRequest,
  buildElevenLabsText,
  emphasizeWords
};
