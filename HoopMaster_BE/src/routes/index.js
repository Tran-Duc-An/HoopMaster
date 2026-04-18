/**
 * Main Router
 * Gộp tất cả routes vào một file
 */

const express = require('express');
const router = express.Router();
// const sessionRoutes = require('./sessionRoutes');
// const userRoutes = require('./userRoutes');
// const exerciseRoutes = require('./exerciseRoutes');
const { validateConfig: validateTTS } = require('../services/ttsService');
const { validateConfig: validateLLM } = require('../services/llmService');
const { getAllSessions } = require('../controllers/poseController');

/**
 * GET /
 * Root endpoint
 */
router.get('/', (req, res) => {
  res.json({
    message: 'AI Basketball Coach Backend API',
    version: '1.0.0',
    endpoints: {
      health: '/health',
      sessions: '/api/sessions',
      websocket: 'ws://localhost:' + (process.env.PORT || 3000)
    }
  });
});

/**
 * GET /health
 * Health check endpoint
 */
router.get('/health', (req, res) => {
  try {
    const ttsConfig = validateTTS();
    const llmConfig = validateLLM();
    
    res.json({
      status: 'OK',
      uptime: process.uptime(),
      timestamp: new Date().toISOString(),
      services: {
        tts: ttsConfig.valid ? 'configured' : 'fallback_mode',
        llm: llmConfig.valid ? 'configured' : 'fallback_mode'
      },
      activeSessions: getAllSessions().length
    });
  } catch (error) {
    console.error('[Routes] Health check error:', error);
    res.status(500).json({ status: 'ERROR', error: error.message });
  }
});

/**
 * Mount session routes (DISABLED FOR NO-MONGO MODE)
 */
// router.use('/api/sessions', sessionRoutes);
// router.use('/api/users', userRoutes);
// Only exercise routes are enabled
const exerciseRoutes = require('./exerciseRoutes');
router.use('/api/exercises', exerciseRoutes);

module.exports = router;