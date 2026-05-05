const express = require('express');
const router = express.Router();
const userController = require('../controllers/userController');

router.post('/signup', userController.signup);
router.post('/login', userController.login);
// Update user profile (including tone)
router.put('/profile/:id', userController.updateProfile);

// Update coach tone only
router.put('/profile/:id/tone', async (req, res) => {
	try {
		const { tone } = req.body;
		if (!['strict', 'cheerful', 'neutral'].includes(tone)) {
			return res.status(400).json({ error: 'Invalid tone' });
		}
		const user = await require('../services/userService').updateProfile(req.params.id, { tone });
		res.status(200).json({ user });
	} catch (err) {
		res.status(400).json({ error: err.message });
	}
});

module.exports = router;
