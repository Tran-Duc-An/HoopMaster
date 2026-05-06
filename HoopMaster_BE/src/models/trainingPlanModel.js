const mongoose = require('mongoose');

const trainingPlanSchema = new mongoose.Schema({
  userId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  source: {
    type: String,
    enum: ['default', 'personalized'],
    default: 'personalized'
  },
  status: {
    type: String,
    enum: ['draft', 'active', 'archived'],
    default: 'draft'
  },
  title: { type: String, required: true },
  description: { type: String },
  goal: { type: String },
  injuryConstraints: [{ type: String }],
  exercises: [
    {
      exerciseId: Number,
      name: String,
      category: String,
      pose: String,
      description: String,
      sets: Number,
      reps: Number,
      duration: String, // e.g. '30s', '1min'
      reason: String,
      safetyNotes: String,
      order: Number
    }
  ],
  schedule: {
    daysPerWeek: Number,
    sessionDurationMinutes: Number
  },
  createdBy: {
    type: String,
    enum: ['agent', 'system', 'user'],
    default: 'agent'
  },
  metadata: mongoose.Schema.Types.Mixed,
  activatedAt: Date,
  createdAt: { type: Date, default: Date.now },
  updatedAt: { type: Date, default: Date.now }
});

module.exports = mongoose.model('TrainingPlan', trainingPlanSchema);
