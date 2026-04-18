/**
 * Pose Controller - Optimized for Single-Issue Focus (Progressive Correction)
 */

const fs = require('fs');
const path = require('path');
const poseFeedback = JSON.parse(fs.readFileSync(path.join(__dirname, '../../data/pose_feedback.json'), 'utf8'));

const { 
  POSE_RULES, 
  evaluateAngle, 
  detectShootingHand, 
  getRelevantLandmarks 
} = require('../models/ruleModel');

const { 
  calculateAngle3D, 
  detectViewOrientation,
  isPoseStable,
  calculateDistance,
  evaluatePose 
} = require('../services/poseService');

const { synthesizeSpeech } = require('../services/ttsService');
const sessionService = require('../services/sessionService');

// Cấu hình kiểm soát feedback
const CONFIG = {
  MIN_FEEDBACK_INTERVAL: 4000,     // Ít nhất 4 giây
  STABILITY_THRESHOLD: 0.01,       // Ngưỡng ổn định của pose
  ANGLE_CHANGE_THRESHOLD: (POSE_RULES?.shooting?.elbow?.threshold || 12),
  REMINDER_TIME_MS: 10000          // Nhắc lại sau 10 giây nếu tư thế vẫn sai
};

/**
 * Lấy feedback ngẫu nhiên nhưng ổn định
 */
function getRandomFeedback(joint, status) {
  const map = { tooLow: 'low', tooHigh: 'high', perfect: 'perfect' };
  const key = map[status] || 'perfect';
  const jointFeedback = poseFeedback[joint]?.feedback?.[key];
  if (jointFeedback && jointFeedback.length > 0) {
    return jointFeedback[Math.floor(Math.random() * jointFeedback.length)];
  }
  return `${joint.charAt(0).toUpperCase() + joint.slice(1)} position is ${status.replace('too', '').toLowerCase()}.`;
}

/**
 * REAL-TIME WORKFLOW
 */
async function handleRealtimePoseAnalysis(socketId, poseData, emitCallback) {
  let session = sessionService.getSession(socketId);
  if (!session) return;

  // 1. Chặn Race Condition: Nếu đang bận xử lý frame trước, bỏ qua frame này
  if (session.isProcessingRealtimeFeedback) return;

  const now = Date.now();
  const lastFeedbackTime = session.lastFeedback?.time || 0;
  
  try {
    sessionService.updateSession(socketId, { isProcessingRealtimeFeedback: true });
    sessionService.incrementStats(socketId, 'totalFrames');

    const { landmarks, exerciseType: poseDataExerciseType } = poseData;
    const exerciseType = poseDataExerciseType || session.exerciseType;

    if (!landmarks || landmarks.length === 0) return;

    // 2. Kiểm tra độ ổn định
    if (session.previousLandmarks) {
      const isStable = isPoseStable(landmarks, session.previousLandmarks);
      if (!isStable) {
        sessionService.updateSession(socketId, { previousLandmarks: landmarks });
        return; 
      }
    }

    // Luôn lưu buffer cho hậu kỳ
    sessionService.addFrameToBuffer(socketId, { landmarks, timestamp: now });

    // 3. Tính toán góc độ hiện tại
    const evalResult = await evaluatePose(exerciseType, landmarks);

    const currentPoseStatus = [
      evalResult.elbowEval?.status || 'perfect',
      evalResult.kneeEval?.status || 'perfect',
      evalResult.shoulderEval?.status || 'perfect'
    ].join('|');

    // 4. Luôn cập nhật số liệu UI ngay lập tức
    emitCallback('angles_update', evalResult);

    // 5. THROTTLE CHECK (Sử dụng Dynamic Cooldown)
    const currentCooldown = session.lastFeedback?.cooldown || CONFIG.MIN_FEEDBACK_INTERVAL;
    if (now - lastFeedbackTime < currentCooldown) {
      sessionService.updateSession(socketId, { previousLandmarks: landmarks });
      return;
    }

    // 6. Logic xác định có nên gửi Voice Feedback không
    let shouldSendFeedback = false;
    const lastAngles = session.lastFeedback?.angles || {};
    const lastReportedStatus = session.lastFeedback?.poseStatus || '';

    const elbowThreshold = POSE_RULES?.shooting?.elbow?.threshold || CONFIG.ANGLE_CHANGE_THRESHOLD;
    const kneeThreshold = POSE_RULES?.shooting?.knee?.threshold || CONFIG.ANGLE_CHANGE_THRESHOLD;
    
    const hasBigAngleChange = 
      Math.abs((evalResult.elbowAngle || 0) - (lastAngles.elbowAngle || 0)) > elbowThreshold ||
      Math.abs((evalResult.kneeAngle || 0) - (lastAngles.kneeAngle || 0)) > kneeThreshold;

    const hasStatusChanged = (lastReportedStatus !== '') && (lastReportedStatus !== currentPoseStatus);

    const isHoldingWrongPose = currentPoseStatus.includes('too');
    const isOverdue = isHoldingWrongPose && (now - lastFeedbackTime > CONFIG.REMINDER_TIME_MS);

    if (!session.lastFeedback || hasBigAngleChange || hasStatusChanged || isOverdue) {
      shouldSendFeedback = true;
    }

    if (shouldSendFeedback) {
      // LOGIC MỚI: CHỈ SỬA 1 LỖI MỖI LẦN (Single-issue focus)
      let message = '';
      
      // Khai báo thứ tự ưu tiên: Nền tảng (Gối) -> Chuyển động (Khuỷu tay) -> Phụ trợ (Vai)
      const errorPriority = [
        { joint: 'knee', data: evalResult.kneeEval },
        { joint: 'elbow', data: evalResult.elbowEval },
        { joint: 'shoulder', data: evalResult.shoulderEval }
      ];

      // Tìm lỗi ĐẦU TIÊN xuất hiện trong danh sách ưu tiên
      const primaryIssue = errorPriority.find(
        item => item.data && item.data.status && item.data.status !== 'perfect'
      );

      if (primaryIssue) {
        // Nếu có lỗi, chỉ lấy đúng 1 câu feedback của lỗi đó
        message = getRandomFeedback(primaryIssue.joint, primaryIssue.data.status);
      } else {
        // Nếu không tìm thấy lỗi nào (tất cả đều perfect)
        message = 'Great form, keep it up!';
      }

      // Gọi TTS
      const ttsResult = await synthesizeSpeech(message, 'coach');
      
      // Tính toán Dynamic Cooldown dựa trên độ dài chuỗi trả về
      const estimatedAudioDuration = (message.length * 80) + 1500; 
      const cooldownToUse = Math.max(CONFIG.MIN_FEEDBACK_INTERVAL, estimatedAudioDuration);
      
      emitCallback('coach_feedback', {
        text: message,
        audioBase64: ttsResult.audioBase64,
        angles: evalResult,
        metadata: { timestamp: now }
      });

      // Cập nhật session sau khi đã phát Voice
      sessionService.updateSession(socketId, {
        lastFeedback: { 
          angles: evalResult, 
          time: now, 
          message: message,
          poseStatus: currentPoseStatus,
          cooldown: cooldownToUse // Lưu thời gian chờ động vào đây
        },
        previousLandmarks: landmarks
      });
    } else {
      // Lưu landmarks cho frame sau đối chiếu ổn định
      sessionService.updateSession(socketId, { previousLandmarks: landmarks });
    }

  } catch (error) {
    console.error('[Controller] Realtime error:', error.message);
  } finally {
    // Luôn giải phóng flag để nhận frame tiếp theo
    sessionService.updateSession(socketId, { isProcessingRealtimeFeedback: false });
  }
}

