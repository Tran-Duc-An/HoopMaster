const TrainingPlan = require('../models/trainingPlanModel');

const createPlan = async (userId, planData) => {
  // Optionally: limit number of plans per user (e.g. 3)
  const count = await TrainingPlan.countDocuments({ userId });
  if (count >= 3) {
    // Remove oldest
    const oldest = await TrainingPlan.findOne({ userId }).sort({ createdAt: 1 });
    if (oldest) await TrainingPlan.deleteOne({ _id: oldest._id });
  }
  const plan = new TrainingPlan({ userId, ...planData });
  await plan.save();
  return plan;
};

const getUserPlans = async (userId) => {
  return TrainingPlan.find({ userId }).sort({ createdAt: -1 });
};

module.exports = { createPlan, getUserPlans };
