const mongoose = require('mongoose');

const userSchema = new mongoose.Schema({
  username: { type: String, required: true, unique: true },
  email: { type: String, required: true, unique: true },
  password: { type: String, required: true },
  name: { type: String },
  avatar: { type: String },
  trainingProfile: {
    level: {
      type: String,
      enum: ['beginner', 'intermediate', 'advanced', 'unknown'],
      default: 'unknown'
    },
    goals: [{ type: String }],
    injuries: [
      {
        area: String,
        severity: {
          type: String,
          enum: ['mild', 'moderate', 'severe', 'unknown'],
          default: 'unknown'
        },
        notes: String,
        active: { type: Boolean, default: true }
      }
    ],
    equipment: [{ type: String }],
    weeklyAvailability: Number,
    sessionDurationMinutes: Number,
    updatedAt: Date
  },
  createdAt: { type: Date, default: Date.now },
  updatedAt: { type: Date, default: Date.now },
  tone: { type: String, enum: ['strict', 'cheerful', 'neutral'], default: 'neutral' }
});

module.exports = mongoose.model('User', userSchema);