/**
 * POST-SHOT WORKFLOW
 */
async function handlePostShotAnalysis(socketId, emitCallback) {
  try {
    const session = sessionService.getSession(socketId);
    if (!session || session.frameBuffer.length === 0) return;

    const stats = calculateShotStatistics(session.frameBuffer);
    const { generatePostShotFeedback } = require('../services/llmService');
    const llmResult = await generatePostShotFeedback(stats);

    if (llmResult.success) {
      const ttsResult = await synthesizeSpeech(llmResult.feedback, 'focus');
      emitCallback('llm_post_shot_feedback', {
        text: llmResult.feedback,
        audioBase64: ttsResult.audioBase64,
        stats,
        metadata: { timestamp: new Date().toISOString() }
      });
    }

    sessionService.clearFrameBuffer(socketId);
    sessionService.updateSession(socketId, { shotInProgress: false });
    sessionService.incrementStats(socketId, 'shotsCompleted');

  } catch (error) {
    console.error('[Controller] Post-shot error:', error.message);
  }
}

/**
 * Phân tích thống kê từ Buffer
 */
function calculateShotStatistics(frameBuffer) {
  const data = { elbow: [], knee: [], shoulder: [], back: [] };
  let shootingHand = null;

  frameBuffer.forEach(frame => {
    if (!shootingHand) shootingHand = detectShootingHand(frame.landmarks);
    const rel = getRelevantLandmarks(frame.landmarks, shootingHand);

    data.elbow.push(calculateAngle3D(rel.shoulder, rel.elbow, rel.wrist));
    data.knee.push(calculateAngle3D(rel.hip, rel.knee, rel.ankle));
    data.shoulder.push(calculateAngle3D(rel.hip, rel.shoulder, rel.elbow));
    data.back.push(calculateAngle3D(rel.shoulder, rel.hip, rel.knee));
  });

  const getAvg = arr => arr.length ? (arr.reduce((a, b) => a + b, 0) / arr.length) : null;

  return {
    avgElbowAngle: getAvg(data.elbow),
    avgKneeAngle: getAvg(data.knee),
    avgShoulderAngle: getAvg(data.shoulder),
    avgBackAngle: getAvg(data.back),
    frameCount: frameBuffer.length,
    shootingHand
  };
}

module.exports = {
  handleRealtimePoseAnalysis,
  handlePostShotAnalysis,
  calculateShotStatistics,
  getSessionInfo: (id) => sessionService.getSessionInfo(id),
  getAllSessions: () => sessionService.getAllSessions()
};