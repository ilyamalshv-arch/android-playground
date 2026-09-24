// Offline-first: the app shell is cached on install; data and sounds are cached as they are fetched.
const CACHE = "sincerer-18bcf96a5a";
const SHELL = ["./", "index.html", "style.css", "app.js", "manifest.webmanifest", "icons/icon-192.png", "icons/icon-512.png", "data/meta.json"];

self.addEventListener("install", (e) => {
  e.waitUntil(caches.open(CACHE).then((c) => c.addAll(SHELL)).then(() => self.skipWaiting()));
});

self.addEventListener("activate", (e) => {
  e.waitUntil(caches.keys().then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k)))).then(() => self.clients.claim()));
});

self.addEventListener("fetch", (e) => {
  const url = new URL(e.request.url);
  if (e.request.method !== "GET" || url.origin !== location.origin) return;
  // Network first for the shell (so updates arrive), cache first for heavy data and sounds.
  const heavy = url.pathname.includes("/data/content_") || url.pathname.includes("/sound/");
  e.respondWith(
    heavy
      ? caches.match(e.request).then((hit) => hit || fetch(e.request).then((res) => { const c = res.clone(); caches.open(CACHE).then((k) => k.put(e.request, c)); return res; }))
      : fetch(e.request).then((res) => { const c = res.clone(); caches.open(CACHE).then((k) => k.put(e.request, c)); return res; }).catch(() => caches.match(e.request)),
  );
});
