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
const ttsService = require('../services/ttsService');

/**
 * Setup tất cả socket event handlers
 */
function setupSocketHandlers(io) {
  io.on('connection', (socket) => {
    console.log(`[Socket] Client connected: ${socket.id} (IP: ${socket.handshake.address})`);

    // Khởi tạo session
    sessionService.createSession(socket.id);


    // Gửi welcome message dạng text
    socket.emit('connected', {
      message: 'Chào mừng bạn đến với AI Basketball Coach!',
      socketId: socket.id,
      timestamp: new Date().toISOString()
    });

    // Gửi lời chào và hướng dẫn dưới dạng audio base64
    (async () => {
      try {
        // Lời chào
        const welcomeText = 'Welcome to AI Basketball Coach!';
        const welcomeAudio = await ttsService.synthesizeSpeech(welcomeText, 'cheerful');
        const welcomePayload = {
          type: 'welcome',
          audioBase64: welcomeAudio.audioBase64,
          text: welcomeText,
          timestamp: new Date().toISOString()
        };
        console.log('[Socket][audio_feedback] welcome:', {
          type: welcomePayload.type,
          text: welcomePayload.text,
          audioBase64Length: welcomePayload.audioBase64?.length
        });
        socket.emit('audio_feedback', welcomePayload);

        // Thêm delay giữa hai audio để tránh chồng chéo
        setTimeout(async () => {
          const instructionText = 'Please get into shooting position to start your training session.';
          const instructionAudio = await ttsService.synthesizeSpeech(instructionText, 'neutral');
          const instructionPayload = {
            type: 'instruction',
            audioBase64: instructionAudio.audioBase64,
            text: instructionText,
            timestamp: new Date().toISOString()
          };
          console.log('[Socket][audio_feedback] instruction:', {
            type: instructionPayload.type,
            text: instructionPayload.text,
            audioBase64Length: instructionPayload.audioBase64?.length
          });
          socket.emit('audio_feedback', instructionPayload);
        }, 2000); // 2.0s delay, có thể điều chỉnh
      } catch (err) {
        console.error('[Socket] TTS error:', err);
      }
    })();

    // Event: pose_data
    socket.on('pose_data', async (data) => {
      console.log('[Socket][pose_data] Received from client:', {
        socketId: socket.id,
        data
      });
      try {
        await handleRealtimePoseAnalysis(
          socket.id,
          data,
          (event, payload) => {
            if (event === 'audio_feedback') {
              console.log('[Socket][audio_feedback][pose_data] emit:', {
                type: payload?.type,
                text: payload?.text,
                audioBase64Length: payload?.audioBase64?.length
              });
            }
            socket.emit(event, payload);
          }
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