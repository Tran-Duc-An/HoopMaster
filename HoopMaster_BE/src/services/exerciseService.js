// Exercise Service: Load and provide access to exercises from JSON
const fs = require('fs');
const path = require('path');

const exercisesPath = path.join(__dirname, '../../data/exercises.json');
const DEFAULT_PLAN_EXERCISE_IDS = [1, 2];

function loadExercises() {
  try {
    const data = fs.readFileSync(exercisesPath, 'utf8');
    return JSON.parse(data);
  } catch (err) {
    console.error('[ExerciseService] Failed to load exercises:', err.message);
    return [];
  }
}

function getAllExercises() {
  return loadExercises();
}

function getExercisesByCategory(category) {
  return loadExercises().filter(ex => ex.category === category);
}

function getExerciseById(id) {
  return loadExercises().find(ex => ex.id === id);
}

function getExerciseVoiceScript(id, options = {}) {
  const exercise = getExerciseById(id);
  if (!exercise) return null;

  const sets = clampPositiveInteger(options.sets, exercise.target?.sets || 1);
  const reps = clampPositiveInteger(options.reps, exercise.target?.reps || exercise.count || 1);
  const restSeconds = clampPositiveInteger(options.restSeconds, exercise.target?.restSeconds || 30);
  const script = [];

  addScriptCue(script, 'intro', exercise.voiceCues?.intro || exercise.description);
  addScriptCue(script, 'setup', exercise.voiceCues?.setup || 'Get ready.');

  for (let set = 1; set <= sets; set++) {
    addScriptCue(script, 'set_start', `Set ${set}.`);

    for (let rep = 1; rep <= reps; rep++) {
      const repText = buildRepCue(exercise, rep);
      addScriptCue(script, 'rep', repText, { set, rep });
    }

    if (set < sets) {
      addScriptCue(
        script,
        'rest',
        `${exercise.voiceCues?.setComplete || 'Set complete.'} Rest for ${restSeconds} seconds.`,
        { set, restSeconds }
      );
    }
  }

  addScriptCue(script, 'complete', exercise.voiceCues?.complete || `${exercise.name} finished.`);

  return {
    exerciseId: exercise.id,
    name: exercise.name,
    category: exercise.category,
    pose: exercise.pose,
    target: { sets, reps, restSeconds },
    counting: exercise.counting,
    warnings: exercise.voiceCues?.warnings || [],
    script
  };
}

function buildRepCue(exercise, rep) {
  const template = exercise.voiceCues?.repTemplate;
  if (template) return template.replace('{rep}', String(rep));

  // Fallback: just count the rep, don't concatenate all phase cues
  return `Rep ${rep}.`;
}

function addScriptCue(script, type, text, metadata = {}) {
  if (!text) return;
  script.push({
    type,
    text,
    ...metadata
  });
}

function clampPositiveInteger(value, fallback) {
  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed < 1) return fallback;
  return parsed;
}

function getCategories() {
  return [...new Set(loadExercises().map(ex => ex.category).filter(Boolean))];
}

function getDefaultPlan() {
  const allExercises = loadExercises();
  const defaultExercises = allExercises.filter(exercise => DEFAULT_PLAN_EXERCISE_IDS.includes(exercise.id));

  const exercises = defaultExercises.map((exercise, index) => ({
    exerciseId: exercise.id,
    name: exercise.name,
    category: exercise.category,
    pose: exercise.pose,
    description: exercise.description,
    steps: exercise.steps,
    target: exercise.target,
    counting: exercise.counting,
    voiceCues: exercise.voiceCues,
    tracking: exercise.tracking,
    sets: exercise.target?.sets || (exercise.category === 'strength' ? 3 : 2),
    reps: exercise.target?.reps || exercise.count,
    duration: exercise.duration,
    order: index + 1
  }));

  return {
    source: 'default',
    status: 'active',
    title: 'Default Basketball Warmup Plan',
    description: 'Safe baseline warmup exercises for every session.',
    exercises
  };
}

module.exports = {
  getAllExercises,
  getExercisesByCategory,
  getExerciseById,
  getExerciseVoiceScript,
  getCategories,
  getDefaultPlan
};
