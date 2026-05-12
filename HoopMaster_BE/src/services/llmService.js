/**
 * LLM Service - Gemini Flash 2.5 API Integration
 * Phân tích tổng kết cú ném dựa trên số liệu thống kê
 * Sinh ra lời khuyên chi tiết và có chiều sâu
 */


const { callMistralAPI } = require('./mistralService');
const fs = require('fs');
const path = require('path');

// Load feedback/instruction content from JSON
const feedbackData = JSON.parse(fs.readFileSync(path.join(__dirname, '../../data/pose_feedback.json'), 'utf8'));
const promptTemplate = JSON.parse(fs.readFileSync(path.join(__dirname, '../../data/pose_prompt_template.json'), 'utf8'));

/**
 * Tạo prompt cho LLM dựa trên dữ liệu cú ném
 * 
 * @param {object} shotData - Dữ liệu tổng hợp cú ném
 * @returns {string} Prompt đầy đủ
 */
function buildPrompt(shotData) {
  const {
    avgElbowAngle,
    avgKneeAngle,
    avgShoulderAngle,
    frameCount,
    shootingHand,
    viewOrientation,
    issues = [],
    tone = 'neutral'
  } = shotData;

  // Lấy instruction theo tone
  let instruction = feedbackData.instruction;
  if (typeof instruction === 'object' && instruction[tone]) {
    instruction = instruction[tone];
  } else if (typeof instruction === 'object') {
    instruction = instruction['neutral'];
  }

  // Prepare values for template
  const values = {
    shootingHand: shootingHand === 'left' ? 'Left' : 'Right',
    viewOrientation: viewOrientation === 'side' ? 'Side' : 'Front',
    frameCount,
    avgElbowAngle: avgElbowAngle ? `${avgElbowAngle.toFixed(1)}°` : 'N/A',
    avgKneeAngle: avgKneeAngle ? `${avgKneeAngle.toFixed(1)}°` : 'N/A',
    avgShoulderAngle: avgShoulderAngle ? `${avgShoulderAngle.toFixed(1)}°` : 'N/A',
    elbowIdeal: feedbackData.elbow.ideal,
    kneeIdeal: feedbackData.knee.ideal,
    shoulderIdeal: '30-120°',
    instruction: instruction,
    issues: issues.length > 0 ? `**Detected Issues:**\n${issues.map((issue, i) => `${i + 1}. ${issue}`).join('\n')}` : '',
    tone: tone
  };

  // Replace placeholders in template
  let prompt = promptTemplate.prompt;
  for (const key in values) {
    prompt = prompt.replace(new RegExp(`{${key}}`, 'g'), values[key]);
  }
  // Optionally, append tone instruction for LLM
  prompt += `\nCoach tone: ${tone}`;
  return prompt;
}

/**
 * Gọi Gemini Flash 2.5 API để sinh feedback
 * 
 * @param {object} shotData - Dữ liệu cú ném
 * @returns {Promise<object>} { success: boolean, feedback: string, metadata: object }
 */
async function generatePostShotFeedback(shotData) {
  const prompt = buildPrompt(shotData);
  const startTime = Date.now();
  try {
    const feedback = await callMistralAPI(prompt);
    const duration = Date.now() - startTime;
    return {
      success: true,
      feedback,
      metadata: {
        model: process.env.LLM_MODEL || 'mistral-small-latest',
        generationTime: duration,
        timestamp: new Date().toISOString()
      }
    };
  } catch (error) {
    console.error('[LLM] Mistral API call failed:', error.message);
    return generateFallbackFeedback(shotData);
  }
}

/**
 * Generate fallback feedback khi API không khả dụng
 * Sử dụng rule-based logic đơn giản
 * 
 * @param {object} shotData
 * @returns {object}
 */
function pickRandom(arrOrStr) {
  if (Array.isArray(arrOrStr)) {
    return arrOrStr[Math.floor(Math.random() * arrOrStr.length)];
  }
  return arrOrStr;
}

