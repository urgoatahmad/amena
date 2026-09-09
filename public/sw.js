const CACHE='stark-protocol-v32';
const SHELL=['/','/index.html','/styles.css','/app.js','/api-config.js','/manifest.webmanifest','/icon-192.png','/icon-512.png'];
self.addEventListener('install',event=>event.waitUntil(caches.open(CACHE).then(c=>c.addAll(SHELL)).then(()=>self.skipWaiting())));
self.addEventListener('activate',event=>event.waitUntil(caches.keys().then(keys=>Promise.all(keys.filter(k=>k!==CACHE).map(k=>caches.delete(k)))).then(()=>self.clients.claim())));
self.addEventListener('fetch',event=>{
  const u=new URL(event.request.url);
  if(u.pathname.startsWith('/api/')) return;
  const core=/\.(html|js|css|webmanifest)$/.test(u.pathname) || u.pathname==='/';
  if(core){
    event.respondWith(fetch(event.request).then(r=>{const clone=r.clone();caches.open(CACHE).then(c=>c.put(event.request,clone));return r}).catch(()=>caches.match(event.request)));
    return;
  }
  event.respondWith(caches.match(event.request).then(cached=>cached||fetch(event.request).then(r=>{const clone=r.clone();caches.open(CACHE).then(c=>c.put(event.request,clone));return r}).catch(()=>cached)));
});
