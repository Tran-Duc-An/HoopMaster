// Controller for exercise-related endpoints
const exerciseService = require('../services/exerciseService');
const {
  createExerciseRuntime,
  processExerciseFrame
} = require('../services/exerciseCounterService');

const getAllExercises = (req, res) => {
  try {
    const exercises = exerciseService.getAllExercises();
    res.json(exercises);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
};

const getExercisesByCategory = (req, res) => {
  try {
    const { category } = req.params;
    const exercises = exerciseService.getExercisesByCategory(category);
    res.json(exercises);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
};

const getExerciseById = (req, res) => {
  try {
    const { id } = req.params;
    const exercise = exerciseService.getExerciseById(Number(id));
    if (exercise) {
      res.json(exercise);
    } else {
      res.status(404).json({ error: 'Exercise not found' });
    }
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
};

const getExerciseVoiceScript = (req, res) => {
  try {
    const { id } = req.params;
    const script = exerciseService.getExerciseVoiceScript(Number(id), req.query);
    if (script) {
      res.json(script);
    } else {
      res.status(404).json({ error: 'Exercise not found' });
    }
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
};

const getCategories = (req, res) => {
  try {
    res.json({ categories: exerciseService.getCategories() });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
};

const getDefaultPlan = (req, res) => {
  try {
    res.json(exerciseService.getDefaultPlan());
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
};

const simulateCounter = (req, res) => {
  try {
    const { id } = req.params;
    const {
      angles = [],
      sets,
      reps,
      restSeconds,
      intervalMs = 1000
    } = req.body || {};

    const runtime = createExerciseRuntime(Number(id), { sets, reps, restSeconds });
    let currentRuntime = runtime;
    const timeline = [];
    const cues = [];
    const exercise = runtime.exercise;

    const frames = Array.isArray(angles) && angles.length > 0
      ? angles
      : buildTimedFrames(exercise, intervalMs);

    frames.forEach((frame, index) => {
      const angle = typeof frame === 'number' ? frame : frame.angle;
      const at = typeof frame === 'object' && typeof frame.at === 'number'
        ? runtime.startedAt + frame.at
        : runtime.startedAt + (index + 1) * intervalMs;

      const landmarks = typeof angle === 'number'
        ? buildLandmarksForExercise(exercise, angle)
        : null;

      const result = processExerciseFrame(currentRuntime, landmarks, at);
      currentRuntime = result.runtime;
      timeline.push(result.progress);

      if (currentRuntime.nextCue) {
        cues.push(currentRuntime.nextCue);
        currentRuntime.nextCue = null;
      }
    });

    res.json({
      exerciseId: runtime.exerciseId,
      name: exercise.name,
      trackingType: exercise.tracking?.type,
      inputFrames: frames.length,
      finalProgress: timeline[timeline.length - 1] || null,
      cues,
      timeline
    });
  } catch (err) {
    const status = err.message === 'Exercise not found' ? 404 : 400;
    res.status(status).json({ error: err.message });
  }
};

function buildTimedFrames(exercise, intervalMs) {
  const targetReps = exercise.target?.reps || exercise.count || 1;
  const phaseCount = exercise.counting?.phases?.length || 1;
  const frameCount = Math.max(targetReps * phaseCount + 1, 4);
  return Array.from({ length: frameCount }, (_, index) => ({ at: (index + 1) * intervalMs }));
}

function buildLandmarksForExercise(exercise, angle) {
  const joint = exercise.tracking?.counter?.joint || 'knee';
  if (joint === 'elbow') return buildLandmarksForElbowAngle(angle);
  return buildLandmarksForKneeAngle(angle);
}

function buildBaseLandmarks() {
  return Array.from({ length: 33 }, () => ({ x: 0, y: 0, z: 0, visibility: 0.99 }));
}

function buildLandmarksForKneeAngle(angleDeg) {
  const landmarks = buildBaseLandmarks();
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

function buildLandmarksForElbowAngle(angleDeg) {
  const landmarks = buildBaseLandmarks();
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

module.exports = {
  getAllExercises,
  getExercisesByCategory,
  getExerciseById,
  getExerciseVoiceScript,
  getCategories,
  getDefaultPlan,
  simulateCounter
};
