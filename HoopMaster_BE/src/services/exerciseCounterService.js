const exerciseService = require('./exerciseService');
const { calculateAngle3D } = require('./poseService');

const LANDMARKS = {
  nose: 0,
  leftEar: 7,
  rightEar: 8,
  leftShoulder: 11,
  rightShoulder: 12,
  leftElbow: 13,
  rightElbow: 14,
  leftWrist: 15,
  rightWrist: 16,
  leftHip: 23,
  rightHip: 24,
  leftKnee: 25,
  rightKnee: 26,
  leftAnkle: 27,
  rightAnkle: 28,
  leftHeel: 29,
  rightHeel: 30,
  leftFootIndex: 31,
  rightFootIndex: 32
};

const JOINT_POINTS = {
  elbow: [
    ['leftShoulder', 'leftElbow', 'leftWrist'],
    ['rightShoulder', 'rightElbow', 'rightWrist']
  ],
  knee: [
    ['leftHip', 'leftKnee', 'leftAnkle'],
    ['rightHip', 'rightKnee', 'rightAnkle']
  ],
  hip: [
    ['leftShoulder', 'leftHip', 'leftKnee'],
    ['rightShoulder', 'rightHip', 'rightKnee']
  ],
  ankle: [
    ['leftKnee', 'leftAnkle', 'leftFootIndex'],
    ['rightKnee', 'rightAnkle', 'rightFootIndex']
  ]
};

function normalizeCoachTone(tone) {
  return ['strict', 'cheerful', 'neutral'].includes(tone) ? tone : 'neutral';
}

function createExerciseRuntime(exerciseId, options = {}) {
  const exercise = exerciseService.getExerciseById(Number(exerciseId));
  if (!exercise) throw new Error('Exercise not found');

  const targetSets = clampPositiveInteger(options.sets, exercise.target?.sets || 1);
  const targetReps = clampPositiveInteger(options.reps, exercise.target?.reps || exercise.count || 1);
  const restSeconds = clampPositiveInteger(options.restSeconds, exercise.target?.restSeconds || 30);
  const now = Date.now();

  return {
    active: true,
    mode: 'exercise_counter',
    exerciseId: exercise.id,
    exercise,
    targetSets,
    targetReps,
    restSeconds,
    coachTone: normalizeCoachTone(options.tone || options.coachTone),
    currentSet: 1,
    currentRep: 0,
    phase: 'setup',
    phaseIndex: 0,
    phaseStartedAt: now,
    startedAt: now,
    completed: false,
    hasReachedBottom: false,
    lastAngle: null,
    lastCueKey: null,
    lastProgressAt: 0,
    nextCue: buildCue('setup', exercise.voiceCues?.setup || exercise.voiceCues?.intro || 'Get ready.', {
      exerciseId: exercise.id
    }),
    // Rest tracking
    restStartedAt: null,
    restRemainingMs: 0
  };
}

function processExerciseFrame(runtime, landmarks, now = Date.now()) {
  if (!runtime?.active || runtime.completed) {
    return { runtime, progress: buildProgress(runtime) };
  }

  // Handle rest phase - freeze counting during rest
  if (runtime.phase === 'rest') {
    return processRestPhase(runtime, now);
  }

  const exercise = runtime.exercise;
  const trackingType = exercise.tracking?.type;

  if (trackingType === 'pose_counter') {
    return processPoseCounter(runtime, landmarks, now);
  }

  return processTimedCadence(runtime, now);
}

/**
 * Process rest phase: count down rest time, do NOT process any exercise phases.
 * When rest is over, automatically resume next set.
 */
function processRestPhase(runtime, now) {
  let updated = { ...runtime, nextCue: null };
  
  // Initialize restStartedAt on first rest frame
  if (!updated.restStartedAt) {
    updated.restStartedAt = now;
    updated.restRemainingMs = updated.restSeconds * 1000;
  } else {
    const elapsed = now - updated.restStartedAt;
    updated.restRemainingMs = Math.max(0, (updated.restSeconds * 1000) - elapsed);
  }

  // Rest period over - start next set
  if (updated.restRemainingMs <= 0) {
    updated.phase = 'setup';
    updated.phaseIndex = 0;
    updated.phaseStartedAt = now;
    updated.restStartedAt = null;
    updated.restRemainingMs = 0;
    updated.hasReachedBottom = false;

    // Emit next set start cue
    if (updated.currentRep === 0) {
      const phases = updated.exercise.counting?.phases || [];
      const firstPhase = phases[0];
      if (firstPhase && firstPhase.cue) {
        updated.nextCue = buildCue('phase', firstPhase.cue, {
          exerciseId: updated.exerciseId,
          phase: firstPhase.key
        });
      }
    }
  }

  return {
    runtime: updated,
    progress: buildProgress(updated)
  };
}

function processTimedCadence(runtime, now) {
  const phases = runtime.exercise.counting?.phases || [];
  if (phases.length === 0) {
    return { runtime, progress: buildProgress(runtime) };
  }

  let updated = { ...runtime, nextCue: null };
  let currentPhase = phases[updated.phaseIndex] || phases[0];
  const elapsed = now - updated.phaseStartedAt;

  if (elapsed >= (currentPhase.durationMs || 1000)) {
    updated.phaseIndex = (updated.phaseIndex + 1) % phases.length;
    updated.phaseStartedAt = now;
    currentPhase = phases[updated.phaseIndex];
    updated.phase = currentPhase.key;

    if (currentPhase.countRep) {
      updated = incrementRep(updated, now);
    } else {
      updated.nextCue = buildCue('phase', currentPhase.cue, {
        exerciseId: updated.exerciseId,
        phase: currentPhase.key
      });
    }
  } else {
    updated.phase = currentPhase.key;
  }

  return {
    runtime: updated,
    progress: buildProgress(updated)
  };
}

