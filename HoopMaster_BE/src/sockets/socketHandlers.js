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
const { enqueueAudioInstruction, clearAudioQueue } = require('../services/audioInstructionQueueService');
const {
  createExerciseRuntime,
  processExerciseFrame,
  normalizeCoachTone
} = require('../services/exerciseCounterService');
const workoutHistoryService = require('../services/workoutHistoryService');

const EXERCISE_CUE_COOLDOWN_MS = parseInt(process.env.EXERCISE_CUE_COOLDOWN_MS, 10) || 1200;

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

    emitQueuedAudio(socket, {
      type: 'welcome',
      text: 'Welcome to AI Basketball Coach!',
      tone: 'cheerful',
      priority: 'normal',
      dedupeKey: 'welcome'
    });

    emitQueuedAudio(socket, {
      type: 'instruction',
      text: 'Please get into shooting position to start your training session.',
      tone: 'neutral',
      priority: 'normal',
      dedupeKey: 'startup_instruction'
    });

    // Event: pose_data
    socket.on('pose_data', async (data) => {
      console.log('[Socket][pose_data] Received from client:', {
        socketId: socket.id,
        data
      });
      try {
        const session = sessionService.getSession(socket.id);
        if (session.exerciseRuntime && !session.exerciseRuntime.completed) {
          const result = processExerciseFrame(session.exerciseRuntime, data?.landmarks, Date.now());
          sessionService.updateSession(socket.id, { exerciseRuntime: result.runtime });

          socket.emit('exercise_progress', result.progress);

          if (result.runtime?.completed) {
            sessionService.incrementStats(socket.id, 'exercisesCompleted');
            // Đánh dấu session vừa hoàn thành exercise để tránh fallback
            // vào shooting form analysis ở các frame tiếp theo
            sessionService.updateSession(socket.id, { exerciseJustCompleted: true });
            // Ghi lại lịch sử tập luyện
            const runtime = result.runtime;
            const exercise = runtime.exercise;
            if (exercise) {
              const durationMinutes = Math.ceil(
                ((runtime.targetSets || 1) * (runtime.targetReps || 1) * (exercise.counting?.secondsPerRep || 4)) / 60
              );
              const userId = socket.handshake?.query?.userId || socket.id;
              workoutHistoryService.logWorkout(userId, {
                exerciseId: exercise.id,
                name: exercise.name,
                category: exercise.category,
                sets: runtime.targetSets || 1,
                reps: runtime.targetReps || 0,
                durationMinutes: Math.max(1, durationMinutes)
              }).catch(err => console.error('[Socket] Failed to log workout:', err.message));
            }
          }

          if (result.runtime?.nextCue) {
            await emitExerciseAudioCue(socket, result.runtime);
          }

          return;
        }

        // Khi exercise đã hoàn thành hoặc vừa hoàn thành, bỏ qua shooting form analysis
        if (session.exerciseRuntime?.completed || session.exerciseJustCompleted) {
          // Giữ flag trong 2 giây để tránh shooting form feedback ngay sau exercise
          return;
        }

        // Khi chưa có exerciseRuntime nhưng exercise đang được khởi tạo (race condition),
        // hoặc khi session còn quá mới (< 2 giây), bỏ qua shooting form analysis
        // để tránh instruction shooting form xuất hiện trong exercise mode.
        const sessionAge = Date.now() - (session.createdAt || Date.now());
        const isWaitingForExercise = session.startExercisePending || sessionAge < 2000;
        if (isWaitingForExercise) {
          return;
        }

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

    // Event: start_exercise
    // Payload: { exerciseId, sets?, reps?, restSeconds?, tone? }
    socket.on('start_exercise', async (payload = {}) => {
      try {
        // Đặt flag startExercisePending ngay lập tức để chặn pose_data
        // chạy shooting form analysis trong khi đang khởi tạo exercise
        sessionService.updateSession(socket.id, { startExercisePending: true, exerciseJustCompleted: false });

        const runtime = createExerciseRuntime(payload.exerciseId, payload);
        sessionService.updateSession(socket.id, {
          exerciseRuntime: runtime,
          frameBuffer: [],
          previousLandmarks: null,
          startExercisePending: false
        });

        socket.emit('exercise_started', {
          exerciseId: runtime.exerciseId,
          name: runtime.exercise.name,
          category: runtime.exercise.category,
          targetSets: runtime.targetSets,
          targetReps: runtime.targetReps,
          restSeconds: runtime.restSeconds,
          tone: runtime.coachTone,
          counting: runtime.exercise.counting,
          tracking: runtime.exercise.tracking
        });

        await emitExerciseAudioCue(socket, runtime, true);
      } catch (error) {
        console.error('[Socket] Error starting exercise:', error);
        // Reset flag nếu có lỗi
        sessionService.updateSession(socket.id, { startExercisePending: false });
        socket.emit('error', {
          message: error.message || 'Failed to start exercise',
          code: 'START_EXERCISE_ERROR'
        });
      }
    });
    // Event: stop_exercise
    socket.on('stop_exercise', () => {
      const session = sessionService.getSession(socket.id);
      const exerciseId = session.exerciseRuntime?.exerciseId;
      clearAudioQueue(socket.id, 'stop_exercise');
      // Đánh dấu exercise vừa kết thúc để tránh shooting form feedback
      sessionService.updateSession(socket.id, { exerciseRuntime: null, exerciseJustCompleted: true });
      socket.emit('exercise_stopped', {
        success: true,
        exerciseId
      });
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
      clearAudioQueue(socket.id, 'reset_session');
      sessionService.resetSession(socket.id);
      socket.emit('session_reset', { success: true });
    });

    // Event: disconnect
    socket.on('disconnect', (reason) => {
      console.log(`[Socket] Client disconnected: ${socket.id}, reason: ${reason}`);
      clearAudioQueue(socket.id, 'disconnect');
      
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

async function emitExerciseAudioCue(socket, runtime, force = false) {
  const cue = runtime.nextCue;
  if (!cue?.text) return;

  const now = Date.now();
  const cueKey = `${cue.type}:${cue.metadata?.set || 0}:${cue.metadata?.rep || 0}:${cue.text}`;
  if (!force && runtime.lastCueKey === cueKey) return;
  if (!force && runtime.lastCueAt && now - runtime.lastCueAt < EXERCISE_CUE_COOLDOWN_MS) return;

  const tone = normalizeCoachTone(runtime.coachTone || (cue.type === 'complete' ? 'cheerful' : 'neutral'));
  await emitQueuedAudio(socket, {
    type: `exercise_${cue.type}`,
    text: cue.text,
    tone,
    priority: cue.type === 'complete' ? 'high' : 'normal',
    dedupeKey: cueKey,
    exerciseId: runtime.exerciseId,
    metadata: {
      ...cue.metadata,
      tone
    },
    timestamp: new Date().toISOString()
  });

  runtime.lastCueKey = cueKey;
  runtime.lastCueAt = now;
  runtime.nextCue = null;
  sessionService.updateSession(socket.id, { exerciseRuntime: runtime });
}

async function emitQueuedAudio(socket, instruction, options = {}) {
  return enqueueAudioInstruction(
    socket.id,
    instruction,
    (event, payload) => {
      if (event === 'audio_feedback') {
        console.log('[Socket][audio_feedback][queued] emit:', {
          type: payload?.type,
          text: payload?.text,
          audioBase64Length: payload?.audioBase64?.length
        });
      }
      socket.emit(event, payload);
    },
    options
  );
}

module.exports = { setupSocketHandlers };
