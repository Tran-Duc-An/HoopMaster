const express = require('express');
const router = express.Router();
const {
  planningChat,
  confirmPlanningPlan,
  getPlanningHistory
} = require('../services/planningAgent');

// POST /api/users/:id/plan-chat
// Body: { text?: string, audioBase64?: string }
router.post('/:id/plan-chat', async (req, res) => {
  try {
    const { text, audioBase64 } = req.body;
    const result = await planningChat(req.params.id, { text, audioBase64 });
    res.json(result);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

// Alias mới, rõ nghĩa hơn cho planning agent.
router.post('/:id/planning-chat/message', async (req, res) => {
  try {
    const { text, audioBase64 } = req.body;
    const result = await planningChat(req.params.id, { text, audioBase64 });
    res.json(result);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

// POST /api/users/:id/planning-chat/confirm-plan
// Body: { planId }
router.post('/:id/planning-chat/confirm-plan', async (req, res) => {
  try {
    const { planId } = req.body;
    if (!planId) return res.status(400).json({ error: 'planId is required' });
    const result = await confirmPlanningPlan(req.params.id, planId);
    res.json(result);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

router.get('/:id/planning-chat/history', async (req, res) => {
  try {
    const history = await getPlanningHistory(req.params.id);
    res.json({ history });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

module.exports = router;
