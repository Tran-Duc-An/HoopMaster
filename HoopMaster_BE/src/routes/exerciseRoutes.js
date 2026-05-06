const express = require('express');
const router = express.Router();
const exerciseController = require('../controllers/exerciseController');

router.get('/', exerciseController.getAllExercises);
router.get('/default', exerciseController.getDefaultPlan);
router.get('/categories', exerciseController.getCategories);
router.get('/category/:category', exerciseController.getExercisesByCategory);
router.post('/:id/simulate-counter', exerciseController.simulateCounter);
router.get('/:id/voice-script', exerciseController.getExerciseVoiceScript);
router.get('/:id', exerciseController.getExerciseById);

module.exports = router;
