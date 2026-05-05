const { callMistralAPI } = require('./mistralService');
const { synthesizeSpeech } = require('./ttsService');
const { addMessage, getHistory } = require('./conversationService');

// Placeholder for STT integration
const toTextFromAudio = async (audioBase64) => {
  // TODO: Tích hợp dịch vụ STT
  return '[speech-to-text result here]';
};

async function aiConversation(userId, type, { text, audioBase64 }, systemPrompt) {
  let userText = text;
  if (!userText && audioBase64) {
    userText = await toTextFromAudio(audioBase64);
  }
  if (!userText) throw new Error('No input text or audio');

  await addMessage(userId, type, 'user', userText);
  const history = await getHistory(userId, type, 10);
  // systemPrompt: cho phép tuỳ chỉnh mục đích hội thoại
  const prompt = `${systemPrompt || 'You are an AI assistant.'}\nConversation history:\n${history.map(m => m.role+': '+m.content).join('\n')}\nassistant:`;
  const llmReply = await callMistralAPI(prompt);
  await addMessage(userId, type, 'assistant', llmReply);
  const ttsResult = await synthesizeSpeech(llmReply, 'focus');
  return { reply: llmReply, audioBase64: ttsResult.audioBase64 };
}

module.exports = { aiConversation };
