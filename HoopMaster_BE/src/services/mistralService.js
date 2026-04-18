// Mistral API integration for basketball shot analysis
const axios = require('axios');

/**
 * Call Mistral API to generate feedback
 * @param {string} prompt
 * @returns {Promise<string>} feedback
 */
async function callMistralAPI(prompt) {
  const API_KEY = process.env.MISTRAL_API_KEY;
  const MODEL = process.env.LLM_MODEL || 'mistral-small-latest';
  const ENDPOINT = `https://api.mistral.ai/v1/chat/completions`;
  const body = {
    model: MODEL,
    messages: [
      { role: 'user', content: prompt }
    ],
    max_tokens: parseInt(process.env.LLM_MAX_TOKENS) || 1000
  };

  // Log request chi tiết
  console.log('[Mistral] REQUEST:', {
    url: ENDPOINT,
    headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${API_KEY}` },
    body
  });

  try {
    const response = await axios.post(
      ENDPOINT,
      body,
      { headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${API_KEY}` }, timeout: 10000 }
    );
    // Log response chi tiết
    console.log('[Mistral] RESPONSE:', response.status, response.data);
    const content = response.data.choices?.[0]?.message?.content;
    if (content) {
      console.log('[Mistral] API call SUCCESS');
      return content.trim();
    }
    console.error('[Mistral] API call FAILED: No feedback returned from Mistral');
    throw new Error('No feedback returned from Mistral');
  } catch (error) {
    if (error.response) {
      console.error('[Mistral] API call FAILED:', error.response.status, error.response.data);
    } else {
      console.error('[Mistral] API call FAILED:', error.message);
    }
    throw error;
  }
}

module.exports = { callMistralAPI };
