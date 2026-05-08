#!/usr/bin/env node
require('dotenv').config();
const mongoose = require('mongoose');
const connectDB = require('../src/models/connectDB');
const Conversation = require('../src/models/conversationModel');
const TrainingPlan = require('../src/models/trainingPlanModel');
const User = require('../src/models/userModel');

function parseArgs(argv) {
  const args = {
    dryRun: false,
    userId: null,
    target: 'all',
    olderThanDays: null
  };

  for (let i = 0; i < argv.length; i += 1) {
    const token = argv[i];
    if (token === '--dry-run') {
      args.dryRun = true;
      continue;
    }
    if (token === '--user' || token === '--userId') {
      args.userId = argv[i + 1] || null;
      i += 1;
      continue;
    }
    if (token === '--target') {
      args.target = (argv[i + 1] || 'all').toLowerCase();
      i += 1;
      continue;
    }
    if (token === '--older-than-days') {
      const parsed = Number(argv[i + 1]);
      args.olderThanDays = Number.isFinite(parsed) && parsed > 0 ? parsed : null;
      i += 1;
      continue;
    }
    if (token === '--help' || token === '-h') {
      args.help = true;
    }
  }

  return args;
}

function printHelp() {
  console.log(`
Mongo cleanup utility (safe selective cleanup)

Usage:
  node scripts/cleanupMongo.js --dry-run --target planning-conversations
  node scripts/cleanupMongo.js --target draft-plans --user <mongoUserId>
  node scripts/cleanupMongo.js --target planning-messages --older-than-days 30
  node scripts/cleanupMongo.js --target reset-training-profile --user <mongoUserId>

Targets:
  planning-conversations   Delete planning conversation documents
  planning-messages        Clear messages array in planning conversations
  draft-plans              Delete training plans where status=draft
  personalized-plans       Delete training plans where source=personalized
  reset-training-profile   Unset users.trainingProfile
  all                      Run all targets above

Flags:
  --dry-run                Print counts only, do not modify data
  --user, --userId <id>    Restrict operation to one user
  --older-than-days <n>    Only affect docs older than n days
`);
}

function buildDateFilter(olderThanDays) {
  if (!olderThanDays) return {};
  const threshold = new Date(Date.now() - olderThanDays * 24 * 60 * 60 * 1000);
  return { createdAt: { $lt: threshold } };
}

function validateUserId(userId) {
  if (!userId) return null;
  if (!mongoose.Types.ObjectId.isValid(userId)) {
    throw new Error(`Invalid userId: ${userId}`);
  }
  return new mongoose.Types.ObjectId(userId);
}

async function cleanupPlanningConversations({ dryRun, userObjectId, olderThanDays }) {
  const filter = {
    type: 'planning',
    ...buildDateFilter(olderThanDays),
    ...(userObjectId ? { userId: userObjectId } : {})
  };
  const count = await Conversation.countDocuments(filter);
  if (dryRun) return { action: 'planning-conversations', count, modified: false };
  const result = await Conversation.deleteMany(filter);
  return { action: 'planning-conversations', count: result.deletedCount || 0, modified: true };
}

async function cleanupPlanningMessages({ dryRun, userObjectId, olderThanDays }) {
  const filter = {
    type: 'planning',
    ...buildDateFilter(olderThanDays),
    ...(userObjectId ? { userId: userObjectId } : {})
  };
  const count = await Conversation.countDocuments(filter);
  if (dryRun) return { action: 'planning-messages', count, modified: false };
  const result = await Conversation.updateMany(filter, { $set: { messages: [] } });
  return { action: 'planning-messages', count: result.modifiedCount || 0, modified: true };
}

async function cleanupDraftPlans({ dryRun, userObjectId, olderThanDays }) {
  const filter = {
    status: 'draft',
    ...buildDateFilter(olderThanDays),
    ...(userObjectId ? { userId: userObjectId } : {})
  };
  const count = await TrainingPlan.countDocuments(filter);
  if (dryRun) return { action: 'draft-plans', count, modified: false };
  const result = await TrainingPlan.deleteMany(filter);
  return { action: 'draft-plans', count: result.deletedCount || 0, modified: true };
}

async function cleanupPersonalizedPlans({ dryRun, userObjectId, olderThanDays }) {
  const filter = {
    source: 'personalized',
    ...buildDateFilter(olderThanDays),
    ...(userObjectId ? { userId: userObjectId } : {})
  };
  const count = await TrainingPlan.countDocuments(filter);
  if (dryRun) return { action: 'personalized-plans', count, modified: false };
  const result = await TrainingPlan.deleteMany(filter);
  return { action: 'personalized-plans', count: result.deletedCount || 0, modified: true };
}

async function resetTrainingProfile({ dryRun, userObjectId }) {
  const filter = userObjectId ? { _id: userObjectId } : {};
  const count = await User.countDocuments(filter);
  if (dryRun) return { action: 'reset-training-profile', count, modified: false };
  const result = await User.updateMany(filter, { $unset: { trainingProfile: '' } });
  return { action: 'reset-training-profile', count: result.modifiedCount || 0, modified: true };
}

async function run() {
  const args = parseArgs(process.argv.slice(2));
  if (args.help) {
    printHelp();
    return;
  }

  const validTargets = new Set([
    'all',
    'planning-conversations',
    'planning-messages',
    'draft-plans',
    'personalized-plans',
    'reset-training-profile'
  ]);
  if (!validTargets.has(args.target)) {
    throw new Error(`Unknown target: ${args.target}`);
  }

  const userObjectId = validateUserId(args.userId);
  await connectDB();

  const operations = [];
  if (args.target === 'all' || args.target === 'planning-conversations') {
    operations.push(cleanupPlanningConversations);
  }
  if (args.target === 'all' || args.target === 'planning-messages') {
    operations.push(cleanupPlanningMessages);
  }
  if (args.target === 'all' || args.target === 'draft-plans') {
    operations.push(cleanupDraftPlans);
  }
  if (args.target === 'all' || args.target === 'personalized-plans') {
    operations.push(cleanupPersonalizedPlans);
  }
  if (args.target === 'all' || args.target === 'reset-training-profile') {
    operations.push(resetTrainingProfile);
  }

  console.log(`[Cleanup] Mode=${args.dryRun ? 'DRY-RUN' : 'EXECUTE'} target=${args.target}`);
  if (args.userId) console.log(`[Cleanup] Scoped userId=${args.userId}`);
  if (args.olderThanDays) console.log(`[Cleanup] Filter olderThanDays=${args.olderThanDays}`);

  for (const operation of operations) {
    const result = await operation({
      dryRun: args.dryRun,
      userObjectId,
      olderThanDays: args.olderThanDays
    });
    const verb = args.dryRun ? 'would affect' : 'affected';
    console.log(`[Cleanup] ${result.action}: ${verb} ${result.count} document(s)`);
  }
}

run()
  .then(async () => {
    await mongoose.connection.close();
    process.exit(0);
  })
  .catch(async (error) => {
    console.error('[Cleanup] Failed:', error.message);
    await mongoose.connection.close();
    process.exit(1);
  });
