/*
  Web builds use the same origin.
  Capacitor/Android must point at the NEW Stark Protocol Railway service.
  Replace the placeholder before making a production APK.
*/
const isNativeShell =
  location.protocol === 'capacitor:' ||
  (location.hostname === 'localhost' && /android/i.test(navigator.userAgent || '')) ||
  !!window.Capacitor;

window.__STARK_PROTOCOL_API__ = isNativeShell
  ? 'https://YOUR-STARK-PROTOCOL-RAILWAY-URL'
  : '';