function generateFallbackFeedback(shotData) {
  const {
    avgElbowAngle,
    avgKneeAngle,
    avgBackAngle,
    releaseMotion,
    releasePoint,
    issues = [],
    tone = 'neutral'
  } = shotData;

  // fallback to neutral if invalid
  const validTones = ['strict', 'cheerful', 'neutral'];
  const selectedTone = validTones.includes(tone) ? tone : 'neutral';

  let feedbacks = [];
  // Elbow
  if (avgElbowAngle && avgElbowAngle < 80) {
    feedbacks.push(pickRandom(feedbackData.elbow.feedback.low[selectedTone]));
  } else if (avgElbowAngle && avgElbowAngle > 105) {
    feedbacks.push(pickRandom(feedbackData.elbow.feedback.high[selectedTone]));
  } else if (avgElbowAngle && avgElbowAngle >= 80 && avgElbowAngle <= 105) {
    feedbacks.push(pickRandom(feedbackData.elbow.feedback.perfect[selectedTone]));
  }
  // Knee
  if (avgKneeAngle && avgKneeAngle < 90) {
    feedbacks.push(pickRandom(feedbackData.knee.feedback.low[selectedTone]));
  } else if (avgKneeAngle && avgKneeAngle > 140) {
    feedbacks.push(pickRandom(feedbackData.knee.feedback.high[selectedTone]));
  } else if (avgKneeAngle && avgKneeAngle >= 90 && avgKneeAngle <= 140) {
    feedbacks.push(pickRandom(feedbackData.knee.feedback.perfect[selectedTone]));
  }
  // Back
  if (avgBackAngle && (avgBackAngle < 170 || avgBackAngle > 190)) {
    feedbacks.push(pickRandom(feedbackData.back.feedback.not_straight));
  } else if (avgBackAngle && avgBackAngle >= 170 && avgBackAngle <= 190) {
    feedbacks.push(pickRandom(feedbackData.back.feedback.good));
  }
  // Release motion
  if (releaseMotion === 'good') {
    feedbacks.push(pickRandom(feedbackData.release.motion.good));
  } else if (releaseMotion === 'bad') {
    feedbacks.push(pickRandom(feedbackData.release.motion.bad));
  }
  // Release point
  if (releasePoint === 'optimal') {
    feedbacks.push(pickRandom(feedbackData.release.point.optimal));
  } else if (releasePoint === 'low') {
    feedbacks.push(pickRandom(feedbackData.release.point.low));
  } else if (releasePoint === 'high') {
    feedbacks.push(pickRandom(feedbackData.release.point.high));
  }
  // General issues
  if (issues.length > 2) {
    feedbacks.push(pickRandom(feedbackData.general.multi_issue[selectedTone]));
  } else if (feedbacks.length === 0) {
    feedbacks.push(pickRandom(feedbackData.general.good[selectedTone]));
  }

  // Gộp feedback thành 1 đoạn
  const feedback = feedbacks.join(' ');
  return {
    success: true,
    feedback,
    metadata: {
      model: 'rule_based_fallback',
      generationTime: 0,
      timestamp: new Date().toISOString()
    }
  };
}

/**
 * Phân tích xu hướng từ nhiều cú ném
 * (Sử dụng cho session tracking)
 * 
 * @param {array} shotHistory - Mảng các shotData
 * @returns {Promise<object>}
 */
async function analyzeSessionTrends(shotHistory) {
  if (!shotHistory || shotHistory.length === 0) {
    return {
      success: false,
      message: 'No shot data available'
    };
  }

  // Tính toán thống kê
  const totalShots = shotHistory.length;
  const avgElbows = shotHistory.map(s => s.avgElbowAngle).filter(a => a !== null);
  const avgKnees = shotHistory.map(s => s.avgKneeAngle).filter(a => a !== null);

  const stats = {
    totalShots,
    avgElbowAngle: avgElbows.length > 0 ? 
      avgElbows.reduce((a, b) => a + b, 0) / avgElbows.length : null,
    avgKneeAngle: avgKnees.length > 0 ? 
      avgKnees.reduce((a, b) => a + b, 0) / avgKnees.length : null,
    consistency: calculateConsistency(avgElbows)
  };

  // Gemini không yêu cầu API key ở đây, chỉ fallback nếu lỗi
  const prompt = `You are a basketball coach. Analyze ${totalShots} shots in this training session:

**Statistics:**
- Average elbow angle: ${stats.avgElbowAngle?.toFixed(1)}°
- Average knee angle: ${stats.avgKneeAngle?.toFixed(1)}°
- Consistency: ${stats.consistency.toFixed(1)}%

Give an overall assessment (3-4 sentences) and a training plan for the next session in English.`;

  try {
    const feedback = await callGeminiAPI(prompt);
    return {
      success: true,
      feedback,
      stats
    };
  } catch (error) {
    console.error('[LLM] Session analysis failed:', error.message);
    return generateSessionFallback(stats);
  }
}

