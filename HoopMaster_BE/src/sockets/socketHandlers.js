/**
 * Socket.io Event Handlers
 * Xử lý tất cả WebSocket events
 */

const {
  handleRealtimePoseAnalysis,
  handlePostShotAnalysis,
  getSessionInfo
} = require('../controllers/poseController');

const sessionService = require('../services/sessionService');

/**
 * Setup tất cả socket event handlers
 */
function setupSocketHandlers(io) {
  io.on('connection', (socket) => {
    console.log(`[Socket] Client connected: ${socket.id} (IP: ${socket.handshake.address})`);

    // Khởi tạo session
    sessionService.createSession(socket.id);

    // Gửi welcome message
    socket.emit('connected', {
      message: 'Connected to AI Basketball Coach',
      socketId: socket.id,
      timestamp: new Date().toISOString()
    });

    // Event: pose_data
    socket.on('pose_data', async (data) => {
      try {
        await handleRealtimePoseAnalysis(
          socket.id,
          data,
          (event, payload) => socket.emit(event, payload)
        );
      } catch (error) {
        console.error('[Socket] Error handling pose_data:', error);
        socket.emit('error', {
          message: 'Internal server error',
          code: 'SERVER_ERROR'
        });
      }
    });

    // Event: shot_released
    socket.on('shot_released', async () => {
      console.log(`[Socket] Shot released by ${socket.id}, starting post-shot analysis`);
      
      try {
        await handlePostShotAnalysis(
          socket.id,
          (event, payload) => socket.emit(event, payload)
        );
      } catch (error) {
        console.error('[Socket] Error in post-shot analysis:', error);
        socket.emit('error', {
          message: 'Post-shot analysis failed',
          code: 'POST_SHOT_ERROR'
        });
      }
    });

    // Event: request_session_info
    socket.on('request_session_info', () => {
      const sessionInfo = getSessionInfo(socket.id);
      socket.emit('session_info', sessionInfo);
    });

    // Event: ping
    socket.on('ping', (timestamp) => {
      socket.emit('pong', {
        clientTimestamp: timestamp,
        serverTimestamp: Date.now()
      });
    });

    // Event: reset_session
    socket.on('reset_session', () => {
      console.log(`[Socket] Resetting session for ${socket.id}`);
      sessionService.resetSession(socket.id);
      socket.emit('session_reset', { success: true });
    });

    // Event: disconnect
    socket.on('disconnect', (reason) => {
      console.log(`[Socket] Client disconnected: ${socket.id}, reason: ${reason}`);
      
      // Cleanup session sau 30 giây (cho phép reconnect)
      setTimeout(() => {
        sessionService.deleteSession(socket.id);
      }, 30000);
    });

    // Event: error
    socket.on('error', (error) => {
      console.error(`[Socket] Client error from ${socket.id}:`, error);
    });
  });
}

module.exports = { setupSocketHandlers };