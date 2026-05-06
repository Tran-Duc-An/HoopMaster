const request = require('supertest');
const express = require('express');

const exerciseRoutes = require('../src/routes/exerciseRoutes');

const app = express();
app.use(express.json());
app.use('/api/exercises', exerciseRoutes);

describe('Exercise API', () => {
  it('EXE_01: returns all cadence-guided exercises', async () => {
    const response = await request(app).get('/api/exercises');

    expect(response.statusCode).toBe(200);
    expect(response.body.length).toBe(6);
    expect(response.body[0]).toHaveProperty('counting');
    expect(response.body[0]).toHaveProperty('voiceCues');
  });

  it('EXE_02: filters exercises by category "warmup"', async () => {
    const response = await request(app).get('/api/exercises/category/warmup');

    expect(response.statusCode).toBe(200);
    expect(response.body.length).toBeGreaterThan(0);
    expect(response.body[0].category).toBe('warmup');
  });

  it('EXE_03: returns exercise details by id', async () => {
    const response = await request(app).get('/api/exercises/1');

    expect(response.statusCode).toBe(200);
    expect(response.body.id).toBe(1);
    expect(response.body.name).toBe('Neck Tilt Count');
    expect(response.body.counting.mode).toBe('rep');
  });

  it('EXE_04: returns 404 for unknown id', async () => {
    const response = await request(app).get('/api/exercises/999');

    expect(response.statusCode).toBe(404);
    expect(response.body.error).toBe('Exercise not found');
  });

  it('EXE_05: returns a voice script with requested reps', async () => {
    const response = await request(app).get('/api/exercises/3/voice-script?sets=1&reps=2');

    expect(response.statusCode).toBe(200);
    expect(response.body.exerciseId).toBe(3);
    expect(response.body.target.reps).toBe(2);
    expect(response.body.script.some(item => item.type === 'rep' && item.rep === 2)).toBe(true);
  });

  it('EXE_06: returns default plan with counting metadata', async () => {
    const response = await request(app).get('/api/exercises/default');

    expect(response.statusCode).toBe(200);
    expect(response.body.source).toBe('default');
    expect(response.body.exercises[0]).toHaveProperty('counting');
    expect(response.body.exercises[0]).toHaveProperty('voiceCues');
  });

  it('EXE_07: simulates pose counter from angle sequence', async () => {
    const response = await request(app)
      .post('/api/exercises/3/simulate-counter')
      .send({ sets: 1, reps: 2, angles: [100, 170, 100, 170] });

    expect(response.statusCode).toBe(200);
    expect(response.body.exerciseId).toBe(3);
    expect(response.body.finalProgress.completed).toBe(true);
    expect(response.body.cues.some(cue => cue.type === 'complete')).toBe(true);
  });
});
