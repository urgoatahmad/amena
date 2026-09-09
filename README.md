# The Stark Protocol v40

A redesigned, Android-first birthday command system for Amena.

## What changed in v40
- Complete UI rebuild: command-deck layout, technical telemetry, live panels, node topology, diagnostics, archive vault, and classified Director layer.
- Quantum Map is now a real interactive game hub, not a quiz page.
- Six playable mechanics: Cipher Break, Memory Grid, Reaction Lock, Routing Protocol, Pattern Shift, and Bluff Matrix.
- Gemini AI missions remain available as an additional generated mode.
- Rebuilt Avengers dossiers and custom vector-style interface glyphs.
- Rebuilt Stark Lab with overclocking, calibration, diagnostics, protocols, and timing calibration.
- Rebuilt JARVIS channel with conversation history, voice fallback, notifications, and deterministic Amena priority response.
- Added Archives and Network views.
- Redacted Director Console now works with both secure cookies and an Android-safe bearer clearance token.
- Server auto-generates a temporary session secret when `SESSION_SECRET` is not supplied. For production, set a permanent random `SESSION_SECRET` in Railway so existing sessions survive restarts.
- No Gemini API key is shipped in the app.

## Railway
Set:
- `GEMINI_API_KEY` — server-side only
- `GEMINI_MODEL=gemini-3.7-flash`
- `PORT=3000`
- `DATA_DIR=./data`
- `SESSION_SECRET=<long random secret>`
- `CONTROL_PASSWORD=REDACTEDGODS`
- `ADMIN_PASSWORD=<your admin password>`

## Android
Before building the APK, replace the placeholder in `public/api-config.js` with the URL of the NEW Stark Protocol Railway service. Then copy/sync the `public/` web assets into the Capacitor Android project.
