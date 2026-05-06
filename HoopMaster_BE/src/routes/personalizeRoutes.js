const express = require('express');
const router = express.Router();
const {
  getUserPlans,
  getActivePlan,
  activatePlan
} = require('../services/trainingPlanService');

// GET /api/users/:id/plans/active
router.get('/:id/plans/active', async (req, res) => {
  try {
    const plan = await getActivePlan(req.params.id);
    res.json({ plan });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

// POST /api/users/:id/plans/:planId/activate
router.post('/:id/plans/:planId/activate', async (req, res) => {
  try {
    const plan = await activatePlan(req.params.id, req.params.planId);
    res.json({ plan });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

// GET /api/users/:id/plans
router.get('/:id/plans', async (req, res) => {
  try {
    const { source, status } = req.query;
    const plans = await getUserPlans(req.params.id, { source, status });
    res.json({ plans });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

module.exports = router;
