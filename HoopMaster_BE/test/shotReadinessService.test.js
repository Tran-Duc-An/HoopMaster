const {
  createShotState,
  updateShotState,
  isInShootingPose,
  detectReleaseMotion,
  shouldAllowPositiveRealtimeFeedback,
  shouldRunPostShotAnalysis
} = require('../src/services/shotReadinessService');

function makeLandmarks(overrides = {}) {
  const landmarks = Array.from({ length: 33 }, () => ({ x: 0, y: 0.5, z: 0, visibility: 0.99 }));
  landmarks[11] = { x: 0.4, y: 0.45, z: 0, visibility: 0.99 };
  landmarks[13] = { x: 0.45, y: 0.5, z: 0, visibility: 0.99 };
  landmarks[15] = { x: 0.5, y: 0.55, z: 0, visibility: 0.99 };
  landmarks[23] = { x: 0.45, y: 0.75, z: 0, visibility: 0.99 };
  landmarks[12] = { x: 0.6, y: 0.45, z: 0, visibility: 0.99 };
  landmarks[14] = { x: 0.55, y: 0.5, z: 0, visibility: 0.99 };
  landmarks[16] = { x: 0.5, y: 0.55, z: 0, visibility: 0.99 };
  landmarks[24] = { x: 0.55, y: 0.75, z: 0, visibility: 0.99 };

  Object.entries(overrides).forEach(([index, point]) => {
    landmarks[Number(index)] = { ...landmarks[Number(index)], ...point };
  });

  return landmarks;
}

function makeReadyLandmarks(overrides = {}) {
  return makeLandmarks({
    13: { y: 0.44 },
    15: { y: 0.34 },
    14: { y: 0.44 },
    16: { y: 0.34 },
    ...overrides
  });
}

describe('shotReadinessService', () => {
  it('detects shooting pose only when wrist is above elbow near shoulder', () => {
    expect(isInShootingPose(makeLandmarks()).ready).toBe(false);
    expect(isInShootingPose(makeReadyLandmarks()).ready).toBe(true);
  });

  it('moves from preparing to set after hold window', () => {
    let state = createShotState({ setHoldMs: 500 });

    state = updateShotState(state, makeReadyLandmarks(), 1000);
    expect(state.phase).toBe('preparing');

    state = updateShotState(state, makeReadyLandmarks(), 1600);
    expect(state.phase).toBe('set');
  });

  it('detects release motion from upward wrist travel', () => {
    const previous = makeReadyLandmarks({ 15: { y: 0.34 }, 16: { y: 0.34 } });
    const current = makeReadyLandmarks({ 15: { y: 0.25 }, 16: { y: 0.25 } });

    expect(detectReleaseMotion(previous, current).released).toBe(true);
  });

  it('blocks positive realtime feedback before release', () => {
    const evalResult = {
      elbowEval: { status: 'perfect' },
      kneeEval: { status: 'perfect' },
      shoulderEval: { status: 'perfect' }
    };

    expect(shouldAllowPositiveRealtimeFeedback({ phase: 'preparing' }, evalResult, 1000)).toBe(false);
    expect(shouldAllowPositiveRealtimeFeedback({ phase: 'released', lastReleaseAt: 900 }, evalResult, 1000)).toBe(true);
  });

  it('allows post-shot analysis only after set or recent release', () => {
    expect(shouldRunPostShotAnalysis({
      frameBuffer: [1, 2, 3],
      shotState: { phase: 'preparing' }
    }, 1000)).toBe(false);

    expect(shouldRunPostShotAnalysis({
      frameBuffer: [1, 2, 3],
      shotState: { phase: 'set' }
    }, 1000)).toBe(true);

    expect(shouldRunPostShotAnalysis({
      frameBuffer: [1, 2, 3],
      shotState: { phase: 'not_ready', lastReleaseAt: 900 }
    }, 1000)).toBe(true);
  });
});
