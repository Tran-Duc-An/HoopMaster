const Conversation = require('../models/conversationModel');

const normalizeSessionId = (sessionId) => {
  if (!sessionId || typeof sessionId !== 'string') return 'default';
  const trimmed = sessionId.trim();
  return trimmed || 'default';
};

const getOrCreateConversation = async (userId, type, sessionId = 'default') => {
  const normalizedSessionId = normalizeSessionId(sessionId);
  let convo = await Conversation.findOne({ userId, type, sessionId: normalizedSessionId });
  if (!convo) {
    convo = new Conversation({ userId, type, sessionId: normalizedSessionId, messages: [] });
    await convo.save();
  }
  return convo;
};

const addMessage = async (userId, type, role, content, sessionId = 'default') => {
  const convo = await getOrCreateConversation(userId, type, sessionId);
  convo.messages.push({ role, content });
  await convo.save();
  return convo;
};

const getHistory = async (userId, type, limit = 10, sessionId = 'default') => {
  const convo = await getOrCreateConversation(userId, type, sessionId);
  return convo.messages.slice(-limit);
};

const generateSessionId = (prefix = 'planning') => {
  const randomSuffix = Math.random().toString(36).slice(2, 8);
  return `${prefix}-${Date.now()}-${randomSuffix}`;
};

const createConversationSession = async (userId, type, sessionId) => {
  const finalSessionId = normalizeSessionId(sessionId) === 'default'
    ? generateSessionId(type)
    : normalizeSessionId(sessionId);

  const existing = await Conversation.findOne({ userId, type, sessionId: finalSessionId });
  if (existing) return existing;

  const convo = new Conversation({ userId, type, sessionId: finalSessionId, messages: [] });
  await convo.save();
  return convo;
};

const listSessions = async (userId, type) => {
  const conversations = await Conversation
    .find({ userId, type })
    .sort({ createdAt: -1 });

  return conversations.map(convo => ({
    sessionId: convo.sessionId || 'default',
    createdAt: convo.createdAt,
    messageCount: convo.messages.length,
    lastMessageAt: convo.messages.length
      ? convo.messages[convo.messages.length - 1].timestamp
      : convo.createdAt
  }));
};

module.exports = {
  getOrCreateConversation,
  addMessage,
  getHistory,
  normalizeSessionId,
  createConversationSession,
  listSessions
};
