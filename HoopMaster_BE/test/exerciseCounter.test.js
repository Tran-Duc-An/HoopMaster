const {
  createExerciseRuntime,
  processExerciseFrame,
  calculateJointAngle
} = require('../src/services/exerciseCounterService');

function makeLandmarksForKneeAngle(angleDeg) {
  const landmarks = Array.from({ length: 33 }, () => ({ x: 0, y: 0, z: 0, visibility: 0.99 }));
  const radians = angleDeg * Math.PI / 180;

  const hip = { x: Math.cos(radians), y: Math.sin(radians), z: 0, visibility: 0.99 };
  const knee = { x: 0, y: 0, z: 0, visibility: 0.99 };
  const ankle = { x: 1, y: 0, z: 0, visibility: 0.99 };

  landmarks[23] = hip;
  landmarks[25] = knee;
  landmarks[27] = ankle;
  landmarks[24] = hip;
  landmarks[26] = knee;
  landmarks[28] = ankle;

  return landmarks;
}

function makeLandmarksForElbowAngle(angleDeg) {
  const landmarks = Array.from({ length: 33 }, () => ({ x: 0, y: 0, z: 0, visibility: 0.99 }));
  const radians = angleDeg * Math.PI / 180;

  const shoulder = { x: Math.cos(radians), y: Math.sin(radians), z: 0, visibility: 0.99 };
  const elbow = { x: 0, y: 0, z: 0, visibility: 0.99 };
  const wrist = { x: 1, y: 0, z: 0, visibility: 0.99 };

  landmarks[11] = shoulder;
  landmarks[13] = elbow;
  landmarks[15] = wrist;
  landmarks[12] = shoulder;
  landmarks[14] = elbow;
  landmarks[16] = wrist;

  return landmarks;
}

describe('exerciseCounterService', () => {
  it('counts one squat rep after moving down then standing tall', () => {
    let runtime = createExerciseRuntime(3, { sets: 1, reps: 2 });

    let result = processExerciseFrame(runtime, makeLandmarksForKneeAngle(100), Date.now());
    runtime = result.runtime;
    expect(runtime.currentRep).toBe(0);
    expect(runtime.phase).toBe('down');

    result = processExerciseFrame(runtime, makeLandmarksForKneeAngle(170), Date.now() + 1000);
    runtime = result.runtime;
    expect(runtime.currentRep).toBe(1);
    expect(result.progress.reps).toBe(1);
    expect(runtime.nextCue.text).toContain('Rep 1');
  });

  it('marks push up complete when target reps are reached', () => {
    let runtime = createExerciseRuntime(4, { sets: 1, reps: 1 });

    runtime = processExerciseFrame(runtime, makeLandmarksForElbowAngle(80), Date.now()).runtime;
    const result = processExerciseFrame(runtime, makeLandmarksForElbowAngle(170), Date.now() + 1000);

    expect(result.runtime.completed).toBe(true);
    expect(result.runtime.active).toBe(false);
    expect(result.progress.completed).toBe(true);
    expect(result.runtime.nextCue.type).toBe('complete');
  });

  it('calculates averaged joint angle from both sides', () => {
    const angle = calculateJointAngle(makeLandmarksForKneeAngle(120), 'knee');
    expect(angle).toBeCloseTo(120, 1);
  });

  it('stores a valid coach tone for exercise audio cues', () => {
    const cheerfulRuntime = createExerciseRuntime(3, { tone: 'cheerful' });
    const fallbackRuntime = createExerciseRuntime(3, { tone: 'cheeful' });

    expect(cheerfulRuntime.coachTone).toBe('cheerful');
    expect(cheerfulRuntime.nextCue.metadata.exerciseId).toBe(3);
    expect(fallbackRuntime.coachTone).toBe('neutral');
  });
});
