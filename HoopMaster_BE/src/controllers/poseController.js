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
  // Ưu tiên lấy neutral nếu có, nếu không thì lấy strict/cheerful bất kỳ
  const jointFeedback = poseFeedback[joint]?.feedback?.[key];
  if (jointFeedback) {
    if (Array.isArray(jointFeedback.neutral) && jointFeedback.neutral.length > 0) {
      return jointFeedback.neutral[Math.floor(Math.random() * jointFeedback.neutral.length)];
    }
    // Nếu không có neutral, thử strict hoặc cheerful
    const allVariants = [];
    if (Array.isArray(jointFeedback.strict)) allVariants.push(...jointFeedback.strict);
    if (Array.isArray(jointFeedback.cheerful)) allVariants.push(...jointFeedback.cheerful);
    if (allVariants.length > 0) {
      return allVariants[Math.floor(Math.random() * allVariants.length)];
    }
  }
  // Nếu không có gì, trả về câu mặc định tiếng Anh
  return `${joint.charAt(0).toUpperCase() + joint.slice(1)} position is ${status.replace('too', '').toLowerCase()}.`;
}

/**
 * REAL-TIME WORKFLOW
 */
async function handleRealtimePoseAnalysis(socketId, poseData, emitCallback) {
        const { landmarks } = poseData;
      // --- Bộ lọc: chỉ feedback khi đã vào tư thế ném ---
      // Điều kiện: cả hai đầu gối đều gập (knee angle < 160), hai tay đều giơ lên (elbow cao hơn hông, cổ tay cao hơn khuỷu)
      // Có thể điều chỉnh điều kiện này cho phù hợp thực tế
      const idx = {
        leftShoulder: 11, rightShoulder: 12,
        leftElbow: 13, rightElbow: 14,
        leftWrist: 15, rightWrist: 16,
        leftHip: 23, rightHip: 24,
        leftKnee: 25, rightKnee: 26,
        leftAnkle: 27, rightAnkle: 28
      };
      function isInShootingPose(landmarks) {
        if (!Array.isArray(landmarks)) return false;
        // Kiểm tra gập gối
        const leftKnee = landmarks[idx.leftKnee], leftHip = landmarks[idx.leftHip], leftAnkle = landmarks[idx.leftAnkle];
        const rightKnee = landmarks[idx.rightKnee], rightHip = landmarks[idx.rightHip], rightAnkle = landmarks[idx.rightAnkle];
        function angle(a, b, c) {
          if (!a || !b || !c) return 180;
          const ab = Math.sqrt((a.x-b.x)**2 + (a.y-b.y)**2 + (a.z-b.z)**2);
          const cb = Math.sqrt((c.x-b.x)**2 + (c.y-b.y)**2 + (c.z-b.z)**2);
          const ac = Math.sqrt((a.x-c.x)**2 + (a.y-c.y)**2 + (a.z-c.z)**2);
          return Math.acos((ab*ab + cb*cb - ac*ac)/(2*ab*cb)) * 180/Math.PI;
        }
        const leftKneeAngle = angle(leftHip, leftKnee, leftAnkle);
        const rightKneeAngle = angle(rightHip, rightKnee, rightAnkle);
        // Kiểm tra tay giơ lên (wrist cao hơn elbow, elbow cao hơn shoulder)
        const rightShoulder = landmarks[idx.rightShoulder], rightElbow = landmarks[idx.rightElbow], rightWrist = landmarks[idx.rightWrist];
        const leftShoulder = landmarks[idx.leftShoulder], leftElbow = landmarks[idx.leftElbow], leftWrist = landmarks[idx.leftWrist];
        const rightArmUp = rightWrist && rightElbow && rightShoulder && rightWrist.y < rightElbow.y && rightElbow.y < rightShoulder.y;
        const leftArmUp = leftWrist && leftElbow && leftShoulder && leftWrist.y < leftElbow.y && leftElbow.y < leftShoulder.y;
        // Điều kiện: ít nhất 1 tay giơ lên và ít nhất 1 gối gập
        return ((leftKneeAngle < 160 || rightKneeAngle < 160) && (rightArmUp || leftArmUp));
      }
      if (!isInShootingPose(landmarks)) {
        // Không gửi feedback nếu chưa vào tư thế ném
        sessionService.updateSession(socketId, { previousLandmarks: landmarks });
        return;
      }
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

    // --- Tính toán thông số cơ thể từ landmarks (nếu chưa có) ---
    if (!session.bodyProfile) {
      // Lấy index các điểm cần thiết theo MediaPipe
      const idx = {
        leftShoulder: 11, rightShoulder: 12,
        leftElbow: 13, rightElbow: 14,
        leftWrist: 15, rightWrist: 16,
        leftHip: 23, rightHip: 24,
        leftKnee: 25, rightKnee: 26,
        leftAnkle: 27, rightAnkle: 28
      };
      function dist(a, b) {
        if (!a || !b) return 0;
        return Math.sqrt((a.x-b.x)**2 + (a.y-b.y)**2 + (a.z-b.z)**2);
      }
      // Ưu tiên bên phải (thường là tay/chân thuận)
      const rightArm = dist(landmarks[idx.rightShoulder], landmarks[idx.rightElbow]) + dist(landmarks[idx.rightElbow], landmarks[idx.rightWrist]);
      const leftArm = dist(landmarks[idx.leftShoulder], landmarks[idx.leftElbow]) + dist(landmarks[idx.leftElbow], landmarks[idx.leftWrist]);
      const rightLeg = dist(landmarks[idx.rightHip], landmarks[idx.rightKnee]) + dist(landmarks[idx.rightKnee], landmarks[idx.rightAnkle]);
      const leftLeg = dist(landmarks[idx.leftHip], landmarks[idx.leftKnee]) + dist(landmarks[idx.leftKnee], landmarks[idx.leftAnkle]);
      // Lưu vào session
      sessionService.updateSession(socketId, {
        bodyProfile: {
          rightArm, leftArm, rightLeg, leftLeg,
          avgArm: (rightArm + leftArm) / 2,
          avgLeg: (rightLeg + leftLeg) / 2
        }
      });
      // Log để kiểm tra
      console.log('[BodyProfile] Calculated for', socketId, sessionService.getSession(socketId).bodyProfile);
    }

    // Kiểm tra có người trong khung hình không (ít nhất 5 điểm có visibility >= 0.5)
    const validPoints = Array.isArray(landmarks)
      ? landmarks.filter(pt => pt && typeof pt.x === 'number' && typeof pt.y === 'number' && typeof pt.z === 'number' && (pt.visibility === undefined || pt.visibility >= 0.5))
      : [];
    if (validPoints.length < 5) {
      emitCallback('audio_feedback', {
        text: 'No person detected',
        audioBase64: '',
        angles: {},
        metadata: { timestamp: Date.now() }
      });
      sessionService.updateSession(socketId, { previousLandmarks: landmarks });
      return;
    }

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
        { joint: 'knee', data: evalResult.kneeEval, side: evalResult.kneeEval?.side },
        { joint: 'elbow', data: evalResult.elbowEval, side: evalResult.elbowEval?.side },
        { joint: 'shoulder', data: evalResult.shoulderEval, side: evalResult.shoulderEval?.side }
      ];

      // Tìm lỗi ĐẦU TIÊN xuất hiện trong danh sách ưu tiên
      const primaryIssue = errorPriority.find(
        item => item.data && item.data.status && item.data.status !== 'perfect'
      );

      if (primaryIssue) {
        // Nếu có lỗi, chỉ lấy đúng 1 câu feedback của lỗi đó
        let baseMsg = getRandomFeedback(primaryIssue.joint, primaryIssue.data.status);
        // Bổ sung chi tiết bên trái/phải nếu có
        let side = primaryIssue.data?.side || primaryIssue.side;
        if (!side && evalResult.shootingHand) {
          // Ưu tiên tay thuận nếu không có side
          side = evalResult.shootingHand === 'right' ? 'right' : 'left';
        }
        if (side && baseMsg && /elbow|shoulder|knee/i.test(primaryIssue.joint)) {
          // Thay thế "your elbow" thành "your right/left elbow" nếu có
          baseMsg = baseMsg.replace(/your (elbow|shoulder|knee)/i, `your ${side} $1`);
          // Nếu không có "your ...", thêm vào đầu câu
          if (!/your (right|left) (elbow|shoulder|knee)/i.test(baseMsg)) {
            baseMsg = `${baseMsg.charAt(0).toUpperCase() + baseMsg.slice(1)}`;
          }
        }
        message = baseMsg;
      } else {
        // Nếu không tìm thấy lỗi nào (tất cả đều perfect)
        message = '';
      }

      if (message) {
        // Gọi TTS
        const ttsResult = await synthesizeSpeech(message, 'coach');
        // Tính toán Dynamic Cooldown dựa trên độ dài chuỗi trả về
        const estimatedAudioDuration = (message.length * 80) + 1500; 
        const cooldownToUse = Math.max(CONFIG.MIN_FEEDBACK_INTERVAL, estimatedAudioDuration);
        emitCallback('audio_feedback', {
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
      }

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