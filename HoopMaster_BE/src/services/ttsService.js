/**
 * Text-to-Speech Service
 * Convert text to SSML with pitch/rate/volume adjustment by intent
 * Generate base64 audio (mock or real API)
 */

const { TTS_INTENTS } = require('../models/ruleModel');

/**
 * Convert text to SSML (Speech Synthesis Markup Language)
 * SSML allows voice adjustment: pitch, rate, volume
 *
 * @param {string} text - Text to speak
 * @param {string} intent - Intent from ruleModel ('up', 'down', 'focus', 'neutral')
 * @param {string} lang - Language (default: 'en-US')
 * @returns {string} SSML markup
 */
function textToSSML(text, intent = 'neutral', lang = 'en-US') {
  if (!text || typeof text !== 'string') {
    console.warn('[TTS] Invalid text input');
    return '';
  }

  // Get modifiers from TTS_INTENTS
  const intentConfig = TTS_INTENTS[intent] || TTS_INTENTS.neutral;
  const { pitch, rate, volume } = intentConfig.ssmlModifiers;

  // Từ khóa nhấn mạnh và mức pitch riêng
  const highlightKeywords = [
    { word: /cao hơn|vươn cao|higher/gi, pitch: '+12%' },
    { word: /thấp hơn|hạ thấp|lower/gi, pitch: '-12%' },
    { word: /nhanh|nhanh lên|tăng tốc|faster|fast|nhanh chóng/gi, pitch: '+10%' },
    { word: /chậm|chậm lại|giảm tốc|slower|slow|từ từ/gi, pitch: '-10%' },
    { word: /mạnh|mạnh hơn|mạnh mẽ|power|strong|stronger/gi, pitch: '+8%' },
    { word: /nhẹ|nhẹ nhàng|nhẹ hơn|soft|softer/gi, pitch: '-8%' },
    { word: /focus|tập trung/gi, pitch: '+6%' },
    { word: /relax|thả lỏng/gi, pitch: '-6%' }
  ];

  let processedText = text;
  if (intent === 'strict' || intent === 'cheerful') {
    // Chỉ nhấn mạnh khi strict hoặc cheerful
    highlightKeywords.forEach(({ word, pitch }) => {
      processedText = processedText.replace(word, (match) => `<prosody pitch="${pitch}">${escapeXML(match)}</prosody>`);
    });
  }

  // Escape XML special characters ngoài các thẻ prosody
  // Để không escape các thẻ vừa thêm, tách từng phần
  function escapeExceptProsody(str) {
    return str.replace(/(<prosody[^>]*>.*?<\/prosody>)/gi, (m) => `@@@${Buffer.from(m).toString('base64')}@@@`)
      .split('@@@').map(part => {
        if (!part) return '';
        if (/^[A-Za-z0-9+/=]+$/.test(part)) return Buffer.from(part, 'base64').toString();
        return escapeXML(part);
      }).join('');
  }
  const escapedText = escapeExceptProsody(processedText);

  // Create SSML with prosody tags
  const ssml = `
<speak version="1.0" xmlns="http://www.w3.org/2001/10/synthesis" xml:lang="${lang}">
  <prosody pitch="${pitch}" rate="${rate}" volume="${volume}">
    ${escapedText}
  </prosody>
</speak>`.trim();

  return ssml;
}

/**
 * Escape XML special characters to avoid SSML errors
 * @param {string} text
 * @returns {string}
 */
function escapeXML(text) {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;');
}

/**
 * Generate Base64 Audio from SSML
 * Provider: mock | google | elevenlabs
 *
 * @param {string} ssml - SSML markup
 * @param {string} provider - 'mock' | 'google' | 'elevenlabs'
 * @returns {Promise<string>} Base64 encoded audio data
 */
async function _generateAudioBase64(ssml, provider = process.env.TTS_PROVIDER || 'mock') {
  try {
    switch (provider) {
      case 'google':
        return await generateGoogleTTS(ssml);
      
      case 'elevenlabs':
        return await generateElevenLabsTTS(ssml);
      
      case 'mock':
      default:
        return generateMockAudioBase64(ssml);
    }
  } catch (error) {
    console.error('[TTS] Error generating audio:', error.message);
    // Fallback to mock if API fails
    return generateMockAudioBase64(ssml);
  }
  }

  // Limit 1 request/second to ElevenLabs (or real provider)
  const Bottleneck = require('bottleneck');
  const limiter = new Bottleneck({
    minTime: 1000 // 1 request per 1000ms
  });

  // Wrap original function with limiter
  const generateAudioBase64 = limiter.wrap(_generateAudioBase64);


/**
 * Mock Audio Generator - Return fake Base64 string
 * In production, replace with real API
 *
 * @param {string} ssml
 * @returns {string} Base64 string
 */
function generateMockAudioBase64(ssml) {
  // Create a fake base64 string (WAV header + silence)
  // In reality, this should be real audio from TTS engine
  
  const mockAudioData = {
    format: 'audio/wav',
    sampleRate: 16000,
    channels: 1,
    duration: 2000, // 2 seconds
    ssml: ssml
  };

  // Mô phỏng WAV file header (44 bytes) + silent audio
  const header = 'UklGRiQAAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQAAAAA=';
  
  return `data:audio/wav;base64,${header}`;
}

/**
 * Google Cloud Text-to-Speech API
 * Docs: https://cloud.google.com/text-to-speech
 *
 * @param {string} ssml
 * @returns {Promise<string>}
 */
