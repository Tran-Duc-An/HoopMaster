// Exercise Service: Load and provide access to exercises from JSON
const fs = require('fs');
const path = require('path');

const exercisesPath = path.join(__dirname, '../../data/exercises.json');

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

module.exports = {
  getAllExercises,
  getExercisesByCategory,
  getExerciseById
};
