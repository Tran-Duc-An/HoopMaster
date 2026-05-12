const express = require('express');
const router = express.Router();

const systemController = require('../controllers/systemController');
const planChatRoutes = require('./planChatRoutes');
const sessionRoutes = require('./sessionRoutes');
const userRoutes = require('./userRoutes');
const exerciseRoutes = require('./exerciseRoutes');
const feedbackRoutes = require('./feedbackRoutes');
const personalizeRoutes = require('./personalizeRoutes');
const workoutHistoryRoutes = require('./workoutHistoryRoutes');

router.get('/', systemController.getApiInfo);
router.get('/health', systemController.getHealth);

router.use('/api/sessions', sessionRoutes);
router.use('/api/users', planChatRoutes);
router.use('/api/users', userRoutes);
router.use('/api/users', personalizeRoutes);
router.use('/api/exercises', exerciseRoutes);
router.use('/api/feedback', feedbackRoutes);
router.use('/api/users', workoutHistoryRoutes);

module.exports = router;
