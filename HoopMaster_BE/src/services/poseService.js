// Đánh giá pose tổng quát cho mọi bài tập
const { evaluatePose } = require('./shootingFormService');

// Đảm bảo export là async function và không ghi đè các hàm khác
module.exports = {
  calculateAngle3D,
  calculateAngle2D,
  calculateDistance,
  detectViewOrientation,
  isValidPoint,
  normalizeLandmarks,
  isPoseStable,
  calculateMultipleAngles,
  evaluatePose: async (...args) => evaluatePose(...args)
};
/**
 * Pose Analysis Service
 * Xử lý các phép tính toán học liên quan đến pose detection:
 * - Tính góc giữa 3 điểm trong không gian 3D
 * - Phát hiện góc nhìn (Side View vs Front View)
 * - Tính khoảng cách Euclidean
 */

/**
 * Tính góc giữa 3 điểm trong không gian 3D (sử dụng công thức vector)
 * 
 * Công thức:
 * 1. Tạo 2 vector: BA = A - B, BC = C - B (B là điểm góc)
 * 2. Tính tích vô hướng (dot product): BA · BC = |BA| × |BC| × cos(θ)
 * 3. θ = arccos((BA · BC) / (|BA| × |BC|))
 * 
 * @param {object} point1 - Điểm thứ nhất {x, y, z}
 * @param {object} point2 - Điểm góc (vertex) {x, y, z}
 * @param {object} point3 - Điểm thứ ba {x, y, z}
 * @returns {number} Góc tính bằng độ (degrees), hoặc null nếu không hợp lệ
 */
function calculateAngle3D(point1, point2, point3) {
  // Kiểm tra tính hợp lệ của input
  if (!point1 || !point2 || !point3) {
    console.warn('[PoseService] Missing points for angle calculation');
    return null;
  }

  if (!isValidPoint(point1) || !isValidPoint(point2) || !isValidPoint(point3)) {
    console.warn('[PoseService] Invalid point coordinates');
    return null;
  }

  try {
    // Tạo 2 vector từ điểm góc (point2)
    const vector1 = {
      x: point1.x - point2.x,
      y: point1.y - point2.y,
      z: point1.z - point2.z
    };

    const vector2 = {
      x: point3.x - point2.x,
      y: point3.y - point2.y,
      z: point3.z - point2.z
    };

    // Tính tích vô hướng (dot product)
    const dotProduct = 
      vector1.x * vector2.x + 
      vector1.y * vector2.y + 
      vector1.z * vector2.z;

    // Tính độ dài (magnitude) của mỗi vector
    const magnitude1 = Math.sqrt(
      vector1.x ** 2 + 
      vector1.y ** 2 + 
      vector1.z ** 2
    );

    const magnitude2 = Math.sqrt(
      vector2.x ** 2 + 
      vector2.y ** 2 + 
      vector2.z ** 2
    );

    // Tránh chia cho 0
    if (magnitude1 === 0 || magnitude2 === 0) {
      console.warn('[PoseService] Zero magnitude vector detected');
      return null;
    }

    // Tính cosine của góc
    let cosineAngle = dotProduct / (magnitude1 * magnitude2);

    // Clamp giá trị về khoảng [-1, 1] để tránh lỗi arccos
    // (do sai số floating-point có thể cho giá trị ngoài khoảng này)
    cosineAngle = Math.max(-1, Math.min(1, cosineAngle));

    // Chuyển đổi từ radian sang độ
    const angleRadians = Math.acos(cosineAngle);
    const angleDegrees = angleRadians * (180 / Math.PI);

    return Math.round(angleDegrees * 100) / 100; // Làm tròn 2 chữ số thập phân
  } catch (error) {
    console.error('[PoseService] Error calculating angle:', error.message);
    return null;
  }
}

