const { getSessionInfo, getAllSessions } = require('./poseController');

function listSessions(req, res) {
  try {
    const sessions = getAllSessions();
    res.json({
      total: sessions.length,
      sessions
    });
  } catch (error) {
    console.error('[SessionController] Error getting all sessions:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
}

function getSessionBySocketId(req, res) {
  try {
    const sessionInfo = getSessionInfo(req.params.socketId);

    if (!sessionInfo) {
      return res.status(404).json({ error: 'Session not found' });
    }

    return res.json(sessionInfo);
  } catch (error) {
    console.error('[SessionController] Error getting session info:', error);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

module.exports = {
  listSessions,
  getSessionBySocketId
};
