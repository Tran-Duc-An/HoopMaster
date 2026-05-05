const { personalizeChat } = require('./personalizeService');

async function planChat(userId, { text, audioBase64 }) {
  return personalizeChat(userId, { text, audioBase64 });
}

module.exports = { planChat };