/**
 * Tính góc 2D đơn giản (chỉ dùng x, y - bỏ qua z)
 * Hữu ích khi camera không cung cấp depth hoặc góc nhìn thẳng (front view)
 * 
 * @param {object} point1 - {x, y}
 * @param {object} point2 - Điểm góc {x, y}
 * @param {object} point3 - {x, y}
 * @returns {number} Góc tính bằng độ
 */
function calculateAngle2D(point1, point2, point3) {
  if (!point1 || !point2 || !point3) return null;

  try {
    // Tính góc sử dụng atan2 (phương pháp đơn giản hơn cho 2D)
    const angle1 = Math.atan2(point1.y - point2.y, point1.x - point2.x);
    const angle2 = Math.atan2(point3.y - point2.y, point3.x - point2.x);
    
    let angleDiff = Math.abs(angle1 - angle2);
    
    // Chuyển đổi sang độ
    angleDiff = angleDiff * (180 / Math.PI);
    
    // Đảm bảo góc luôn <= 180 độ
    if (angleDiff > 180) {
      angleDiff = 360 - angleDiff;
    }
    
    return Math.round(angleDiff * 100) / 100;
  } catch (error) {
    console.error('[PoseService] Error calculating 2D angle:', error.message);
    return null;
  }
}

/**
 * Kiểm tra tính hợp lệ của một điểm landmark
 * @param {object} point - {x, y, z, visibility?}
 * @returns {boolean}
 */
function isValidPoint(point) {
  if (!point) return false;
  
  // Kiểm tra x, y, z có phải số hợp lệ không
  if (typeof point.x !== 'number' || isNaN(point.x)) return false;
  if (typeof point.y !== 'number' || isNaN(point.y)) return false;
  if (typeof point.z !== 'number' || isNaN(point.z)) return false;
  
  // Kiểm tra visibility nếu có (MediaPipe cung cấp confidence score)
  if (point.visibility !== undefined && point.visibility < 0.5) {
    return false; // Điểm có confidence thấp
  }
  
  return true;
}

/**
 * Phát hiện góc nhìn: Side View (nhìn nghiêng) hay Front View (nhìn trực diện)
 * 
 * Cách hoạt động:
 * - Nếu 2 vai cách xa nhau theo trục X → Front View (người quay mặt về camera)
 * - Nếu 2 vai gần nhau theo trục X → Side View (người đứng nghiêng)
 * 
 * @param {object} leftShoulder - Vai trái {x, y, z}
 * @param {object} rightShoulder - Vai phải {x, y, z}
 * @returns {string} 'side' | 'front' | 'unknown'
 */
function detectViewOrientation(leftShoulder, rightShoulder) {
  if (!isValidPoint(leftShoulder) || !isValidPoint(rightShoulder)) {
    return 'unknown';
  }

  try {
    // Tính khoảng cách theo trục X giữa 2 vai
    const shoulderDistanceX = Math.abs(leftShoulder.x - rightShoulder.x);
    
    // Tính khoảng cách Euclidean tổng thể
    const shoulderDistanceTotal = calculateDistance(leftShoulder, rightShoulder);
    
    // Nếu khoảng cách X chiếm > 60% tổng khoảng cách → Front View
    // Ngưỡng này có thể điều chỉnh tùy vào setup camera
    const threshold = 0.6;
    const ratio = shoulderDistanceX / shoulderDistanceTotal;
    
    if (ratio > threshold) {
      return 'front'; // Người dùng quay mặt về camera
    } else {
      return 'side';  // Người dùng đứng nghiêng
    }
  } catch (error) {
    console.error('[PoseService] Error detecting view orientation:', error.message);
    return 'unknown';
  }
}

/**
 * Tính khoảng cách Euclidean giữa 2 điểm trong không gian 3D
 * @param {object} point1 - {x, y, z}
 * @param {object} point2 - {x, y, z}
 * @returns {number} Khoảng cách
 */
function calculateDistance(point1, point2) {
  if (!point1 || !point2) return 0;

  const dx = point1.x - point2.x;
  const dy = point1.y - point2.y;
  const dz = point1.z - point2.z;

  return Math.sqrt(dx ** 2 + dy ** 2 + dz ** 2);
}

