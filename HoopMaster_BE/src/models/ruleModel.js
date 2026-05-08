/**
 * Basketball Shooting Form Rules Model
 * Chứa các quy tắc góc độ chuẩn cho động tác ném bóng rổ
 * và ánh xạ Intent cho TTS (giọng điệu phản hồi)
 */

/**
 * Các góc độ chuẩn cho động tác ném bóng rổ (Side View - nhìn nghiêng)
 * Dựa trên nghiên cứu biomechanics và huấn luyện chuyên nghiệp
 */
// POSE_RULES: Load từ file JSON
const fs = require('fs');
const path = require('path');
const poseRulesPath = path.join(__dirname, '../../data/pose_rules.json');
let POSE_RULES = {};
try {
  POSE_RULES = JSON.parse(fs.readFileSync(poseRulesPath, 'utf8'));
} catch (err) {
  console.error('[ruleModel] Failed to load pose_rules.json:', err.message);
  POSE_RULES = {};
}

/**
 * Cấu hình Tolerance (Độ chấp nhận sai số)
 * Nếu góc nằm trong khoảng [ideal - tolerance, ideal + tolerance] thì coi là hoàn hảo
 */
const ANGLE_TOLERANCE = parseInt(process.env.ANGLE_TOLERANCE) || 24;

/**
 * Intent Mapping cho TTS Service
 * Mỗi intent sẽ điều chỉnh pitch, rate, volume khác nhau
 */
const TTS_INTENTS = {
    strict: {
      description: 'Strict, concise, serious',
      ssmlModifiers: {
        pitch: '-8%',    // Giọng thấp, nghiêm túc
        rate: '1.05',    // Nhanh hơn một chút
        volume: 'medium'
      }
    },
    cheerful: {
      description: 'Cheerful, friendly, motivating',
      ssmlModifiers: {
        pitch: '+8%',    // Giọng cao, vui vẻ
        rate: '1.15',   // Nhanh, sôi động
        volume: 'loud'
      }
    },
    neutral: {
      description: 'Neutral, balanced',
      ssmlModifiers: {
        pitch: '0%',
        rate: '1.0',
        volume: 'medium'
      }
    },
  up: {
    description: 'Encouraging, positive tone',
    ssmlModifiers: {
      pitch: '+5%',    // Slightly higher
      rate: '1.1',     // Faster
      volume: 'loud'   // Louder
    }
  },
  down: {
    description: 'Gentle, calm tone',
    ssmlModifiers: {
      pitch: '-5%',    // Slightly lower
      rate: '0.9',     // Slower
      volume: 'medium' // Medium
    }
  },
  focus: {
    description: 'Focused, professional',
    ssmlModifiers: {
      pitch: '0%',     // Normal
      rate: '1.0',     // Standard speed
      volume: 'medium'
    }
  },
  neutral: {
    description: 'Neutral, no adjustment',
    ssmlModifiers: {
      pitch: '0%',
      rate: '1.0',
      volume: 'medium'
    }
  }
};

/**
 * Mapping MediaPipe Pose Landmarks sang tên dễ hiểu
 * MediaPipe Pose có 33 keypoints, nhưng chúng ta chỉ quan tâm một số điểm
 */
const MEDIAPIPE_LANDMARKS = {
  // Upper Body (Phần thân trên)
  leftShoulder: 11,
  rightShoulder: 12,
  leftElbow: 13,
  rightElbow: 14,
  leftWrist: 15,
  rightWrist: 16,
  
  // Lower Body (Phần thân dưới)
  leftHip: 23,
  rightHip: 24,
  leftKnee: 25,
  rightKnee: 26,
  leftAnkle: 27,
  rightAnkle: 28,
  
  // Reference points (Điểm tham chiếu)
  nose: 0,
  leftEye: 1,
  rightEye: 2
};

/**
 * Hàm kiểm tra góc có nằm trong khoảng chuẩn không
 * @param {number} angle - Góc hiện tại
 * @param {object} rule - Quy tắc từ SHOOTING_RULES
 * @returns {object} { status: 'perfect'|'tooLow'|'tooHigh', message: string, intent: string }
 */
