const { extractEmphasisWords } = require('../src/services/ttsService');

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
