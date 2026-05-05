const express = require('express');
const router = express.Router();
const { planChat } = require('../services/planChatAgent');

// POST /api/users/:id/plan-chat
// Body: { text?: string, audioBase64?: string }
router.post('/:id/plan-chat', async (req, res) => {
  try {
    const { text, audioBase64 } = req.body;
    const result = await planChat(req.params.id, { text, audioBase64 });
    res.json(result);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

module.exports = router;
