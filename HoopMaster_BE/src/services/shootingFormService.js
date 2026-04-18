
// Generic Pose Evaluation Service
const { POSE_RULES, evaluateAngle, detectShootingHand, getRelevantLandmarks } = require('../models/ruleModel');


/**
 * Đánh giá pose cho mọi bài tập (bao gồm cả shooting)
 * @param {string} exerciseType - Tên bài tập ("shooting", "push_up", ...)
 * @param {Array} landmarks - Landmarks từ MediaPipe
 * @returns {Object} Đánh giá pose
 */
async function evaluatePose(exerciseType, landmarks) {
  if (exerciseType === 'shooting') {
    // Giữ nguyên logic shooting cũ
    const { calculateAngle3D, detectViewOrientation } = require('./poseService');
    const shootingHand = detectShootingHand(landmarks);
    const viewOrientation = detectViewOrientation(
      landmarks[11], // leftShoulder
      landmarks[12]  // rightShoulder
    );
    const relevantLandmarks = getRelevantLandmarks(landmarks, shootingHand);
    const rules = POSE_RULES.shooting;
    const elbowAngle = calculateAngle3D(
      relevantLandmarks.shoulder,
      relevantLandmarks.elbow,
      relevantLandmarks.wrist
    );
    const kneeAngle = calculateAngle3D(
      relevantLandmarks.hip,
      relevantLandmarks.knee,
      relevantLandmarks.ankle
    );
    const shoulderAngle = calculateAngle3D(
      relevantLandmarks.hip,
      relevantLandmarks.shoulder,
      relevantLandmarks.elbow
    );
    const elbowEval = elbowAngle ? evaluateAngle(elbowAngle, rules.elbow) : null;
    const kneeEval = kneeAngle ? evaluateAngle(kneeAngle, rules.knee) : null;
    const shoulderEval = shoulderAngle ? evaluateAngle(shoulderAngle, rules.shoulder) : null;
    return {
      elbowEval,
      kneeEval,
      shoulderEval,
      shootingHand,
      viewOrientation,
      elbowAngle,
      kneeAngle,
      shoulderAngle
    };
  }
  // Đánh giá chung cho các bài tập khác
  const rules = POSE_RULES[exerciseType];
  if (!rules) return { error: 'No rules for this exercise' };
  const { calculateAngle3D } = require('./poseService');
  const result = {};
  // Có thể mở rộng lấy relevantLandmarks theo từng bài tập
  const relevantLandmarks = getRelevantLandmarks(landmarks, 'right'); // default right
  for (const key in rules) {
    const rule = rules[key];
    let angle = null;
    if (rule.landmarks && relevantLandmarks[key]) {
      const { point1, point2, point3 } = rule.landmarks;
      angle = calculateAngle3D(
        relevantLandmarks[point1] || relevantLandmarks[key],
        relevantLandmarks[point2] || relevantLandmarks[key],
        relevantLandmarks[point3] || relevantLandmarks[key]
      );
    }
    result[key] = angle ? evaluateAngle(angle, rule) : null;
  }
  return result;
}

module.exports = {
  evaluatePose
};