async function generateGoogleTTS(ssml) {
  const axios = require('axios');
  
  const API_KEY = process.env.TTS_API_KEY;
  if (!API_KEY) {
    throw new Error('Google TTS API key not configured');
  }

  const endpoint = 'https://texttospeech.googleapis.com/v1/text:synthesize';

  const requestBody = {
    input: { ssml },
    voice: {
      languageCode: 'en-US',
      name: 'en-US-Standard-B', // Standard US English male
      ssmlGender: 'MALE'
    },
    audioConfig: {
      audioEncoding: 'MP3',
      pitch: 0,
      speakingRate: 1.0
    }
  };

  try {
    const response = await axios.post(`${endpoint}?key=${API_KEY}`, requestBody, {
      timeout: 5000,
      headers: { 'Content-Type': 'application/json' }
    });

    // Google returns base64 in audioContent field
    const audioContent = response.data.audioContent;
    return `data:audio/mp3;base64,${audioContent}`;
  } catch (error) {
    console.error('[TTS] Google API error:', error.response?.data || error.message);
    throw error;
  }
}

/**
 * ElevenLabs Text-to-Speech API
 * Docs: https://elevenlabs.io/docs
 *
 * @param {string} ssml
 * @returns {Promise<string>}
 */
async function generateElevenLabsTTS(ssml) {
  const axios = require('axios');
  
  const API_KEY = process.env.TTS_API_KEY;
  if (!API_KEY) {
    throw new Error('ElevenLabs API key not configured');
  }

  // ElevenLabs does not support SSML directly, need to extract text
  const text = ssml.replace(/<[^>]*>/g, ''); // Remove XML tags
  
  const voiceId = 'ErXwobaYiN019PkySvjV'; // Antoni voice (default English)
  const endpoint = `https://api.elevenlabs.io/v1/text-to-speech/${voiceId}`;

  const requestBody = {
    text: text,
    model_id: 'eleven_multilingual_v2',
    voice_settings: {
      stability: 0.5,
      similarity_boost: 0.75
    }
  };

  try {
    const response = await axios.post(endpoint, requestBody, {
      headers: {
        'Accept': 'audio/mpeg',
        'xi-api-key': API_KEY,
        'Content-Type': 'application/json'
      },
      responseType: 'arraybuffer',
      timeout: 10000
    });

    // Convert arraybuffer to base64
    const base64Audio = Buffer.from(response.data).toString('base64');
    return `data:audio/mpeg;base64,${base64Audio}`;
  } catch (error) {
    console.error('[TTS] ElevenLabs API error:', error.response?.data || error.message);
    throw error;
  }
}

/**
 * High-level function: Convert text to audio ready to send to client
 *
 * @param {string} text - Text to speak
 * @param {string} intent - Intent from rules ('up', 'down', 'focus')
 * @returns {Promise<object>} { audioBase64: string, ssml: string, metadata: object }
 */
async function synthesizeSpeech(text, intent = 'neutral') {
  const startTime = Date.now();

  try {
    // Step 1: Convert text to SSML
    const ssml = textToSSML(text, intent);

    // Step 2: Generate audio from SSML
    const audioBase64 = await generateAudioBase64(ssml);

    const duration = Date.now() - startTime;

    return {
      success: true,
      audioBase64,
      ssml,
      metadata: {
        text,
        intent,
        provider: process.env.TTS_PROVIDER || 'mock',
        generationTime: duration,
        timestamp: new Date().toISOString()
      }
    };
  } catch (error) {
    console.error('[TTS] Synthesis failed:', error.message);
    
    // Return mock audio if error
    return {
      success: false,
      audioBase64: generateMockAudioBase64(textToSSML(text, intent)),
      ssml: textToSSML(text, intent),
      metadata: {
        text,
        intent,
        provider: 'mock_fallback',
        error: error.message,
        timestamp: new Date().toISOString()
      }
    };
  }
}

/**
 * Batch synthesize multiple sentences in parallel
 * @param {array} requests - Array of {text, intent}
 * @returns {Promise<array>} Array of synthesis results
 */
async function batchSynthesize(requests) {
  if (!Array.isArray(requests) || requests.length === 0) {
    return [];
  }

  try {
    const promises = requests.map(req => 
      synthesizeSpeech(req.text, req.intent)
    );

    return await Promise.all(promises);
  } catch (error) {
    console.error('[TTS] Batch synthesis error:', error.message);
    return [];
  }
}

/**
 * Validate TTS configuration
 * @returns {object} { valid: boolean, errors: array }
 */
function validateConfig() {
  const errors = [];
  const provider = process.env.TTS_PROVIDER;

  if (!provider) {
    errors.push('TTS_PROVIDER not set in environment variables');
  }

  if (provider === 'google' || provider === 'elevenlabs') {
    if (!process.env.TTS_API_KEY) {
      errors.push(`TTS_API_KEY required for provider: ${provider}`);
    }
  }

  // Log actual value of environment variables
  errors.push(`[DEBUG] TTS_PROVIDER: ${process.env.TTS_PROVIDER || '[NOT SET]'}`);
  errors.push(`[DEBUG] TTS_API_KEY: ${process.env.TTS_API_KEY ? '[SET]' : '[NOT SET]'}`);

  return {
    valid: errors.length === 0,
    errors,
    config: {
      provider: provider || 'mock',
      hasApiKey: !!process.env.TTS_API_KEY
    }
  };
}

module.exports = {
  textToSSML,
  generateAudioBase64,
  synthesizeSpeech,
  batchSynthesize,
  validateConfig,
  // Utility exports
  escapeXML,
  generateMockAudioBase64
};