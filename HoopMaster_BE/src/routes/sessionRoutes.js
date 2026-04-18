/**
 * Session Routes
 * REST API endpoints cho session management
 */

const express = require('express');
const router = express.Router();
const { getSessionInfo, getAllSessions } = require('../controllers/poseController');

/**
 * GET /api/sessions
 * Lấy danh sách tất cả sessions
 */
router.get('/', (req, res) => {
  try {
    const sessions = getAllSessions();
    res.json({
      total: sessions.length,
      sessions
    });
  } catch (error) {
    console.error('[SessionRoutes] Error getting all sessions:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

/**
 * GET /api/sessions/:socketId
 * Lấy thông tin chi tiết của một session
 */
router.get('/:socketId', (req, res) => {
  try {
    const sessionInfo = getSessionInfo(req.params.socketId);
    
    if (!sessionInfo) {
      return res.status(404).json({ error: 'Session not found' });
    }
    
    res.json(sessionInfo);
  } catch (error) {
    console.error('[SessionRoutes] Error getting session info:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

module.exports = router;