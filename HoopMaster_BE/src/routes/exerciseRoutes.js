const express = require('express');
const router = express.Router();
const exerciseService = require('../services/exerciseService');

// Lấy tất cả bài tập
router.get('/', (req, res) => {
  res.json(exerciseService.getAllExercises());
});

// Lấy bài tập theo category
router.get('/category/:category', (req, res) => {
  const { category } = req.params;
  res.json(exerciseService.getExercisesByCategory(category));
});

// Lấy bài tập theo id
router.get('/:id', (req, res) => {
  const { id } = req.params;
  const exercise = exerciseService.getExerciseById(Number(id));
  if (exercise) {
    res.json(exercise);
  } else {
    res.status(404).json({ error: 'Exercise not found' });
  }
});

module.exports = router;
