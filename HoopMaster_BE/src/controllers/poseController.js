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

const sessionService = require('../services/sessionService');
const { enqueueAudioInstruction } = require('../services/audioInstructionQueueService');
const {
  updateShotState,
  isInShootingPose,
  shouldAllowPositiveRealtimeFeedback,
  shouldRunPostShotAnalysis
} = require('../services/shotReadinessService');

// Cấu hình kiểm soát feedback
const CONFIG = {
  MIN_FEEDBACK_INTERVAL: 2500,     // Realtime hơn nhưng vẫn tránh spam
  STABILITY_THRESHOLD: 0.01,       // Ngưỡng ổn định của pose
  ANGLE_CHANGE_THRESHOLD: (POSE_RULES?.shooting?.elbow?.threshold || 12),
  REMINDER_TIME_MS: 50000,
  ISSUE_CONFIRM_FRAMES: 2,         // Cần lặp cùng lỗi qua nhiều frame mới nhắc
  ISSUE_FLIP_COOLDOWN_MS: 9000,    // Tránh đảo low/high liên tục trên cùng joint
  SAME_JOINT_STREAK_LIMIT: 2,      // Hạn chế lặp elbow quá nhiều
  MAX_CORRECTION_STREAK: 4         // Tối đa số lần correction liên tiếp trước khi cho ném
};

const SHOOTING_POSE_PROMPT = {
  COOLDOWN_MS: 7000,
  MAX_CONSECUTIVE_PROMPTS: 4,
  GRACE_WINDOW_MS: 12000
};

/**
 * Lấy feedback ngẫu nhiên nhưng ổn định
 */
function normalizeCoachTone(tone) {
  return ['strict', 'cheerful', 'neutral'].includes(tone) ? tone : 'neutral';
}

