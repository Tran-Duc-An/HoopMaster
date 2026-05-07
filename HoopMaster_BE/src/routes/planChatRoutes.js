const express = require('express');
const router = express.Router();
const planningChatController = require('../controllers/planningChatController');

router.post('/:id/plan-chat', planningChatController.sendPlanningMessage);
router.post('/:id/planning-chat/message', planningChatController.sendPlanningMessage);
router.post('/:id/planning-chat/confirm-plan', planningChatController.confirmPlan);
router.get('/:id/planning-chat/history', planningChatController.getHistory);

module.exports = router;
