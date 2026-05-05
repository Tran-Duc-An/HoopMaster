/**
 * AI Basketball Coach - Backend Server
 * Main entry point với Express.js và Socket.io
 * Tối ưu cho Mobile: CORS mở rộng, Ping/Pong Heartbeat, Reconnection handling
 */

require('dotenv').config({ override: true });
const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const cors = require('cors');

const routes = require('./routes');
const { setupSocketHandlers } = require('./sockets/socketHandlers');
const { cleanupExpiredSessions, getAllSessions } = require('./controllers/poseController');
const connectDB = require('./models/connectDB');

// Configuration
const PORT = process.env.PORT || 3000;
const ALLOWED_ORIGINS = process.env.ALLOWED_ORIGINS 
  ? process.env.ALLOWED_ORIGINS.split(',') 
  : ['http://localhost:3000', 'http://localhost:5173'];

// Initialize Express
const app = express();
const server = http.createServer(app);

// Connect to MongoDB
connectDB();

// Middleware
app.use(cors({
  origin: ALLOWED_ORIGINS,
  credentials: true
}));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Mount routes (will only enable exercise endpoints in routes/index.js)
app.use('/', routes);

// Initialize Socket.io
const io = new Server(server, {
  cors: {
    origin: ALLOWED_ORIGINS,
    methods: ['GET', 'POST'],
    credentials: true
  },
  pingTimeout: 60000,
  pingInterval: 25000,
  upgradeTimeout: 30000,
  maxHttpBufferSize: 1e8,
  transports: ['websocket', 'polling'],
  allowEIO3: true
});

// Setup socket handlers
setupSocketHandlers(io);

// (Đã di chuyển block log TTS/LLM vào trong server.listen)
if (process.env.NODE_ENV === 'development') {
  setInterval(() => {
    const sessions = getAllSessions();
    console.log(`[Server] Active sessions: ${sessions.length}`);
    
    if (sessions.length > 0) {
      const totalFrames = sessions.reduce((sum, s) => sum + s.stats.totalFrames, 0);
      console.log(`[Server] Total frames processed: ${totalFrames}`);
    }
  }, 30000);
}

// Graceful shutdown
process.on('SIGTERM', () => {
  console.log('[Server] SIGTERM received, shutting down gracefully...');
  
  server.close(() => {
    console.log('[Server] HTTP server closed');
    console.log('[Server] All sessions cleaned up');
    process.exit(0);
  });

  setTimeout(() => {
    console.error('[Server] Forced shutdown after timeout');
    process.exit(1);
  }, 10000);
});

process.on('SIGINT', () => {
  console.log('[Server] SIGINT received, shutting down...');
  process.exit(0);
});

// Start server
server.listen(PORT, () => {
  const { validateConfig: validateTTS } = require('./services/ttsService');
  const { validateConfig: validateLLM } = require('./services/llmService');
  
  console.log('\n====================================');
  console.log('🏀 AI Basketball Coach Backend');
  console.log('====================================');
  console.log(`Server running on port: ${PORT}`);
  console.log(`Environment: ${process.env.NODE_ENV || 'development'}`);
  console.log(`Allowed origins: ${ALLOWED_ORIGINS.join(', ')}`);
  console.log('\nEndpoints:');
  console.log(`  - Health: http://localhost:${PORT}/health`);
  console.log(`  - Sessions: http://localhost:${PORT}/api/sessions`);
  console.log(`  - WebSocket: ws://localhost:${PORT}`);
  console.log('\nService Status:');
  
  const ttsConfig = validateTTS();
  const llmConfig = validateLLM();
  
  console.log(`  - TTS: ${ttsConfig.valid ? '✓ Configured' : '⚠ Fallback mode'}`);
  console.log(`    Provider: ${ttsConfig.config.provider}`);
  
  console.log(`  - LLM: ${llmConfig.valid ? '✓ Configured' : '⚠ Fallback mode'}`);
  console.log(`    Model: ${llmConfig.config.model}`);
  
  console.log('\n====================================\n');
  console.log('Waiting for client connections...\n');
});

module.exports = { app, server, io };