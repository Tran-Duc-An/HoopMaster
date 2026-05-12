/**
 * Workout History Service
 * Ghi lại và truy xuất lịch sử tập luyện theo ngày
 */
const WorkoutHistory = require('../models/workoutHistoryModel');

/**
 * Ghi lại một buổi tập sau khi hoàn thành exercise
 */
async function logWorkout(userId, exerciseData) {
  const today = new Date().toISOString().slice(0, 10); // YYYY-MM-DD

  // Tìm record của ngày hôm nay, upsert
  const record = await WorkoutHistory.findOneAndUpdate(
    { userId, date: today },
    {
      $inc: {
        totalExercises: 1,
        totalSets: exerciseData.sets || 0,
        totalReps: exerciseData.reps || 0,
        totalMinutes: exerciseData.durationMinutes || 0
      },
      $push: {
        exercises: {
          exerciseId: exerciseData.exerciseId,
          name: exerciseData.name || 'Unknown',
          category: exerciseData.category || 'general',
          sets: exerciseData.sets || 0,
          reps: exerciseData.reps || 0,
          durationMinutes: exerciseData.durationMinutes || 0
        }
      }
    },
    { upsert: true, new: true }
  );

  return record;
}

/**
 * Lấy lịch sử tập luyện 7 ngày gần nhất
 */
async function getWeeklyHistory(userId) {
  const sevenDaysAgo = new Date();
  sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 6);
  const startDate = sevenDaysAgo.toISOString().slice(0, 10);

  const records = await WorkoutHistory.find({
    userId,
    date: { $gte: startDate }
  }).sort({ date: -1 }).lean();

  // Tạo mảng 7 ngày, fill ngày không có dữ liệu với 0
  const days = [];
  for (let i = 6; i >= 0; i--) {
    const d = new Date();
    d.setDate(d.getDate() - i);
    const dateStr = d.toISOString().slice(0, 10);
    const dayRecord = records.find(r => r.date === dateStr);
    days.push({
      date: dateStr,
      dayLabel: d.toLocaleDateString('en-US', { weekday: 'short' }),
      totalExercises: dayRecord?.totalExercises || 0,
      totalMinutes: dayRecord?.totalMinutes || 0,
      totalSets: dayRecord?.totalSets || 0
    });
  }

  return days;
}

/**
 * Lấy tất cả lịch sử (cho profile)
 */
async function getAllHistory(userId, limit = 30) {
  return WorkoutHistory.find({ userId })
    .sort({ date: -1 })
    .limit(limit)
    .lean();
}

module.exports = {
  logWorkout,
  getWeeklyHistory,
  getAllHistory
};