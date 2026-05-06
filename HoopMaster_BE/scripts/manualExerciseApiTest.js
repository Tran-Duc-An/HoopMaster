const axios = require('axios');

const cliBase = process.argv
  .find(arg => arg.startsWith('--base='))
  ?.replace('--base=', '');

const API_BASE = cliBase || process.env.API_BASE || 'http://localhost:3000';

async function main() {
  console.log(`[Manual API Test] Base URL: ${API_BASE}`);

  await call('GET health', () => axios.get(`${API_BASE}/health`));
  await call('GET default plan', () => axios.get(`${API_BASE}/api/exercises/default`), summarizeDefaultPlan);
  await call('GET voice script', () => axios.get(`${API_BASE}/api/exercises/3/voice-script?sets=1&reps=2`), summarizeVoiceScript);
  await call('POST squat counter simulation', () => axios.post(`${API_BASE}/api/exercises/3/simulate-counter`, {
    sets: 1,
    reps: 2,
    angles: [100, 170, 100, 170]
  }), summarizeSimulation);
  await call('POST push-up counter simulation', () => axios.post(`${API_BASE}/api/exercises/4/simulate-counter`, {
    sets: 1,
    reps: 2,
    angles: [80, 170, 80, 170]
  }), summarizeSimulation);

  console.log('\n[Manual API Test] Done');
}

async function call(label, fn, summarize = data => data) {
  try {
    const response = await fn();
    console.log(`\n[OK] ${label}: ${response.status}`);
    console.log(JSON.stringify(summarize(response.data), null, 2));
  } catch (error) {
    if (error.response) {
      console.log(`\n[FAIL] ${label}: ${error.response.status}`);
      console.log(JSON.stringify(error.response.data, null, 2));
      return;
    }
    console.log(`\n[FAIL] ${label}: ${error.message}`);
  }
}

function summarizeDefaultPlan(data) {
  return {
    title: data.title,
    exerciseCount: data.exercises?.length,
    exercises: data.exercises?.map(ex => ({
      id: ex.exerciseId,
      name: ex.name,
      category: ex.category,
      reps: ex.reps,
      trackingType: ex.tracking?.type
    }))
  };
}

function summarizeVoiceScript(data) {
  return {
    exerciseId: data.exerciseId,
    name: data.name,
    target: data.target,
    script: data.script?.map(item => ({
      type: item.type,
      set: item.set,
      rep: item.rep,
      text: item.text
    }))
  };
}

function summarizeSimulation(data) {
  return {
    exerciseId: data.exerciseId,
    name: data.name,
    trackingType: data.trackingType,
    finalProgress: data.finalProgress,
    cues: data.cues?.map(cue => ({
      type: cue.type,
      text: cue.text,
      metadata: cue.metadata
    }))
  };
}

main().catch(error => {
  console.error('[Manual API Test] Fatal:', error);
  process.exit(1);
});
