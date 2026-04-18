/**
 * Example Test Client - Để test WebSocket connection
 * Chạy: node examples/testClient.js
 */

const io = require('socket.io-client');

// Configuration
const SERVER_URL = 'http://localhost:3000';

// Mock MediaPipe Landmarks (33 points)
// Đây là pose đứng thẳng, tay giơ lên (shooting position)
const mockLandmarks = [
  // 0: nose
  { x: 0.5, y: 0.2, z: 0.0, visibility: 0.99 },
  // 1-2: eyes
  { x: 0.48, y: 0.18, z: 0.0, visibility: 0.99 },
  { x: 0.52, y: 0.18, z: 0.0, visibility: 0.99 },
  // 3-10: face landmarks (skip for brevity)
  ...Array(8).fill({ x: 0.5, y: 0.2, z: 0.0, visibility: 0.95 }),
  // 11: left shoulder
  { x: 0.4, y: 0.35, z: 0.0, visibility: 0.99 },
  // 12: right shoulder
  { x: 0.6, y: 0.35, z: 0.0, visibility: 0.99 },
  // 13: left elbow (góc ~90 độ)
  { x: 0.35, y: 0.5, z: 0.0, visibility: 0.99 },
  // 14: right elbow
  { x: 0.65, y: 0.5, z: 0.0, visibility: 0.99 },
  // 15: left wrist
  { x: 0.3, y: 0.4, z: 0.0, visibility: 0.99 },
  // 16: right wrist
  { x: 0.7, y: 0.4, z: 0.0, visibility: 0.99 },
  // 17-22: hands (skip)
  ...Array(6).fill({ x: 0.5, y: 0.5, z: 0.0, visibility: 0.9 }),
  // 23: left hip
  { x: 0.42, y: 0.65, z: 0.0, visibility: 0.99 },
  // 24: right hip
  { x: 0.58, y: 0.65, z: 0.0, visibility: 0.99 },
  // 25: left knee (góc ~120 độ)
  { x: 0.4, y: 0.8, z: 0.0, visibility: 0.99 },
  // 26: right knee
  { x: 0.6, y: 0.8, z: 0.0, visibility: 0.99 },
  // 27: left ankle
  { x: 0.38, y: 0.95, z: 0.0, visibility: 0.99 },
  // 28: right ankle
  { x: 0.62, y: 0.95, z: 0.0, visibility: 0.99 },
  // 29-32: feet (skip)
  ...Array(4).fill({ x: 0.5, y: 0.98, z: 0.0, visibility: 0.9 })
];

console.log('🏀 AI Basketball Coach - Test Client');
console.log('=====================================\n');

// Connect to server
const socket = io(SERVER_URL, {
  transports: ['websocket'],
  reconnection: true,
  reconnectionDelay: 1000,
  reconnectionAttempts: 5
});

// Connection events
socket.on('connect', () => {
  console.log('✅ Connected to server');
  console.log('Socket ID:', socket.id);
  console.log('\nStarting pose data stream...\n');
  
  // Simulate pose data stream (10 FPS)
  let frameCount = 0;
  const interval = setInterval(() => {
    frameCount++;
    
    // Slightly modify landmarks to simulate movement
    const modifiedLandmarks = mockLandmarks.map(lm => ({
      ...lm,
      x: lm.x + (Math.random() - 0.5) * 0.01,
      y: lm.y + (Math.random() - 0.5) * 0.01
    }));

    socket.emit('pose_data', {
      landmarks: modifiedLandmarks,
      timestamp: Date.now(),
      frameId: `frame_${frameCount}`
    });

    // Simulate shot release after 3 seconds (30 frames)
    if (frameCount === 30) {
      console.log('\n🎯 Simulating shot release...\n');
      socket.emit('shot_released');
    }

    // Stop after 5 seconds (50 frames)
    if (frameCount >= 50) {
      clearInterval(interval);
      console.log('\n✅ Test completed! Waiting for final feedback...\n');
      
      // Disconnect after 3 seconds
      setTimeout(() => {
        socket.disconnect();
        process.exit(0);
      }, 3000);
    }
  }, 100); // 100ms = 10 FPS
});

socket.on('connected', (data) => {
  console.log('📨 Server message:', data.message);
});

socket.on('coach_feedback', (data) => {
  console.log('\n🎤 REAL-TIME FEEDBACK:');
  console.log('Text:', data.text);
  console.log('Intent:', data.intent);
  console.log('Angles:', data.angles);
  console.log('Audio:', data.audioBase64.substring(0, 50) + '...');
  console.log('---');
});

socket.on('angles_update', (data) => {
  console.log('📐 Angles Update:', {
    elbow: data.elbowAngle?.toFixed(1),
    knee: data.kneeAngle?.toFixed(1),
    shoulder: data.shoulderAngle?.toFixed(1),
    allGood: data.allGood
  });
});

socket.on('llm_post_shot_feedback', (data) => {
  console.log('\n🤖 LLM POST-SHOT FEEDBACK:');
  console.log('Text:', data.text);
  console.log('\nStatistics:');
  console.log('- Avg Elbow Angle:', data.stats.avgElbowAngle?.toFixed(1));
  console.log('- Avg Knee Angle:', data.stats.avgKneeAngle?.toFixed(1));
  console.log('- Frame Count:', data.stats.frameCount);
  console.log('- Shooting Hand:', data.stats.shootingHand);
  console.log('\nMetadata:');
  console.log('- Model:', data.metadata.model);
  console.log('- Generation Time:', data.metadata.generationTime + 'ms');
  console.log('---');
});

socket.on('session_info', (data) => {
  console.log('\n📊 Session Info:', data);
});

socket.on('error', (error) => {
  console.error('\n❌ Error:', error);
});

socket.on('disconnect', (reason) => {
  console.log('\n❌ Disconnected:', reason);
});

socket.on('connect_error', (error) => {
  console.error('\n❌ Connection Error:', error.message);
  process.exit(1);
});

// Handle Ctrl+C
process.on('SIGINT', () => {
  console.log('\n\nShutting down test client...');
  socket.disconnect();
  process.exit(0);
});