const express = require('express');
const router = express.Router();
const trainingPlanController = require('../controllers/trainingPlanController');

router.get('/:id/plans/active', trainingPlanController.getActiveUserPlan);
router.post('/:id/plans/:planId/activate', trainingPlanController.activateUserPlan);
router.get('/:id/plans', trainingPlanController.listUserPlans);

module.exports = router;
