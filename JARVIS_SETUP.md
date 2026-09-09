# JARVIS / Gemini setup

The Stark Protocol uses the Gemini Developer API server-side. The browser never receives the Gemini key.

1. Create a Gemini API key in Google AI Studio.
2. Add this environment variable to Railway:

```env
GEMINI_API_KEY=your_key_here
GEMINI_MODEL=gemini-3.7-flash
```

3. Redeploy the Node server.
4. Open **JARVIS** in the app and send a message.

Quantum Map AI missions use the same Gemini key. If Gemini is unavailable, the map falls back to a local randomized mission generator so the game still works.

Do not put the API key in `public/app.js`, `api-config.js`, Android assets, or any browser-visible file.


**Security:** Treat `GEMINI_API_KEY` like a password. Never paste it into `public/`, Android assets, Git, screenshots, or chat. If a key has already been exposed, revoke/replace it before deploying.
