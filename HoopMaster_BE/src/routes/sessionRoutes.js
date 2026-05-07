const express = require('express');
const router = express.Router();
const sessionController = require('../controllers/sessionController');

router.get('/', sessionController.listSessions);
router.get('/:socketId', sessionController.getSessionBySocketId);

module.exports = router;