function processPoseCounter(runtime, landmarks, now) {
  // If in rest phase, don't process pose
  if (runtime.phase === 'rest') {
    return processRestPhase(runtime, now);
  }

  const counter = runtime.exercise.tracking?.counter || {};
  const angle = calculateJointAngle(landmarks, counter.joint);
  let updated = { ...runtime, nextCue: null, lastAngle: angle };

  if (angle === null) {
    updated.phase = 'no_pose';
    return {
      runtime: updated,
      progress: buildProgress(updated, { warning: 'No valid pose detected' })
    };
  }

  if (angle <= counter.downThreshold) {
    const downPhaseKey = findPhaseKey(updated.exercise, ['down', 'left', 'low']) || 'down';
    if (updated.phase !== downPhaseKey) {
      const downPhase = (updated.exercise.counting?.phases || []).find(p => p.key === downPhaseKey);
      if (downPhase?.cue) {
        updated.nextCue = buildCue('phase', downPhase.cue, {
          exerciseId: updated.exerciseId,
          phase: downPhase.key
        });
      }
    }
    updated.phase = downPhaseKey;
    updated.hasReachedBottom = true;
  } else if (angle >= counter.upThreshold) {
    const countPhase = updated.exercise.counting?.countOnPhase || 'top';
    updated.phase = countPhase;
    if (updated.hasReachedBottom) {
      updated = incrementRep(updated, now);
      updated.hasReachedBottom = false;
    }
  }

  return {
    runtime: updated,
    progress: buildProgress(updated)
  };
}

function incrementRep(runtime, now) {
  let updated = { ...runtime };
  updated.currentRep += 1;

  updated.nextCue = buildCue('rep', buildRepCue(updated.exercise, updated.currentRep), {
    exerciseId: updated.exerciseId,
    set: updated.currentSet,
    rep: updated.currentRep
  });

  if (updated.currentRep >= updated.targetReps) {
    if (updated.currentSet >= updated.targetSets) {
      updated.completed = true;
      updated.active = false;
      updated.phase = 'complete';
      updated.nextCue = buildCue('complete', updated.exercise.voiceCues?.complete || `${updated.exercise.name} finished.`, {
        exerciseId: updated.exerciseId,
        set: updated.currentSet,
        rep: updated.currentRep
      });
    } else {
      // Transition to rest phase
      updated.phase = 'rest';
      updated.restStartedAt = now;
      updated.restRemainingMs = updated.restSeconds * 1000;
      updated.nextCue = buildCue(
        'rest',
        `${updated.exercise.voiceCues?.setComplete || 'Set complete.'} Rest for ${updated.restSeconds} seconds.`,
        {
          exerciseId: updated.exerciseId,
          set: updated.currentSet,
          restSeconds: updated.restSeconds
        }
      );
      updated.currentSet += 1;
      updated.currentRep = 0;
      updated.hasReachedBottom = false;
    }
  }

  return updated;
}

function buildProgress(runtime, extra = {}) {
  if (!runtime) return null;
  
  const phases = runtime.exercise?.counting?.phases || [];
  const totalPhases = phases.length;
  const currentPhaseObj = phases[runtime.phaseIndex] || null;
  
  let restRemainingMs = 0;
  if (runtime.phase === 'rest' && runtime.restStartedAt) {
    const restElapsed = Date.now() - runtime.restStartedAt;
    restRemainingMs = Math.max(0, (runtime.restSeconds * 1000) - restElapsed);
  }

  return {
    exerciseId: runtime.exerciseId,
    tone: runtime.coachTone,
    name: runtime.exercise?.name,
    category: runtime.exercise?.category,
    set: runtime.currentSet,
    targetSets: runtime.targetSets,
    reps: runtime.currentRep,
    targetReps: runtime.targetReps,
    phase: runtime.phase,
    phaseIndex: runtime.phaseIndex,
    totalPhases: totalPhases,
    currentPhaseCue: currentPhaseObj?.cue || null,
    completed: runtime.completed,
    angle: runtime.lastAngle,
    restRemainingMs: restRemainingMs,
    restSeconds: runtime.restSeconds,
    timestamp: Date.now(),
    ...extra
  };
}

function calculateJointAngle(landmarks, joint) {
  const configs = JOINT_POINTS[joint];
  if (!Array.isArray(landmarks) || !configs) return null;

  const angles = configs
    .map(([a, b, c]) => calculateAngle3D(
      landmarks[LANDMARKS[a]],
      landmarks[LANDMARKS[b]],
      landmarks[LANDMARKS[c]]
    ))
    .filter(angle => typeof angle === 'number' && !Number.isNaN(angle));

  if (angles.length === 0) return null;
  return Math.round((angles.reduce((sum, angle) => sum + angle, 0) / angles.length) * 100) / 100;
}

function buildRepCue(exercise, rep) {
  const template = exercise.voiceCues?.repTemplate;
  if (template) return template.replace('{rep}', String(rep));
  return `Rep ${rep}.`;
}

function buildCue(type, text, metadata = {}) {
  if (!text) return null;
  return {
    type,
    text,
    metadata: {
      ...metadata,
      timestamp: Date.now()
    }
  };
}

function findPhaseKey(exercise, keys) {
  const phases = exercise.counting?.phases || [];
  return phases.find(phase => keys.includes(phase.key))?.key;
}

function clampPositiveInteger(value, fallback) {
  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed < 1) return fallback;
  return parsed;
}

module.exports = {
  createExerciseRuntime,
  processExerciseFrame,
  calculateJointAngle,
  buildProgress,
  normalizeCoachTone
};