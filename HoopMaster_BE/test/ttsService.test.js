const {
  buildElevenLabsRequest,
  buildElevenLabsText,
  emphasizeWords,
  extractEmphasisWords
} = require('../src/services/ttsService');

describe('ttsService emphasis extraction', () => {
  it('does not emphasize neutral coaching text', () => {
    expect(extractEmphasisWords('Raise elbow higher and speed up!', 'neutral')).toEqual([]);
  });

  it('detects directional coaching keywords for cheerful local TTS prosody', () => {
    expect(extractEmphasisWords('Raise elbow higher and speed up!', 'cheerful')).toEqual(
      expect.arrayContaining(['higher', 'up'])
    );
  });

  it('detects directional coaching keywords for strict local TTS prosody', () => {
    expect(extractEmphasisWords('Lower your stance and focus.', 'strict')).toEqual(
      expect.arrayContaining(['lower', 'focus'])
    );
  });

  it('returns an empty list when there are no emphasis keywords in an emphatic tone', () => {
    expect(extractEmphasisWords('Keep your form steady.', 'strict')).toEqual([]);
  });
});

describe('ttsService ElevenLabs request mapping', () => {
  const originalEnv = process.env;

  beforeEach(() => {
    process.env = { ...originalEnv };
  });

  afterAll(() => {
    process.env = originalEnv;
  });

  it('emphasizes requested words without changing unrelated text', () => {
    expect(emphasizeWords('Raise elbow higher and speed up', ['higher', 'up'])).toBe(
      'Raise elbow HIGHER and speed UP'
    );
  });

  it('builds ElevenLabs text with intent punctuation and emphasized words', () => {
    expect(
      buildElevenLabsText({
        text: 'Raise elbow higher and speed up',
        intent: 'cheerful',
        emphasisWords: ['higher', 'up']
      })
    ).toBe('Raise elbow HIGHER and speed UP!');
  });

  it('maps local TTS-style options onto ElevenLabs API fields', () => {
    process.env.ELEVENLABS_VOICE_ID = 'default-voice';
    process.env.ELEVENLABS_MODEL = 'eleven_flash_v2_5';
    process.env.ELEVENLABS_STABILITY = '0.4';
    process.env.ELEVENLABS_SIMILARITY_BOOST = '0.8';

    const request = buildElevenLabsRequest({
      text: 'Lower your stance and focus',
      intent: 'strict',
      emphasisWords: ['lower', 'focus'],
      voice: 'coach-voice',
      language: 'en-US',
      engine: 'eleven_v3',
      format: 'mp3',
      rate: 1.15,
      volume: 1.2
    });

    expect(request).toMatchObject({
      voiceId: 'coach-voice',
      outputFormat: 'mp3_44100_128',
      mediaType: 'audio/mpeg',
      body: {
        text: 'LOWER your stance and FOCUS.',
        model_id: 'eleven_v3',
        language_code: 'en',
        voice_settings: {
          stability: 0.4,
          similarity_boost: 0.8,
          speed: 1.15,
          use_speaker_boost: true
        }
      }
    });
  });
});
