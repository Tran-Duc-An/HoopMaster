const userService = require('../services/userService');

const signup = async (req, res) => {
  try {
    const user = await userService.signup(req.body);
    res.status(201).json({ user });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
};

const login = async (req, res) => {
  try {
    const user = await userService.login(req.body);
    res.status(200).json({ user });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
};

const updateProfile = async (req, res) => {
  try {
    const user = await userService.updateProfile(req.params.id, req.body);
    res.status(200).json({ user });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
};

const updateTone = async (req, res) => {
  try {
    const { tone } = req.body;
    if (!['strict', 'cheerful', 'neutral'].includes(tone)) {
      return res.status(400).json({ error: 'Invalid tone' });
    }

    const user = await userService.updateProfile(req.params.id, { tone });
    return res.status(200).json({ user });
  } catch (err) {
    return res.status(400).json({ error: err.message });
  }
};

module.exports = { signup, login, updateProfile, updateTone };
