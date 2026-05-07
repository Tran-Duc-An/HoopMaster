const { generatePostShotFeedback } = require('../services/llmService');

async function createShotFeedback(req, res) {
  try {
    const result = await generatePostShotFeedback(req.body);
    res.json(result);
  } catch (error) {
    console.error('[FeedbackController] Shot feedback error:', error);
    res.status(500).json({ success: false, error: error.message });
  }
}

module.exports = {
  createShotFeedback
};