function getRandomFeedback(joint, status, tone = 'neutral') {
  const map = {
    tooLow: 'low',
    acceptable_low: 'low',
    tooHigh: 'high',
    acceptable_high: 'high',
    perfect: 'perfect'
  };
  const key = map[status] || 'perfect';
  const selectedTone = normalizeCoachTone(tone);
  // Ưu tiên lấy neutral nếu có, nếu không thì lấy strict/cheerful bất kỳ
  const jointFeedback = poseFeedback[joint]?.feedback?.[key];
  if (jointFeedback) {
    if (Array.isArray(jointFeedback) && jointFeedback.length > 0) {
      return jointFeedback[Math.floor(Math.random() * jointFeedback.length)];
    }
    if (Array.isArray(jointFeedback[selectedTone]) && jointFeedback[selectedTone].length > 0) {
      return jointFeedback[selectedTone][Math.floor(Math.random() * jointFeedback[selectedTone].length)];
    }
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

function getRandomToneFeedback(group, tone = 'neutral') {
  const selectedTone = normalizeCoachTone(tone);
  const toneMessages = group?.[selectedTone] || group?.neutral;
  if (Array.isArray(toneMessages) && toneMessages.length > 0) {
    return toneMessages[Math.floor(Math.random() * toneMessages.length)];
  }

  const allMessages = Object.values(group || {}).flat().filter(Boolean);
  if (allMessages.length > 0) {
    return allMessages[Math.floor(Math.random() * allMessages.length)];
  }

  return '';
}

function isActionableIssue(status) {
  return ['tooLow', 'tooHigh', 'acceptable_low', 'acceptable_high'].includes(status);
}

function normalizeIssueDirection(status = '') {
  if (['tooLow', 'acceptable_low'].includes(status)) return 'low';
  if (['tooHigh', 'acceptable_high'].includes(status)) return 'high';
  return 'perfect';
}

function choosePrimaryIssueWithAntiSpam(evalResult, session) {
  const candidates = [
    { joint: 'knee', data: evalResult.kneeEval, side: evalResult.kneeEval?.side },
    { joint: 'elbow', data: evalResult.elbowEval, side: evalResult.elbowEval?.side },
    { joint: 'shoulder', data: evalResult.shoulderEval, side: evalResult.shoulderEval?.side }
  ].filter(item => item.data && isActionableIssue(item.data.status));

  if (candidates.length === 0) return null;

  const recentJoints = Array.isArray(session.recentIssueJoints) ? session.recentIssueJoints : [];
  const repeatedJoint = recentJoints.length >= CONFIG.SAME_JOINT_STREAK_LIMIT
    && recentJoints.slice(-CONFIG.SAME_JOINT_STREAK_LIMIT).every(j => j === recentJoints[recentJoints.length - 1]);

  if (repeatedJoint) {
    const avoidJoint = recentJoints[recentJoints.length - 1];
    const alternate = candidates.find(item => item.joint !== avoidJoint);
    if (alternate) return alternate;
  }

  return candidates[0];
}

function getFormReadyFeedback(tone = 'neutral') {
  const selectedTone = normalizeCoachTone(tone);
  const preferred = poseFeedback.shoulder?.feedback?.perfect?.[selectedTone]
    || poseFeedback.shoulder?.feedback?.perfect?.neutral
    || poseFeedback.knee?.feedback?.perfect?.[selectedTone]
    || poseFeedback.elbow?.feedback?.perfect?.[selectedTone];

  if (Array.isArray(preferred) && preferred.length > 0) {
    return preferred[0];
  }

  return 'Good form. Hold it and shoot when ready.';
}

/**
 * REAL-TIME WORKFLOW
 */
async function handleRealtimePoseAnalysis(socketId, poseData, emitCallback) {
  const { landmarks } = poseData || {};
  let session = sessionService.getSession(socketId);
  if (!session) return;
  const coachTone = normalizeCoachTone(poseData?.tone || session.coachTone || 'neutral');
  const now = Date.now();
  const previousShotPhase = session.shotState?.phase || 'not_ready';
  const shotState = updateShotState(session.shotState, landmarks, now);
  session = sessionService.updateSession(socketId, { shotState, coachTone });

  // Auto-detect shot release from pose data and trigger post-shot analysis
  // Guard: cooldown 2 giây giữa các lần đếm shoot để tránh đếm trùng
  const SHOT_COOLDOWN_MS = 2000;
  const lastShotCountedAt = session.lastShotCountedAt || 0;
  if (shotState.phase === 'released' && previousShotPhase === 'set' && !session.pendingPostShotAnalysis && (now - lastShotCountedAt > SHOT_COOLDOWN_MS)) {
    sessionService.updateSession(socketId, { pendingPostShotAnalysis: true, lastShotCountedAt: now });
    // Fire and forget - post-shot analysis runs independently
    handlePostShotAnalysis(socketId, emitCallback).finally(() => {
      const s = sessionService.getSession(socketId);
      if (s) {
        sessionService.updateSession(socketId, { pendingPostShotAnalysis: false });
      }
    });
  }

  // Chặn frame mới trong lúc đang tạo/gửi feedback trước đó, kể cả frame chưa vào pose.
  if (session.isProcessingRealtimeFeedback) return;

      const shootingPose = isInShootingPose(landmarks);
      const graceUntil = session.shootingPoseGraceUntil || 0;
      // Đảm bảo chỉ kiểm tra cooldown 1 lần duy nhất
      if (!shootingPose.ready && now >= graceUntil) {
        const lastPromptTime = session.lastShootingPosePromptTime || 0;
        
        if (Array.isArray(landmarks) && landmarks.length > 0 && (now - lastPromptTime > SHOOTING_POSE_PROMPT.COOLDOWN_MS)) {
          const promptCount = (session.shootingPosePromptCount || 0) + 1;
          const forceGrace = promptCount >= SHOOTING_POSE_PROMPT.MAX_CONSECUTIVE_PROMPTS;
          
          // 1. CẬP NHẬT COOLDOWN NGAY LẬP TỨC ĐỂ CHẶN CÁC FRAME TIẾP THEO
          sessionService.updateSession(socketId, {
            lastShootingPosePromptTime: now,
            shootingPosePromptCount: forceGrace ? 0 : promptCount,
            shootingPoseGraceUntil: forceGrace ? now + SHOOTING_POSE_PROMPT.GRACE_WINDOW_MS : 0
          });

          const message = forceGrace
            ? 'Good enough. Keep this pose steady and focus on elbow, knee, and shoulder alignment.'
            : 'Raise your shooting wrist slightly above your elbow and keep it near shoulder level.';
          
          await enqueueAudioInstruction(socketId, {
            type: forceGrace ? 'shooting_pose_grace' : 'shooting_pose_prompt',
            text: message,
            tone: coachTone,
            angles: {},
            priority: 'low',
            dedupeKey: forceGrace ? 'shooting_pose_grace' : 'shooting_pose_prompt',
            metadata: {
              timestamp: now,
              reason: forceGrace ? 'shooting_pose_grace_window' : 'not_in_shooting_pose',
              poseReason: shootingPose.reason || 'arm_not_ready',
              poseMetrics: shootingPose.metrics || {}
            }
          }, emitCallback);
        }
        
        // Luôn lưu landmarks để check ổn định cho các frame sau
        sessionService.updateSession(socketId, { previousLandmarks: landmarks, coachTone });
        return;
      }
      if (shootingPose.ready) {
        sessionService.updateSession(socketId, {
          shootingPosePromptCount: 0,
          shootingPoseGraceUntil: 0
        });
      }

  const lastFeedbackTime = session.lastFeedback?.time || 0;
  
  try {
    sessionService.updateSession(socketId, {
      isProcessingRealtimeFeedback: true,
      coachTone,
      hasPromptedForShootingPose: false
    });
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
      // Đếm số lần feedback lỗi liên tiếp
      let errorCount = session.errorFeedbackCount || 0;
      let lastErrorStatus = session.lastErrorStatus || '';
      let correctionStreakCount = session.correctionStreakCount || 0;
      let message = '';
      const primaryIssue = choosePrimaryIssueWithAntiSpam(evalResult, session);
      let isForcePraise = false;
      if (primaryIssue) {
        const thisDirection = normalizeIssueDirection(primaryIssue.data.status);
        // Nếu lỗi giống lần trước thì tăng count, nếu khác thì reset count
        const thisErrorStatus = `${primaryIssue.joint}:${primaryIssue.data.status}`;
        if (thisErrorStatus === lastErrorStatus) {
          errorCount += 1;
        } else {
          errorCount = 1;
        }
        const previousDirection = normalizeIssueDirection(session.lastIssueDirection);
        const sameJointAsLast = session.lastIssueJoint === primaryIssue.joint;
        const flippedDirection = sameJointAsLast && previousDirection !== 'perfect' && previousDirection !== thisDirection;
        const recentlyFlipped = flippedDirection && (now - (session.lastIssueTime || 0) < CONFIG.ISSUE_FLIP_COOLDOWN_MS);
        if (recentlyFlipped) {
          sessionService.updateSession(socketId, { previousLandmarks: landmarks });
          return;
        }

        const pendingIssueKey = session.pendingIssueKey || '';
        const pendingIssueCount = (pendingIssueKey === thisErrorStatus)
          ? (session.pendingIssueCount || 0) + 1
          : 1;
        sessionService.updateSession(socketId, {
          pendingIssueKey: thisErrorStatus,
          pendingIssueCount
        });
        if (pendingIssueCount < CONFIG.ISSUE_CONFIRM_FRAMES) {
          sessionService.updateSession(socketId, { previousLandmarks: landmarks });
          return;
        }
        correctionStreakCount += 1;

        // Nếu lỗi lặp lại quá ngưỡng thì khen và cho ném để tránh "kẹt" correction vô hạn
        if (errorCount >= 3 || correctionStreakCount >= CONFIG.MAX_CORRECTION_STREAK) {
          isForcePraise = true;
          message = getFormReadyFeedback(coachTone);
          correctionStreakCount = 0;
          errorCount = 0;
        } else {
          let baseMsg = getRandomFeedback(primaryIssue.joint, primaryIssue.data.status, coachTone);
          let side = primaryIssue.data?.side || primaryIssue.side;
          if (!side && evalResult.shootingHand && primaryIssue.joint !== 'knee') {
            side = evalResult.shootingHand === 'right' ? 'right' : 'left';
          }
          if (side && baseMsg && /elbow|shoulder/i.test(primaryIssue.joint)) {
            baseMsg = baseMsg.replace(/your (elbow|shoulder)\b/i, `your ${side} $1`);
            if (!/your (right|left) (elbow|shoulder)\b/i.test(baseMsg)) {
              baseMsg = `${baseMsg.charAt(0).toUpperCase() + baseMsg.slice(1)}`;
            }
          }
          message = baseMsg;
        }
        // Lưu trạng thái lỗi hiện tại
        sessionService.updateSession(socketId, {
          errorFeedbackCount: errorCount,
          lastErrorStatus: thisErrorStatus,
          lastIssueJoint: primaryIssue.joint,
          lastIssueDirection: thisDirection,
          lastIssueTime: now,
          correctionStreakCount
        });
      } else {
        // Nếu không còn lỗi, reset biến đếm
        errorCount = 0;
        sessionService.updateSession(socketId, {
          errorFeedbackCount: 0,
          lastErrorStatus: '',
          pendingIssueKey: '',
          pendingIssueCount: 0,
          lastIssueDirection: 'perfect',
          correctionStreakCount: 0
        });
        if (shouldAllowPositiveRealtimeFeedback(shotState, evalResult, now)) {
          message = getFormReadyFeedback(coachTone);
        }
      }
      if (message) {
        const estimatedAudioDuration = (message.length * 80) + 1500; 
        const cooldownToUse = Math.max(CONFIG.MIN_FEEDBACK_INTERVAL, estimatedAudioDuration);
        await enqueueAudioInstruction(socketId, {
          type: (primaryIssue && !isForcePraise) ? 'form_correction' : 'form_ready',
          text: message,
          tone: coachTone,
          angles: evalResult,
          priority: (primaryIssue && !isForcePraise) ? 'normal' : 'low',
          dedupeKey: `${primaryIssue?.joint || 'good'}:${currentPoseStatus}:${message}`,
          metadata: {
            timestamp: now,
            poseStatus: currentPoseStatus,
            shotPhase: shotState.phase
          }
        }, emitCallback);
        const shotStateUpdate = (primaryIssue && !isForcePraise)
          ? shotState
          : { ...shotState, lastFormReadyAt: now };
        sessionService.updateSession(socketId, {
          recentIssueJoints: primaryIssue
            ? [...(Array.isArray(session.recentIssueJoints) ? session.recentIssueJoints : []), primaryIssue.joint].slice(-4)
            : (Array.isArray(session.recentIssueJoints) ? session.recentIssueJoints.slice(-4) : []),
          shotState: shotStateUpdate,
          lastFeedback: { 
            angles: evalResult, 
            time: now, 
            message: message,
            poseStatus: currentPoseStatus,
            cooldown: cooldownToUse
          },
          previousLandmarks: landmarks
        });
      } else {
        sessionService.updateSession(socketId, { previousLandmarks: landmarks });
      }
    } else {
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
 * Chỉ đếm shot và tính thống kê, KHÔNG gọi LLM
 * LLM session summary được gọi khi session kết thúc
 */
async function handlePostShotAnalysis(socketId, emitCallback) {
  try {
    const session = sessionService.getSession(socketId);
    const now = Date.now();
    sessionService.incrementStats(socketId, 'shotsCompleted');
    let stats = {};
    if (session && session.frameBuffer.length > 0) {
      stats = calculateShotStatistics(session.frameBuffer);
      // Lưu thống kê các shot để dùng cho session summary
      const allShotStats = session.allShotStats || [];
      allShotStats.push(stats);
      sessionService.clearFrameBuffer(socketId);
      sessionService.updateSession(socketId, {
        shotInProgress: false,
        lastFeedback: null,
        previousLandmarks: null,
        allShotStats,
        shotState: {
          ...session.shotState,
          phase: 'not_ready',
          enteredPhaseAt: now,
          lastReleaseAt: null,
          lastFormReadyAt: null,
          reason: 'post_shot_complete'
        }
      });
    }
    const updatedSession = sessionService.getSession(socketId);
    emitCallback('shot_count_update', {
      shotCount: updatedSession?.sessionStats?.shotsCompleted || 0,
      timestamp: new Date().toISOString(),
      stats
    });
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
