const express = require('express');
const router = express.Router();
const { generatePostShotFeedback } = require('../services/llmService');

// POST /api/feedback/shot
// Body: { ...shotData, tone: 'strict' | 'cheerful' | 'neutral' }
router.post('/shot', async (req, res) => {
  try {
    const shotData = req.body;
    // tone is optional, default handled in service
    const result = await generatePostShotFeedback(shotData);
    res.json(result);
  } catch (error) {
    console.error('[Routes] Shot feedback error:', error);
    res.status(500).json({ success: false, error: error.message });
  }
});

module.exports = router;
