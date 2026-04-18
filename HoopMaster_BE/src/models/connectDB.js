// MongoDB connection is disabled for NO-MONGO mode
// const mongoose = require('mongoose');
// const connectDB = async () => {
//   try {
//     await mongoose.connect(process.env.MONGODB_URI || 'mongodb://localhost:27017/hoopmaster', {
//       useNewUrlParser: true,
//       useUnifiedTopology: true
//     });
//     console.log('[MongoDB] Connected successfully');
//   } catch (err) {
//     console.error('[MongoDB] Connection error:', err);
//     process.exit(1);
//   }
// };
// module.exports = connectDB;