function evaluateAngle(angle, rule) {
  const { min, max, ideal, feedbackMessages = {}, intent } = rule;
  // Đảm bảo feedbackMessages luôn có đủ key để tránh lỗi undefined
  const safeFeedback = {
    tooLow: feedbackMessages.tooLow || 'Angle too low',
    tooHigh: feedbackMessages.tooHigh || 'Angle too high',
    perfect: feedbackMessages.perfect || 'Perfect angle!'
  };
  
  // Kiểm tra góc có nằm trong khoảng cho phép không
  if (angle < min) {
    return {
      status: 'tooLow',
      message: safeFeedback.tooLow,
      intent: intent.tooLow,
      deviation: min - angle  // Độ lệch so với min
    };
  }

  if (angle > max) {
    return {
      status: 'tooHigh',
      message: safeFeedback.tooHigh,
      intent: intent.tooHigh,
      deviation: angle - max  // Độ lệch so với max
    };
  }

  // Kiểm tra có nằm trong khoảng hoàn hảo không (ideal ± tolerance)
  if (Math.abs(angle - ideal) <= ANGLE_TOLERANCE) {
    return {
      status: 'perfect',
      message: safeFeedback.perfect,
      intent: intent.perfect,
      deviation: 0
    };
  }

  // Nằm trong khoảng cho phép nhưng chưa hoàn hảo
  return {
    status: angle < ideal ? 'acceptable_low' : 'acceptable_high',
    message: angle < ideal ? safeFeedback.tooLow : safeFeedback.tooHigh,
    intent: angle < ideal ? intent.tooLow : intent.tooHigh,
    deviation: Math.abs(angle - ideal)
  };
}

/**
 * Xác định tay nào đang ném (Left/Right Hand)
 * Dựa vào tay nào có cổ tay cao hơn hoặc xa camera hơn
 * @param {object} landmarks - Tất cả landmarks từ MediaPipe
 * @returns {string} 'left' | 'right'
 */
function detectShootingHand(landmarks) {
  const leftWrist = landmarks[MEDIAPIPE_LANDMARKS.leftWrist];
  const rightWrist = landmarks[MEDIAPIPE_LANDMARKS.rightWrist];
  
  if (!leftWrist || !rightWrist) {
    return 'right'; // Default to right hand
  }
  
  // Tay nào cao hơn (y thấp hơn vì trục Y tăng xuống dưới)
  if (leftWrist.y < rightWrist.y) {
    return 'left';
  }
  
  return 'right';
}

/**
 * Lấy landmarks tương ứng với tay đang ném
 * @param {object} landmarks - Tất cả landmarks
 * @param {string} hand - 'left' | 'right'
 * @returns {object} { shoulder, elbow, wrist, hip, knee, ankle }
 */
function getRelevantLandmarks(landmarks, hand) {
  const isLeft = hand === 'left';
  
  return {
    shoulder: landmarks[isLeft ? MEDIAPIPE_LANDMARKS.leftShoulder : MEDIAPIPE_LANDMARKS.rightShoulder],
    elbow: landmarks[isLeft ? MEDIAPIPE_LANDMARKS.leftElbow : MEDIAPIPE_LANDMARKS.rightElbow],
    wrist: landmarks[isLeft ? MEDIAPIPE_LANDMARKS.leftWrist : MEDIAPIPE_LANDMARKS.rightWrist],
    hip: landmarks[isLeft ? MEDIAPIPE_LANDMARKS.leftHip : MEDIAPIPE_LANDMARKS.rightHip],
    knee: landmarks[isLeft ? MEDIAPIPE_LANDMARKS.leftKnee : MEDIAPIPE_LANDMARKS.rightKnee],
    ankle: landmarks[isLeft ? MEDIAPIPE_LANDMARKS.leftAnkle : MEDIAPIPE_LANDMARKS.rightAnkle]
  };
}

module.exports = {
  POSE_RULES,
  ANGLE_TOLERANCE,
  TTS_INTENTS,
  MEDIAPIPE_LANDMARKS,
  evaluateAngle,
  detectShootingHand,
  getRelevantLandmarks
};