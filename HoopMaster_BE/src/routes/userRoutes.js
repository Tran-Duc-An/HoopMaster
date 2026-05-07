const express = require('express');
const router = express.Router();
const userController = require('../controllers/userController');

router.post('/signup', userController.signup);
router.post('/login', userController.login);
// Update user profile (including tone)
router.put('/profile/:id', userController.updateProfile);

// Update coach tone only
router.put('/profile/:id/tone', userController.updateTone);

module.exports = router;