/**
 * Normalize landmarks về tỉ lệ chuẩn (để không phụ thuộc vào kích thước người)
 * Chuẩn hóa dựa trên khoảng cách giữa 2 vai
 * 
 * @param {array} landmarks - Mảng landmarks từ MediaPipe
 * @returns {array} Landmarks đã normalize
 */
function normalizeLandmarks(landmarks, leftShoulderIndex = 11, rightShoulderIndex = 12) {
  if (!landmarks || landmarks.length === 0) return landmarks;

  try {
    const leftShoulder = landmarks[leftShoulderIndex];
    const rightShoulder = landmarks[rightShoulderIndex];
    
    if (!leftShoulder || !rightShoulder) {
      return landmarks; // Không thể normalize, trả về gốc
    }

    // Tính khoảng cách vai làm reference
    const shoulderDistance = calculateDistance(leftShoulder, rightShoulder);
    
    if (shoulderDistance === 0) {
      return landmarks;
    }

    // Normalize mỗi landmark
    return landmarks.map(landmark => {
      if (!landmark) return landmark;
      
      return {
        x: landmark.x / shoulderDistance,
        y: landmark.y / shoulderDistance,
        z: landmark.z / shoulderDistance,
        visibility: landmark.visibility
      };
    });
  } catch (error) {
    console.error('[PoseService] Error normalizing landmarks:', error.message);
    return landmarks;
  }
}

/**
 * Tính độ ổn định của pose (so sánh với frame trước đó)
 * Trả về true nếu pose ổn định, false nếu đang chuyển động quá nhanh
 * 
 * @param {array} currentLandmarks - Landmarks frame hiện tại
 * @param {array} previousLandmarks - Landmarks frame trước
 * @param {number} threshold - Ngưỡng chấp nhận (default: 0.05)
 * @returns {boolean}
 */
function isPoseStable(currentLandmarks, previousLandmarks, threshold = 0.05) {
  if (!currentLandmarks || !previousLandmarks) return true;
  if (currentLandmarks.length !== previousLandmarks.length) return true;

  try {
    let totalMovement = 0;
    let validPoints = 0;

    for (let i = 0; i < currentLandmarks.length; i++) {
      const current = currentLandmarks[i];
      const previous = previousLandmarks[i];
      
      if (!current || !previous) continue;
      if (!isValidPoint(current) || !isValidPoint(previous)) continue;

      const distance = calculateDistance(current, previous);
      totalMovement += distance;
      validPoints++;
    }

    if (validPoints === 0) return true;

    const averageMovement = totalMovement / validPoints;
    return averageMovement < threshold; // Pose ổn định nếu di chuyển < threshold
  } catch (error) {
    console.error('[PoseService] Error checking pose stability:', error.message);
    return true; // Default to stable
  }
}

/**
 * Batch calculate nhiều góc cùng lúc
 * @param {object} landmarks - Tất cả landmarks
 * @param {array} angleConfigs - Mảng config [{point1Index, point2Index, point3Index, name}]
 * @returns {object} { angleName: angleValue }
 */
function calculateMultipleAngles(landmarks, angleConfigs) {
  const results = {};

  angleConfigs.forEach(config => {
    const { point1Index, point2Index, point3Index, name } = config;
    
    const point1 = landmarks[point1Index];
    const point2 = landmarks[point2Index];
    const point3 = landmarks[point3Index];

    results[name] = calculateAngle3D(point1, point2, point3);
  });

  return results;
}

module.exports = {
  calculateAngle3D,
  calculateAngle2D,
  calculateDistance,
  detectViewOrientation,
  isValidPoint,
  normalizeLandmarks,
  isPoseStable,
  calculateMultipleAngles,
  evaluatePose: async (...args) => require('./shootingFormService').evaluatePose(...args)
};