const mongoose = require('mongoose');

const conversationSchema = new mongoose.Schema({
  userId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  type: { type: String, required: true }, // e.g. 'plan', 'general', 'feedback', ...
  sessionId: { type: String, default: 'default' },
  messages: [
    {
      role: { type: String, enum: ['user', 'assistant'], required: true },
      content: { type: String, required: true },
      timestamp: { type: Date, default: Date.now }
    }
  ],
  createdAt: { type: Date, default: Date.now }
});

conversationSchema.index({ userId: 1, type: 1, sessionId: 1 }, { unique: true });

module.exports = mongoose.model('Conversation', conversationSchema);
