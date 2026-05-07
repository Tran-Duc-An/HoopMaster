const IDX = {
  leftShoulder: 11,
  rightShoulder: 12,
  leftElbow: 13,
  rightElbow: 14,
  leftWrist: 15,
  rightWrist: 16,
  leftHip: 23,
  rightHip: 24
};

const DEFAULTS = {
  visibilityThreshold: 0.5,
  wristShoulderTolerance: 0.15,
  setHoldMs: parseInt(process.env.SHOT_SET_HOLD_MS, 10) || 900,
  releaseRecentMs: parseInt(process.env.SHOT_RELEASE_RECENT_MS, 10) || 3500,
  releaseWristVelocityY: Number(process.env.SHOT_RELEASE_WRIST_VELOCITY_Y || 0.035),
  releaseExtensionDelta: Number(process.env.SHOT_RELEASE_EXTENSION_DELTA || 0.045),
  minFrameBuffer: parseInt(process.env.SHOT_MIN_FRAME_BUFFER, 10) || 3
};

function createShotState(options = {}) {
  return {
    phase: 'not_ready',
    enteredPhaseAt: 0,
    lastReleaseAt: null,
    metrics: {},
    reason: 'initial',
    options: { ...DEFAULTS, ...options }
  };
}

function updateShotState(previousState = createShotState(), landmarks, timestamp = Date.now(), options = {}) {
  const config = { ...DEFAULTS, ...(previousState.options || {}), ...options };
  const previousPhase = previousState.phase || 'not_ready';
  const inPose = isInShootingPose(landmarks, config);
  const release = detectReleaseMotion(previousState.previousLandmarks, landmarks, config);

  let phase = 'not_ready';
  let reason = 'not_in_shooting_pose';
  let lastReleaseAt = previousState.lastReleaseAt || null;

  if (release.released && previousPhase === 'set') {
    phase = 'released';
    reason = release.reason;
    lastReleaseAt = timestamp;
  } else if (lastReleaseAt && timestamp - lastReleaseAt <= config.releaseRecentMs) {
    phase = 'released';
    reason = 'release_recent';
  } else if (inPose.ready) {
    const preparedAt = ['preparing', 'set', 'released'].includes(previousPhase)
      ? previousState.enteredPhaseAt
      : timestamp;
    const heldLongEnough = timestamp - preparedAt >= config.setHoldMs;
    phase = heldLongEnough ? 'set' : 'preparing';
    reason = heldLongEnough ? 'pose_set' : 'pose_preparing';
  }

  const enteredPhaseAt = phase === previousPhase ? previousState.enteredPhaseAt : timestamp;

  return {
    phase,
    enteredPhaseAt,
    lastReleaseAt,
    lastFormReadyAt: previousState.lastFormReadyAt || null,
    metrics: {
      ...inPose.metrics,
      ...release.metrics
    },
    reason,
    options: config,
    previousLandmarks: landmarks
  };
}

function isInShootingPose(landmarks, options = {}) {
  const config = { ...DEFAULTS, ...options };
  if (!Array.isArray(landmarks)) {
    return { ready: false, side: null, metrics: {}, reason: 'missing_landmarks' };
  }

  const right = getArmReadiness(landmarks, 'right', config);
  const left = getArmReadiness(landmarks, 'left', config);
  const selected = right.ready ? right : left;

  if (!selected.ready) {
    return {
      ready: false,
      side: null,
      metrics: right.metrics || left.metrics || {},
      reason: right.reason || left.reason || 'arm_not_ready'
    };
  }

  return selected;
}

function isShotSet(landmarks, previousState = {}, options = {}) {
  const timestamp = options.timestamp || Date.now();
  const state = updateShotState(previousState, landmarks, timestamp, options);
  return state.phase === 'set' || state.phase === 'released';
}

