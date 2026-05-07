const {
  getUserPlans,
  getActivePlan,
  activatePlan
} = require('../services/trainingPlanService');

async function listUserPlans(req, res) {
  try {
    const { source, status } = req.query;
    const plans = await getUserPlans(req.params.id, { source, status });
    res.json({ plans });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
}

async function getActiveUserPlan(req, res) {
  try {
    const plan = await getActivePlan(req.params.id);
    res.json({ plan });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
}

async function activateUserPlan(req, res) {
  try {
    const plan = await activatePlan(req.params.id, req.params.planId);
    res.json({ plan });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
}

module.exports = {
  listUserPlans,
  getActiveUserPlan,
  activateUserPlan
};
