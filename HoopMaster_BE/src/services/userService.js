const User = require('../models/userModel');
const bcrypt = require('bcryptjs');

const signup = async (userData) => {
  const { username, email, password, name } = userData;
  const existingUser = await User.findOne({ $or: [{ username }, { email }] });
  if (existingUser) throw new Error('Username or email already exists');
  const hashedPassword = await bcrypt.hash(password, 10);
  const user = new User({ username, email, password: hashedPassword, name });
  await user.save();
  return user;
};

const login = async ({ usernameOrEmail, password }) => {
  const user = await User.findOne({ $or: [{ username: usernameOrEmail }, { email: usernameOrEmail }] });
  if (!user) throw new Error('User not found');
  const isMatch = await bcrypt.compare(password, user.password);
  if (!isMatch) throw new Error('Invalid credentials');
  return user;
};

const updateProfile = async (userId, updateData) => {
  const user = await User.findByIdAndUpdate(userId, updateData, { new: true });
  if (!user) throw new Error('User not found');
  return user;
};

module.exports = { signup, login, updateProfile };