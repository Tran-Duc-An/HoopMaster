const mongoose = require('mongoose');

const workoutHistorySchema = new mongoose.Schema({
  userId: { type: String, required: true, index: true },
  date: { type: String, required: true }, // YYYY-MM-DD
  totalExercises: { type: Number, default: 0 },
  totalSets: { type: Number, default: 0 },
  totalReps: { type: Number, default: 0 },
  totalMinutes: { type: Number, default: 0 },
  exercises: [{
    exerciseId: Number,
    name: String,
    category: String,
    sets: Number,
    reps: Number,
    durationMinutes: Number
  }],
  createdAt: { type: Date, default: Date.now }
});

// Compound index for efficient per-user, per-date queries
workoutHistorySchema.index({ userId: 1, date: -1 });

module.exports = mongoose.model('WorkoutHistory', workoutHistorySchema);