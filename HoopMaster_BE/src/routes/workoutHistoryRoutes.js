const express = require('express');
const router = express.Router();
const workoutHistoryController = require('../controllers/workoutHistoryController');

router.get('/:id/workout-history/weekly', workoutHistoryController.getWeeklyHistory);
router.post('/:id/workout-history/log', workoutHistoryController.logWorkout);
router.get('/:id/workout-history', workoutHistoryController.getAllHistory);

module.exports = router;