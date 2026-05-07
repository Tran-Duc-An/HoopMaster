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
      return generateElevenLabsTTS(normalized.text);
    case 'google':
      return generateGoogleTTS(normalized.ssml || textToSSML(normalized.text, normalized.intent));
    case 'local':
    case 'kokoro':
      return generateLocalTTS(normalized.text, normalized.intent, normalized.emphasisWords);
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
        : extractEmphasisWords(text, intent)
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

async function generateLocalTTS(text, intent = 'neutral', emphasisWords = []) {
  const endpoint = process.env.LOCAL_TTS_URL || 'http://localhost:8000/api/v1/tts';
  const audioFormat = process.env.LOCAL_TTS_FORMAT || 'wav';
  const response = await axios.post(
    endpoint,
    {
      text,
      intent,
      emphasis_words: emphasisWords,
      format: audioFormat,
      voice: process.env.LOCAL_TTS_VOICE || undefined,
      engine: process.env.LOCAL_TTS_ENGINE || undefined,
      language: process.env.LOCAL_TTS_LANGUAGE || undefined
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

async function generateElevenLabsTTS(text) {
  const API_KEY = process.env.TTS_API_KEY;
  if (!API_KEY) throw new Error('ElevenLabs API key not configured');

  const voiceId = process.env.ELEVENLABS_VOICE_ID || 'ErXwobaYiN019PkySvjV';
  const response = await axios.post(
    `https://api.elevenlabs.io/v1/text-to-speech/${voiceId}`,
    {
      text,
      model_id: process.env.ELEVENLABS_MODEL || 'eleven_multilingual_v2',
      voice_settings: {
        stability: Number(process.env.ELEVENLABS_STABILITY || 0.5),
        similarity_boost: Number(process.env.ELEVENLABS_SIMILARITY_BOOST || 0.75)
      }
    },
    {
      headers: {
        Accept: 'audio/mpeg',
        'xi-api-key': API_KEY,
        'Content-Type': 'application/json'
      },
      responseType: 'arraybuffer',
      timeout: parseInt(process.env.TTS_TIMEOUT_MS, 10) || 10000
    }
  );

  return `data:audio/mpeg;base64,${Buffer.from(response.data).toString('base64')}`;
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
      localFormat: process.env.LOCAL_TTS_FORMAT || 'wav'
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
  extractEmphasisWords
};