function detectReleaseMotion(previousLandmarks, currentLandmarks, options = {}) {
  const config = { ...DEFAULTS, ...options };
  if (!Array.isArray(previousLandmarks) || !Array.isArray(currentLandmarks)) {
    return { released: false, reason: 'missing_previous_frame', metrics: {} };
  }

  const sides = ['right', 'left'].map((side) => {
    const previous = getArmGeometry(previousLandmarks, side, config);
    const current = getArmGeometry(currentLandmarks, side, config);
    if (!previous.valid || !current.valid) {
      return { side, released: false, reason: 'invalid_points', metrics: {} };
    }

    const wristVelocityY = previous.wrist.y - current.wrist.y;
    const armExtensionDelta = current.armLength - previous.armLength;
    const released = wristVelocityY >= config.releaseWristVelocityY
      || armExtensionDelta >= config.releaseExtensionDelta;

    return {
      side,
      released,
      reason: released ? 'release_motion_detected' : 'no_release_motion',
      metrics: { wristVelocityY, armExtensionDelta }
    };
  });

  return sides.find((side) => side.released) || sides[0];
}

function shouldAllowPositiveRealtimeFeedback(shotState, evalResult, timestamp = Date.now(), options = {}) {
  const config = { ...DEFAULTS, ...options };
  if (!allPerfect(evalResult)) return false;
  if (shotState?.phase !== 'set') return false;
  if (shotState?.lastFormReadyAt && timestamp - shotState.lastFormReadyAt < config.releaseRecentMs) return false;
  return true;
}

function shouldRunPostShotAnalysis(session, timestamp = Date.now(), options = {}) {
  const config = { ...DEFAULTS, ...options };
  const shotState = session?.shotState;
  const frameCount = session?.frameBuffer?.length || 0;
  if (frameCount < config.minFrameBuffer) return false;
  if (shotState?.phase === 'set') return true;
  if (shotState?.lastReleaseAt && timestamp - shotState.lastReleaseAt <= config.releaseRecentMs) return true;
  return false;
}

function getArmReadiness(landmarks, side, config) {
  const geometry = getArmGeometry(landmarks, side, config);
  if (!geometry.valid) {
    return { ready: false, side, metrics: {}, reason: 'missing_visible_arm_points' };
  }

  const wristAboveElbow = geometry.wrist.y < geometry.elbow.y;
  const elbowAboveHip = !geometry.hip || geometry.elbow.y < geometry.hip.y;
  const wristNearShoulder = geometry.wrist.y <= geometry.shoulder.y + config.wristShoulderTolerance;
  const ready = wristAboveElbow && elbowAboveHip && wristNearShoulder;

  return {
    ready,
    side,
    metrics: {
      wristAboveElbow,
      elbowAboveHip,
      wristNearShoulder,
      armLength: geometry.armLength
    },
    reason: ready ? 'arm_ready' : 'arm_not_ready'
  };
}

function getArmGeometry(landmarks, side, config) {
  const shoulder = landmarks[IDX[`${side}Shoulder`]];
  const elbow = landmarks[IDX[`${side}Elbow`]];
  const wrist = landmarks[IDX[`${side}Wrist`]];
  const hip = landmarks[IDX[`${side}Hip`]];

  if (!isVisible(shoulder, config) || !isVisible(elbow, config) || !isVisible(wrist, config)) {
    return { valid: false };
  }

  return {
    valid: true,
    shoulder,
    elbow,
    wrist,
    hip: isVisible(hip, config) ? hip : null,
    armLength: distance(shoulder, elbow) + distance(elbow, wrist)
  };
}

function isVisible(point, config = DEFAULTS) {
  return point
    && typeof point.x === 'number'
    && typeof point.y === 'number'
    && typeof point.z === 'number'
    && (point.visibility === undefined || point.visibility >= config.visibilityThreshold);
}

function distance(a, b) {
  if (!a || !b) return 0;
  return Math.sqrt((a.x - b.x) ** 2 + (a.y - b.y) ** 2 + (a.z - b.z) ** 2);
}

function allPerfect(evalResult = {}) {
  return ['elbowEval', 'kneeEval', 'shoulderEval'].every((key) => {
    const status = evalResult[key]?.status || 'perfect';
    return status === 'perfect';
  });
}

module.exports = {
  createShotState,
  updateShotState,
  isInShootingPose,
  isShotSet,
  detectReleaseMotion,
  shouldAllowPositiveRealtimeFeedback,
  shouldRunPostShotAnalysis
};
