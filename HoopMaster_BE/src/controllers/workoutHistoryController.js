const workoutHistoryService = require('../services/workoutHistoryService');

async function getWeeklyHistory(req, res) {
  try {
    const userId = req.params.id;
    const days = await workoutHistoryService.getWeeklyHistory(userId);
    res.json({ days });
  } catch (err) {
    console.error('[WorkoutHistoryController] getWeeklyHistory error:', err.message);
    res.status(400).json({ error: err.message });
  }
}

async function logWorkout(req, res) {
  try {
    const userId = req.params.id;
    const exerciseData = req.body;
    const record = await workoutHistoryService.logWorkout(userId, exerciseData);
    res.status(201).json(record);
  } catch (err) {
    console.error('[WorkoutHistoryController] logWorkout error:', err.message);
    res.status(400).json({ error: err.message });
  }
}

async function getAllHistory(req, res) {
  try {
    const userId = req.params.id;
    const limit = parseInt(req.query.limit) || 30;
    const history = await workoutHistoryService.getAllHistory(userId, limit);
    res.json({ history });
  } catch (err) {
    console.error('[WorkoutHistoryController] getAllHistory error:', err.message);
    res.status(400).json({ error: err.message });
  }
}

module.exports = {
  getWeeklyHistory,
  logWorkout,
  getAllHistory
};