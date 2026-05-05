const mongoose = require('mongoose');

const trainingPlanSchema = new mongoose.Schema({
  userId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  title: { type: String, required: true },
  description: { type: String },
  exercises: [
    {
      name: String,
      description: String,
      sets: Number,
      reps: Number,
      duration: String // e.g. '30s', '1min'
    }
  ],
  createdAt: { type: Date, default: Date.now }
});

module.exports = mongoose.model('TrainingPlan', trainingPlanSchema);
