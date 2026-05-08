const {
  planningChat,
  confirmPlanningPlan,
  getPlanningHistory
} = require('../services/planningAgent');
const {
  createConversationSession,
  listSessions
} = require('../services/conversationService');

async function sendPlanningMessage(req, res) {
  try {
    const { text, audioBase64, sessionId } = req.body;
    const resolvedSessionId = sessionId || 'default';
    console.log(`[PlanningChatController] sendPlanningMessage userId=${req.params.id} sessionId=${resolvedSessionId} textLength=${(text || '').length} hasAudio=${Boolean(audioBase64)}`);
    const result = await planningChat(req.params.id, { text, audioBase64, sessionId });
    console.log(`[PlanningChatController] sendPlanningMessage success userId=${req.params.id} type=${result.type}`);
    res.json(result);
  } catch (err) {
    console.error(`[PlanningChatController] sendPlanningMessage failed userId=${req.params.id}:`, err.message);
    res.status(400).json({ error: err.message });
  }
}

async function confirmPlan(req, res) {
  try {
    const { planId } = req.body;
    if (!planId) return res.status(400).json({ error: 'planId is required' });

    console.log(`[PlanningChatController] confirmPlan userId=${req.params.id} planId=${planId}`);
    const result = await confirmPlanningPlan(req.params.id, planId);
    console.log(`[PlanningChatController] confirmPlan success userId=${req.params.id} planId=${planId}`);
    return res.json(result);
  } catch (err) {
    console.error(`[PlanningChatController] confirmPlan failed userId=${req.params.id}:`, err.message);
    return res.status(400).json({ error: err.message });
  }
}

async function getHistory(req, res) {
  try {
    const sessionId = req.query.sessionId || 'default';
    const history = await getPlanningHistory(req.params.id, 10, sessionId);
    res.json({ history });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
}

async function createSession(req, res) {
  try {
    const requestedSessionId = req.body?.sessionId;
    const conversation = await createConversationSession(req.params.id, 'planning', requestedSessionId);
    return res.status(201).json({
      sessionId: conversation.sessionId,
      createdAt: conversation.createdAt
    });
  } catch (err) {
    return res.status(400).json({ error: err.message });
  }
}

async function getSessions(req, res) {
  try {
    const sessions = await listSessions(req.params.id, 'planning');
    return res.json({ sessions });
  } catch (err) {
    return res.status(400).json({ error: err.message });
  }
}

module.exports = {
  sendPlanningMessage,
  confirmPlan,
  getHistory,
  createSession,
  getSessions
};
