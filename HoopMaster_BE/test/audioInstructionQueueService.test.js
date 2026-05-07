jest.mock('../src/services/ttsService', () => ({
  synthesizeSpeech: jest.fn(async (text, intent) => ({
    success: true,
    audioBase64: `data:audio/wav;base64,${Buffer.from(text).toString('base64')}`,
    metadata: { emphasisWords: [], intent }
  }))
}));

const ttsService = require('../src/services/ttsService');
const {
  enqueueAudioInstruction,
  clearAudioQueue,
  resetAudioQueue,
  estimateAudioDurationMs
} = require('../src/services/audioInstructionQueueService');

describe('audioInstructionQueueService', () => {
  beforeEach(() => {
    jest.useFakeTimers();
    jest.clearAllMocks();
    resetAudioQueue('socket-1');
  });

  afterEach(() => {
    clearAudioQueue('socket-1', 'test_done');
    jest.useRealTimers();
  });

  it('estimates a minimum playback duration', () => {
    expect(estimateAudioDurationMs('short', '', { minDurationMs: 1234 })).toBeGreaterThanOrEqual(1234);
  });

  it('emits queued instructions sequentially', async () => {
    const emitted = [];
    const emit = (event, payload) => emitted.push({ event, payload });
    const options = { minDurationMs: 10, minGapMs: 0, charMs: 0, baseMs: 0 };

    await enqueueAudioInstruction('socket-1', { text: 'first', type: 'one' }, emit, options);
    await enqueueAudioInstruction('socket-1', { text: 'second', type: 'two' }, emit, options);

    await Promise.resolve();
    await Promise.resolve();
    expect(emitted.map((item) => item.payload.text)).toEqual(['first']);

    await jest.advanceTimersByTimeAsync(10);
    expect(emitted.map((item) => item.payload.text)).toEqual(['first', 'second']);
  });

  it('dedupes queued instructions by key', async () => {
    const emitted = [];
    const emit = (event, payload) => emitted.push({ event, payload });
    const options = { minDurationMs: 10, minGapMs: 0, charMs: 0, baseMs: 0 };

    await enqueueAudioInstruction('socket-1', { text: 'repeat', type: 'cue', dedupeKey: 'same' }, emit, options);
    await enqueueAudioInstruction('socket-1', { text: 'repeat', type: 'cue', dedupeKey: 'same' }, emit, options);

    await Promise.resolve();
    await jest.advanceTimersByTimeAsync(20);

    expect(ttsService.synthesizeSpeech).toHaveBeenCalledTimes(1);
    expect(emitted).toHaveLength(1);
  });
});
