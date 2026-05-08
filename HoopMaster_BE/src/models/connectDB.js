const mongoose = require('mongoose');

const DEFAULT_DB_NAME = 'HoopMaster';

const normalizeMongoUriDbName = (uri) => {
	if (!uri) return `mongodb://localhost:27017/${DEFAULT_DB_NAME}`;

	// Keep query params intact and only normalize the database segment.
	const [baseUri, queryString] = uri.split('?');
	const lastSlashIndex = baseUri.lastIndexOf('/');

	if (lastSlashIndex === -1) return uri;

	const prefix = baseUri.slice(0, lastSlashIndex + 1);
	const dbName = baseUri.slice(lastSlashIndex + 1);
	if (!dbName) return uri;

	if (dbName.toLowerCase() === DEFAULT_DB_NAME.toLowerCase() && dbName !== DEFAULT_DB_NAME) {
		const normalized = `${prefix}${DEFAULT_DB_NAME}`;
		return queryString ? `${normalized}?${queryString}` : normalized;
	}

	return uri;
};

const connectDB = async () => {
	try {
		const mongoUri = normalizeMongoUriDbName(process.env.MONGODB_URI);
		await mongoose.connect(mongoUri);
		console.log('[MongoDB] Connected successfully');
	} catch (err) {
		console.error('[MongoDB] Connection error:', err);
		process.exit(1);
	}
};
module.exports = connectDB;
