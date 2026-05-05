const express = require('express');
const router = express.Router();
const { generatePersonalizedPlan } = require('../services/personalizeService');
const { createPlan, getUserPlans } = require('../services/trainingPlanService');
const User = require('../models/userModel');



// GET /api/users/:id/plans
router.get('/:id/plans', async (req, res) => {
  try {
    const plans = await getUserPlans(req.params.id);
    res.json({ plans });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

module.exports = router;
