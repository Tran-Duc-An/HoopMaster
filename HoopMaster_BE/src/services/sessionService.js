/**
 * Session Management Service
 * Quản lý lifecycle của tất cả client sessions
 */

// Session storage (trong production nên dùng Redis)
const activeSessions = new Map();

// Configuration
const SESSION_TIMEOUT_MS = parseInt(process.env.SESSION_TIMEOUT_MS) || 300000; // 5 phút
const MAX_FRAMES_BUFFER = parseInt(process.env.MAX_FRAMES_BUFFER) || 100;
const FEEDBACK_COOLDOWN_MS = parseInt(process.env.FEEDBACK_COOLDOWN_MS) || 3000;

/**
 * Khởi tạo session mới
 */
function createSession(socketId) {
  const session = {
    socketId,
    createdAt: Date.now(),
    lastActivity: Date.now(),
    lastFeedbackTime: 0,
    frameBuffer: [],
    previousLandmarks: null,
    shotInProgress: false,
    sessionStats: {
      totalFrames: 0,
      feedbackCount: 0,
      shotsCompleted: 0
    }
  };

  activeSessions.set(socketId, session);
  console.log(`[SessionService] Session created: ${socketId}`);
  
  return session;
}

/**
 * Lấy session hoặc tạo mới nếu chưa tồn tại
 */
function getSession(socketId) {
  if (!activeSessions.has(socketId)) {
    return createSession(socketId);
  }
  
  const session = activeSessions.get(socketId);
  session.lastActivity = Date.now();
  return session;
}

/**
 * Cập nhật session
 */
function updateSession(socketId, updates) {
  const session = getSession(socketId);
  Object.assign(session, updates);
  activeSessions.set(socketId, session);
  return session;
}

/**
 * Xóa session
 */
function deleteSession(socketId) {
  if (activeSessions.has(socketId)) {
    const session = activeSessions.get(socketId);
    console.log(`[SessionService] Session deleted: ${socketId} (Stats: ${JSON.stringify(session.sessionStats)})`);
    activeSessions.delete(socketId);
    return true;
  }
  return false;
}

/**
 * Lấy thông tin session
 */
function getSessionInfo(socketId) {
  if (!activeSessions.has(socketId)) {
    return null;
  }

  const session = activeSessions.get(socketId);
  return {
    socketId: session.socketId,
    uptime: Date.now() - session.createdAt,
    lastActivity: session.lastActivity,
    stats: session.sessionStats,
    bufferSize: session.frameBuffer.length
  };
}

/**
 * Lấy tất cả sessions
 */
function getAllSessions() {
  const sessions = [];
  activeSessions.forEach((session, socketId) => {
    sessions.push(getSessionInfo(socketId));
  });
  return sessions;
}

/**
 * Cleanup expired sessions
 */
function cleanupExpiredSessions() {
  const now = Date.now();
  let cleanedCount = 0;

  activeSessions.forEach((session, socketId) => {
    const idleTime = now - session.lastActivity;
    if (idleTime > SESSION_TIMEOUT_MS) {
      deleteSession(socketId);
      cleanedCount++;
    }
  });

  if (cleanedCount > 0) {
    console.log(`[SessionService] Cleaned up ${cleanedCount} expired sessions`);
  }

  return cleanedCount;
}

/**
 * Reset session (clear buffer, stats)
 */
function resetSession(socketId) {
  const session = getSession(socketId);
  session.frameBuffer = [];
  session.previousLandmarks = null;
  session.shotInProgress = false;
  session.lastFeedbackTime = 0;
  session.sessionStats = {
    totalFrames: 0,
    feedbackCount: 0,
    shotsCompleted: 0
  };
  activeSessions.set(socketId, session);
  return session;
}

/**
 * Thêm frame vào buffer
 */
function addFrameToBuffer(socketId, frame) {
  const session = getSession(socketId);
  
  if (session.frameBuffer.length >= MAX_FRAMES_BUFFER) {
    session.frameBuffer.shift();
  }
  
  session.frameBuffer.push(frame);
  activeSessions.set(socketId, session);
}

/**
 * Kiểm tra cooldown
 */
function canSendFeedback(socketId) {
  const session = getSession(socketId);
  const timeSinceLastFeedback = Date.now() - session.lastFeedbackTime;
  return timeSinceLastFeedback >= FEEDBACK_COOLDOWN_MS;
}

/**
 * Update feedback timestamp
 */
function updateFeedbackTime(socketId) {
  const session = getSession(socketId);
  session.lastFeedbackTime = Date.now();
  session.sessionStats.feedbackCount++;
  activeSessions.set(socketId, session);
}

/**
 * Increment stats counters
 */
function incrementStats(socketId, statName) {
  const session = getSession(socketId);
  if (session.sessionStats[statName] !== undefined) {
    session.sessionStats[statName]++;
    activeSessions.set(socketId, session);
  }
}

/**
 * Clear frame buffer
 */
function clearFrameBuffer(socketId) {
  const session = getSession(socketId);
  session.frameBuffer = [];
  activeSessions.set(socketId, session);
}

module.exports = {
  createSession,
  getSession,
  updateSession,
  deleteSession,
  getSessionInfo,
  getAllSessions,
  cleanupExpiredSessions,
  resetSession,
  addFrameToBuffer,
  canSendFeedback,
  updateFeedbackTime,
  incrementStats,
  clearFrameBuffer
};