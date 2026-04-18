# Guide to Run Backend & Test Client

## Backend (Node.js)
1. Install dependencies:
   ```bash
   npm install
   ```
2. Start server:
   ```bash
   node src/server.js
   # or
   npx nodemon src/server.js
   ```

## Test Client (Python)
1. Install Python 3 if not available.
2. Install required packages (if any):
   ```bash
   pip install requests websockets
   ```
3. Run test client:
   ```bash
   python test_client.py
   ```

## Notes
- Edit `.env` for environment variables (not tracked by git).
- Run `npm install` after cloning to restore `node_modules`.
