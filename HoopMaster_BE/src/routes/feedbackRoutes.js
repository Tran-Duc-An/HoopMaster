const express = require('express');
const router = express.Router();
const feedbackController = require('../controllers/feedbackController');

router.post('/shot', feedbackController.createShotFeedback);

module.exports = router;