/**
 * Tính độ nhất quán (consistency) từ mảng góc độ
 * @param {array} angles - Mảng các góc độ
 * @returns {number} Phần trăm nhất quán (0-100)
 */
function calculateConsistency(angles) {
  if (!angles || angles.length < 2) return 100;

  const mean = angles.reduce((a, b) => a + b, 0) / angles.length;
  const variance = angles.reduce((sum, angle) => 
    sum + Math.pow(angle - mean, 2), 0) / angles.length;
  const stdDev = Math.sqrt(variance);

  // Chuyển đổi stdDev thành phần trăm (giả định stdDev < 20 là tốt)
  const consistencyScore = Math.max(0, 100 - (stdDev / 20) * 100);
  return consistencyScore;
}

/**
 * Fallback cho session analysis
 */
function generateSessionFallback(stats) {
  let feedback = feedbackData.session.summary.replace('{totalShots}', stats.totalShots);
  if (stats.consistency > 80) {
    feedback += pickRandom(feedbackData.session.consistency.high);
  } else if (stats.consistency > 60) {
    feedback += pickRandom(feedbackData.session.consistency.medium);
  } else {
    feedback += pickRandom(feedbackData.session.consistency.low);
  }
  return {
    success: true,
    feedback,
    stats,
    metadata: {
      model: 'rule_based_fallback'
    }
  };
}

/**
 * Validate LLM configuration
 */
function validateConfig() {
  const errors = [];

  if (!process.env.MISTRAL_API_KEY) {
    errors.push('MISTRAL_API_KEY not configured - will use fallback responses');
  }
  if (!process.env.LLM_MODEL) {
    errors.push('LLM_MODEL not set, using default: mistral-small-latest');
  }
  // Log giá trị thực tế của biến môi trường
  errors.push(`[DEBUG] MISTRAL_API_KEY: ${process.env.MISTRAL_API_KEY ? '[SET]' : '[NOT SET]'}`);
  errors.push(`[DEBUG] LLM_MODEL: ${process.env.LLM_MODEL || '[NOT SET]'}`);
  return {
    valid: errors.length === 0,
    errors,
    config: {
      model: process.env.LLM_MODEL || 'mistral-small-latest',
      maxTokens: 500,
      hasApiKey: !!process.env.MISTRAL_API_KEY
    }
  };
}

/**
 * Generate session summary feedback từ LLM
 * Phân tích tổng thể buổi tập: các lỗi còn mắc phải, điểm tốt, cần cải thiện gì
 */
async function generateSessionSummary(sessionStats) {
  const {
    totalShots,
    avgElbowAngle,
    avgKneeAngle,
    avgShoulderAngle,
    shootingHand
  } = sessionStats;

  const prompt = `You are an elite basketball coach reviewing a player's training session.

The player completed ${totalShots} shots in this session.

Session statistics:
- Average elbow angle: ${avgElbowAngle ? avgElbowAngle.toFixed(1) + '°' : 'N/A'} (ideal: 60-145°)
- Average knee angle: ${avgKneeAngle ? avgKneeAngle.toFixed(1) + '°' : 'N/A'} (ideal: 75-155°)
- Average shoulder angle: ${avgShoulderAngle ? avgShoulderAngle.toFixed(1) + '°' : 'N/A'} (ideal: 20-150°)
- Shooting hand: ${shootingHand || 'right'}

Write a session summary in Vietnamese. Include:
1. **Điểm tốt**: What the player did well (1-2 sentences)
2. **Lỗi còn mắc phải**: What needs improvement (1-2 sentences)
3. **Cần cải thiện**: Specific advice for next session (1-2 sentences)

Keep it encouraging but honest. Total 3-5 sentences, short and actionable.`;

  try {
    const feedback = await callMistralAPI(prompt);
    return {
      success: true,
      feedback,
      metadata: {
        model: process.env.LLM_MODEL || 'mistral-small-latest',
        timestamp: new Date().toISOString()
      }
    };
  } catch (error) {
    console.error('[LLM] Session summary API failed:', error.message);
    // Fallback: generate từ template
    return generateSessionFallback(sessionStats);
  }
}

module.exports = {
  generatePostShotFeedback,
  generateSessionSummary,
  analyzeSessionTrends,
  validateConfig,
  // Utilities
  buildPrompt,
  calculateConsistency
};
