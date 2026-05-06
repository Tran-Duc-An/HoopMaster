const express = require('express');
const cors = require('cors');
const exerciseRoutes = require('../src/routes/exerciseRoutes');

const PORT = process.env.PORT || 3100;

const app = express();
app.use(cors());
app.use(express.json());

app.get('/health', (req, res) => {
  res.json({
    status: 'OK',
    mode: 'exercise-api-sandbox',
    timestamp: new Date().toISOString()
  });
});

app.use('/api/exercises', exerciseRoutes);

app.listen(PORT, () => {
  console.log(`[Exercise API Sandbox] http://localhost:${PORT}`);
  console.log('[Exercise API Sandbox] Try: npm run test:api:exercise -- --base=http://localhost:3100');
});
