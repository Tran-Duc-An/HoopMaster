const { validateConfig: validateTTS } = require('../services/ttsService');
const { validateConfig: validateLLM } = require('../services/llmService');
const { getAllSessions } = require('./poseController');

function getApiInfo(req, res) {
  res.json({
    message: 'AI Basketball Coach Backend API',
    version: '1.0.0',
    endpoints: {
      health: '/health',
      sessions: '/api/sessions',
      websocket: 'ws://localhost:' + (process.env.PORT || 3000)
    }
  });
}

function getHealth(req, res) {
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
    console.error('[SystemController] Health check error:', error);
    res.status(500).json({ status: 'ERROR', error: error.message });
  }
}

module.exports = {
  getApiInfo,
  getHealth
};
