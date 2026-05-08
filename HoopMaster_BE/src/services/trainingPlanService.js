const TrainingPlan = require('../models/trainingPlanModel');

const createPlan = async (userId, planData) => {
  const source = planData.source || 'personalized';
  const status = planData.status || 'draft';
  console.log(`[TrainingPlanService] createPlan start userId=${userId} source=${source} status=${status}`);

  // Keep only a few personalized plans per user; default catalog is not affected.
  const count = await TrainingPlan.countDocuments({ userId, source: 'personalized' });
  if (source === 'personalized' && count >= 5) {
    const oldest = await TrainingPlan
      .findOne({ userId, source: 'personalized', status: { $ne: 'active' } })
      .sort({ createdAt: 1 });
    if (oldest) await TrainingPlan.deleteOne({ _id: oldest._id });
  }

  const plan = new TrainingPlan({ userId, ...planData, source, status });
  await plan.save();
  console.log(`[TrainingPlanService] createPlan success userId=${userId} planId=${plan._id} status=${plan.status}`);
  return plan;
};

const getUserPlans = async (userId, filters = {}) => {
  const query = { userId };
  if (filters.source) query.source = filters.source;
  if (filters.status) query.status = filters.status;
  return TrainingPlan.find(query).sort({ createdAt: -1 });
};

const getActivePlan = async (userId) => {
  return TrainingPlan.findOne({ userId, status: 'active' }).sort({ activatedAt: -1 });
};

const getLatestDraftPlan = async (userId) => {
  return TrainingPlan
    .findOne({ userId, source: 'personalized', status: 'draft' })
    .sort({ createdAt: -1 });
};

const getPlanById = async (userId, planId) => {
  return TrainingPlan.findOne({ _id: planId, userId });
};

const activatePlan = async (userId, planId) => {
  console.log(`[TrainingPlanService] activatePlan start userId=${userId} planId=${planId}`);
  const plan = await getPlanById(userId, planId);
  if (!plan) throw new Error('Training plan not found');

  await TrainingPlan.updateMany(
    { userId, status: 'active', _id: { $ne: plan._id } },
    { $set: { status: 'archived', updatedAt: new Date() } }
  );

  plan.status = 'active';
  plan.activatedAt = new Date();
  plan.updatedAt = new Date();
  await plan.save();
  console.log(`[TrainingPlanService] activatePlan success userId=${userId} planId=${plan._id} status=${plan.status}`);
  return plan;
};

module.exports = {
  createPlan,
  getUserPlans,
  getActivePlan,
  getLatestDraftPlan,
  getPlanById,
  activatePlan
};
