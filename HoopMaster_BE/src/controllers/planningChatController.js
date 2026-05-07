const {
  planningChat,
  confirmPlanningPlan,
  getPlanningHistory
} = require('../services/planningAgent');

async function sendPlanningMessage(req, res) {
  try {
    const { text, audioBase64 } = req.body;
    const result = await planningChat(req.params.id, { text, audioBase64 });
    res.json(result);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
}

async function confirmPlan(req, res) {
  try {
    const { planId } = req.body;
    if (!planId) return res.status(400).json({ error: 'planId is required' });

    const result = await confirmPlanningPlan(req.params.id, planId);
    return res.json(result);
  } catch (err) {
    return res.status(400).json({ error: err.message });
  }
}

async function getHistory(req, res) {
  try {
    const history = await getPlanningHistory(req.params.id);
    res.json({ history });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
}

module.exports = {
  sendPlanningMessage,
  confirmPlan,
  getHistory
};
