const Conversation = require('../models/conversationModel');

const getOrCreateConversation = async (userId, type) => {
  let convo = await Conversation.findOne({ userId, type });
  if (!convo) {
    convo = new Conversation({ userId, type, messages: [] });
    await convo.save();
  }
  return convo;
};

const addMessage = async (userId, type, role, content) => {
  const convo = await getOrCreateConversation(userId, type);
  convo.messages.push({ role, content });
  await convo.save();
  return convo;
};

const getHistory = async (userId, type, limit = 10) => {
  const convo = await getOrCreateConversation(userId, type);
  return convo.messages.slice(-limit);
};

module.exports = { getOrCreateConversation, addMessage, getHistory };
