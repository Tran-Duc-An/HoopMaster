const express = require('express');
const router = express.Router();
const planningChatController = require('../controllers/planningChatController');

router.post('/:id/plan-chat', planningChatController.sendPlanningMessage);
router.post('/:id/planning-chat/message', planningChatController.sendPlanningMessage);
router.post('/:id/planning-chat/confirm-plan', planningChatController.confirmPlan);
router.post('/:id/planning-chat/sessions', planningChatController.createSession);
router.get('/:id/planning-chat/sessions', planningChatController.getSessions);
router.get('/:id/planning-chat/history', planningChatController.getHistory);

module.exports = router;
