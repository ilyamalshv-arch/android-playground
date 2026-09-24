/* Sincerer — web version. Same content and logic as the Android app; data comes from data/*.json (built by
   tools/build_web.py from the app's own sources). No framework: small render functions per screen. */
"use strict";

// ---------------------------------------------------------------- state & storage
const store = {
  get(k, d) { try { const v = localStorage.getItem("sincerer." + k); return v == null ? d : JSON.parse(v); } catch { return d; } },
  set(k, v) { try { localStorage.setItem("sincerer." + k, JSON.stringify(v)); } catch {} },
};
const browserLang = (navigator.language || "en").slice(0, 2);
const S = {
  lang: store.get("lang", ["ru", "es", "en"].includes(browserLang) ? browserLang : "en"),
  music: store.get("music", true),
  sounds: store.get("sounds", true),
  haptics: store.get("haptics", true),
  splash: store.get("splash", true),
  journal: store.get("journal", []),
  marks: [],   // [{id, intensity}]
  note: "",
};
let META = null;
const LIB = {}; // lang -> {schools, byId}
const $app = document.getElementById("app");

// ---------------------------------------------------------------- i18n
/** t(ru): translation of a UI string from the app's own dictionary; t3 for web-only strings. */
function t(ru) {
  if (S.lang === "ru") return ru;
  const e = META && META.strings[ru];
  return e ? e[S.lang] || e.en : ru;
}
function t3(ru, en, es) { return S.lang === "ru" ? ru : S.lang === "es" ? es : en; }
const nameOf = (o) => o.name[S.lang] || o.name.en;
const emotion = (id) => META.emotions.find((e) => e.id === id);
const groupColor = (g) => (META.groups.find((x) => x.id === g) || {}).color || "#e9c9a0";
const colorOfEmotion = (id) => groupColor((emotion(id) || {}).group);
const intensityLabel = (i) => (i === 1 ? t("слегка") : i === 3 ? t("сильно") : t("заметно"));
const esc = (s) => String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));

async function loadLibrary(lang) {
  if (LIB[lang]) return LIB[lang];
  const data = await (await fetch(`data/content_${lang}.json`)).json();
  const famOrder = META.families.map((f) => f.id);
  data.schools.sort((a, b) => famOrder.indexOf(a.family) - famOrder.indexOf(b.family) || a.sortYear - b.sortYear);
  data.byId = Object.fromEntries(data.schools.map((s) => [s.id, s]));
  return (LIB[lang] = data);
}
const lib = () => LIB[S.lang];

// ---------------------------------------------------------------- safety & recognition (same rules as the app)
let CRISIS = [];
function isCrisis(text) {
  const n = text.toLowerCase().replace(/ё/g, "е");
  return CRISIS.some((re) => re.test(n));
}
function isHeavy(marks) { return marks.filter((m) => m.intensity >= 3 && META.heavy.includes(m.id)).length >= 2; }

function guess(text, limit = 4) {
  const low = text.toLowerCase().replace(/ё/g, "е");
  if (!low.trim()) return [];
  const cyr = (low.match(/[а-я]/g) || []).length >= (low.match(/[a-z]/g) || []).length;
  const plain = low.normalize("NFD").replace(/\p{Mn}+/gu, "");
  const score = (lang, src) => {
    const guard = lang === "ru" ? "(?<!\\p{L})" : "(?<![a-z])";
    return Object.entries(META.cues[lang]).map(([id, stems]) => {
      let hits = 0;
      for (const s of stems) { try { hits += (src.match(new RegExp(guard + s, "gu")) || []).length; } catch {} }
      return [id, META.broad.includes(id) ? hits : hits * 3];
    }).filter((x) => x[1] > 0).sort((a, b) => b[1] - a[1]);
  };
  // Latin text may be English or Spanish whatever the UI language: take whichever recognises more.
  let scored;
  if (cyr) scored = score("ru", low);
  else {
    const es = score("es", plain), en = score("en", low);
    const sum = (l) => l.reduce((a, x) => a + x[1], 0);
    scored = sum(es) > sum(en) || (sum(es) === sum(en) && S.lang === "es") ? es : en;
  }
  return scored.slice(0, limit).map((x) => x[0]);
}

// ---------------------------------------------------------------- sound & touch
let audio = null;
const Sound = {
  ctx: null, buffers: {}, ambientGain: null, started: false,
  async init() {
    if (this.ctx) return;
    const AC = window.AudioContext || window.webkitAudioContext;
    if (!AC) return;
    this.ctx = new AC();
    const ext = document.createElement("audio").canPlayType('audio/ogg; codecs="vorbis"') ? "ogg" : "mp3";
    await Promise.all(["tap", "select", "confirm", "ambient"].map(async (n) => {
      try { this.buffers[n] = await this.ctx.decodeAudioData(await (await fetch(`sound/${n}.${ext}`)).arrayBuffer()); } catch {}
    }));
  },
  play(n, vol = 0.5) {
    if (!S.sounds || !this.ctx || !this.buffers[n]) return;
    const src = this.ctx.createBufferSource(); const g = this.ctx.createGain();
    g.gain.value = vol; src.buffer = this.buffers[n]; src.connect(g).connect(this.ctx.destination); src.start();
  },
  ambient(level) {
    if (!this.ctx || !this.buffers.ambient) return;
    if (!this.ambientGain) {
      this.ambientGain = this.ctx.createGain(); this.ambientGain.gain.value = 0; this.ambientGain.connect(this.ctx.destination);
      const src = this.ctx.createBufferSource(); src.buffer = this.buffers.ambient; src.loop = true; src.connect(this.ambientGain); src.start();
    }
    const target = S.music && !document.hidden ? level : 0;
    this.ambientGain.gain.setTargetAtTime(target, this.ctx.currentTime, 1.2);
  },
};
let ambientLevel = 0.22;
async function wakeAudio() {
  await Sound.init();
  if (Sound.ctx && Sound.ctx.state === "suspended") await Sound.ctx.resume();
  Sound.ambient(ambientLevel);
}
document.addEventListener("visibilitychange", () => Sound.ambient(ambientLevel));
const buzz = (ms = 8) => { if (S.haptics && navigator.vibrate) navigator.vibrate(ms); };
const fx = { tap() { Sound.play("tap"); buzz(6); }, select() { Sound.play("select", 0.55); buzz(8); }, confirm() { Sound.play("confirm", 0.6); buzz(15); } };

// Light blooms from the exact touch point on anything with [data-glow].
document.addEventListener("pointerdown", (e) => {
  const el = e.target.closest("[data-glow]");
  if (!el) return;
  const r = el.getBoundingClientRect();
  const g = document.createElement("span");
  g.className = "glow";
  const size = Math.max(r.width, r.height) * 2.6;
  g.style.cssText = `left:${e.clientX - r.left}px;top:${e.clientY - r.top}px;width:${size}px;height:${size}px;--g:${el.dataset.glow || "#f3e9da"}`;
  el.appendChild(g);
  setTimeout(() => g.remove(), 950);
}, { passive: true });

// ---------------------------------------------------------------- velvet background
const Velvet = {
  mood: [233, 201, 160], target: [233, 201, 160],
  setMood(hex) { const n = parseInt(hex.slice(1), 16); this.target = [n >> 16, (n >> 8) & 255, n & 255]; document.documentElement.style.setProperty("--mood", hex); },
  start() {
    const c = document.getElementById("velvet"), x = c.getContext("2d");
    let last = 0;
    const draw = (now) => {
      requestAnimationFrame(draw);
      if (now - last < 33) return; // ~30 fps is plenty for slow light
      last = now;
      const w = (c.width = innerWidth * 0.5), h = (c.height = innerHeight * 0.5);
      for (let i = 0; i < 3; i++) this.mood[i] += (this.target[i] - this.mood[i]) * 0.03;
      const [r, g, b] = this.mood.map(Math.round), T = now / 48000 * Math.PI * 2;
      const bg = x.createLinearGradient(0, 0, 0, h);
      bg.addColorStop(0, "#1c0a0c"); bg.addColorStop(0.5, "#140708"); bg.addColorStop(1, "#0c0405");
      x.fillStyle = bg; x.fillRect(0, 0, w, h);
      const folds = x.createLinearGradient(0, 0, w, 0);
      for (let i = 0; i <= 36; i++) { const k = 0.5 + 0.5 * Math.sin(i / 36 * 9 * Math.PI * 2 + T); folds.addColorStop(i / 36, `rgba(140,15,20,${0.05 + 0.07 * k})`); }
      x.fillStyle = folds; x.fillRect(0, 0, w, h);
      const orb = (cx, cy, rad, col) => { const gr = x.createRadialGradient(cx, cy, 0, cx, cy, rad); gr.addColorStop(0, col); gr.addColorStop(1, "rgba(0,0,0,0)"); x.fillStyle = gr; x.fillRect(0, 0, w, h); };
      orb(w * (0.25 + 0.12 * Math.sin(T)), h * (0.22 + 0.08 * Math.cos(T * 2)), w * 0.7, `rgba(${r},${g},${b},.22)`);
      orb(w * (0.8 + 0.1 * Math.cos(T * 3)), h * (0.55 + 0.1 * Math.sin(T)), w * 0.6, `rgba(${r},${g},${b},.16)`);
      orb(w * (0.4 + 0.2 * Math.sin(T * 2)), h * (0.9 + 0.05 * Math.cos(T)), w * 0.8, `rgba(${r},${g},${b},.12)`);
      orb(w * (0.65 + 0.15 * Math.cos(T)), h * (0.12 + 0.06 * Math.sin(T * 3)), w * 0.45, "rgba(243,233,218,.06)");
    };
    requestAnimationFrame(draw);
  },
};

// ---------------------------------------------------------------- router
const routes = {};
function go(hash) { if (location.hash !== hash) location.hash = hash; else render(); }
window.addEventListener("hashchange", render);
async function render() {
  const [, name, arg] = (location.hash || "#/").split("/");
  const fn = routes[name || ""] || routes[""];
  await loadLibrary(S.lang);
  window.scrollTo(0, 0);
  fn(arg ? decodeURIComponent(arg) : undefined);
}
const bar = (title, back = true, actions = "") =>
  `<div class="bar">${back ? `<button class="back" onclick="history.back()" aria-label="${esc(t("Назад"))}">←</button>` : ""}<h1>${esc(title)}</h1>${actions}</div>`;

function langSwitch() {
  const langs = ["ru", "en", "es"], i = Math.max(0, langs.indexOf(S.lang));
  return `<div class="lang"><div class="thumb" style="transform:translateX(${i * 100}%)"></div>${langs
    .map((l) => `<button class="${l === S.lang ? "on" : ""}" onclick="setLang('${l}')">${l.toUpperCase()}</button>`).join("")}</div>`;
}
window.setLang = async (l) => {
  if (l === S.lang) return;
  fx.select(); S.lang = l; store.set("lang", l); document.documentElement.lang = l;
  const thumb = document.querySelector(".lang .thumb");
  if (thumb) thumb.style.transform = `translateX(${["ru", "en", "es"].indexOf(l) * 100}%)`;
  await loadLibrary(l);
  setTimeout(render, 250);
};

// ---------------------------------------------------------------- splash: the wipeable mass
routes["splash"] = () => {
  const lensQuotes = lib().schools.flatMap((s) => ["joy", "hope", "calm", "gratitude", "awe", "inspiration", "relief", "love", "pride", "melancholy"]
    .map((e) => s.lenses[e] && s.lenses[e].quote).filter((q) => q && q.text.length >= 12 && q.text.length <= 120));
  const q = lensQuotes[Math.floor(Math.random() * lensQuotes.length)];
  ambientLevel = 0.7;
  $app.innerHTML = `<div class="splash">
    <canvas id="room"></canvas>
    <div class="title"><h1>Sincerer</h1><p>${esc(t("искренность начинается внутри"))}</p></div>
    <canvas id="mass"></canvas>
    <div class="top">${langSwitch()}<button class="skip" onclick="enterApp()">${esc(t("Пропустить"))}</button></div>
    <div class="bottom"><div id="enterSlot"></div>${q ? `<div class="q"><i>«${esc(q.text.replace(/^[«"“]|[»"”]$/g, ""))}»</i><small>${esc(q.author)}</small></div>` : ""}</div>
  </div>`;
  drawRoom(document.getElementById("room"));
  startMass(document.getElementById("mass"));
};
window.enterApp = () => { fx.confirm(); massRunning = false; ambientLevel = 0.22; Sound.ambient(ambientLevel); go("#/home"); };

function drawRoom(c) {
  const w = (c.width = innerWidth), h = (c.height = innerHeight), x = c.getContext("2d");
  const floorTop = h * 0.74;
  const g = x.createLinearGradient(0, 0, w, 0);
  for (let i = 0; i <= 52; i++) { const k = 0.5 + 0.5 * Math.sin(i / 52 * 13 * Math.PI * 2); g.addColorStop(i / 52, `rgb(${Math.round((0.26 + 0.36 * k) * 255)},${Math.round((0.02 + 0.03 * k) * 255)},${Math.round((0.03 + 0.04 * k) * 255)})`); }
  x.fillStyle = g; x.fillRect(0, 0, w, floorTop);
  const v = x.createLinearGradient(0, 0, 0, floorTop); v.addColorStop(0, "rgba(0,0,0,.45)"); v.addColorStop(0.5, "rgba(0,0,0,0)"); v.addColorStop(1, "rgba(0,0,0,.35)");
  x.fillStyle = v; x.fillRect(0, 0, w, floorTop);
  x.fillStyle = "#EDE6DA"; x.fillRect(0, floorTop, w, h - floorTop);
  const band = (h - floorTop) / 7, tooth = w / 9;
  x.fillStyle = "#15110F";
  for (let b = 0; b < 8; b += 2) {
    const y0 = floorTop + b * band; x.beginPath(); let xx = -tooth, up = true; x.moveTo(xx, y0);
    while (xx <= w + tooth) { xx += tooth / 2; x.lineTo(xx, up ? y0 + band * 0.5 : y0); up = !up; }
    const y1 = y0 + band; xx = w + tooth; x.lineTo(xx, y1); up = true;
    while (xx >= -tooth) { xx -= tooth / 2; x.lineTo(xx, up ? y1 + band * 0.5 : y1); up = !up; }
    x.closePath(); x.fill();
  }
}

let massRunning = false;
function startMass(canvas) {
  // Eraser model: the relief is lit once from the resting surface; the finger only thins the mass.
  const W = 240, H = Math.max(200, Math.min(520, Math.round(W * innerHeight / innerWidth)));
  canvas.width = W; canvas.height = H;
  const ctx = canvas.getContext("2d"), img = ctx.createImageData(W, H), px = img.data;
  const rest = new Float32Array(W * H), height = new Float32Array(W * H); let scratch = new Float32Array(W * H);
  const base = new Uint8ClampedArray(W * H * 3);
  const waves = Array.from({ length: 7 }, () => [Math.random() * 0.09 + 0.02, Math.random() * 0.09 + 0.02, Math.random() * 6.28, Math.random() * 0.08 + 0.03]);
  for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) {
    let v = 1; for (const w of waves) v += w[3] * Math.sin(x * w[0] + y * w[1] + w[2]);
    rest[y * W + x] = v; height[y * W + x] = v;
  }
  const L = [-0.45, -0.62, 0.64], hz = L[2] + 1, hn = Math.hypot(L[0], L[1], hz), Hh = [L[0] / hn, L[1] / hn, hz / hn];
  for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) {
    const i = y * W + x, l = rest[x > 0 ? i - 1 : i], r = rest[x < W - 1 ? i + 1 : i], u = rest[y > 0 ? i - W : i], d = rest[y < H - 1 ? i + W : i];
    const nx = (l - r) * 5, ny = (u - d) * 5, inv = 1 / Math.sqrt(nx * nx + ny * ny + 1);
    const nX = nx * inv, nY = ny * inv, nZ = inv;
    const diffuse = Math.max(0, nX * L[0] + nY * L[1] + nZ * L[2]);
    const s = Math.pow(Math.max(0, nX * Hh[0] + nY * Hh[1] + nZ * Hh[2]), 32);
    const shade = 0.52 + 0.52 * diffuse - ((l + r + u + d) * 0.25 - rest[i]) * 1.2;
    base[i * 3] = (0.95 * shade + 0.75 * s) * 255; base[i * 3 + 1] = (0.925 * shade + 0.75 * s) * 255; base[i * 3 + 2] = (0.885 * shade + 0.78 * s) * 255;
  }
  let idle = 0, frame = 0, ready = false, prev = null, travelled = 0;
  const radius = W * 0.045;
  const stamp = (cx, cy) => {
    const x0 = Math.max(0, Math.floor(cx - radius)), x1 = Math.min(W - 1, Math.ceil(cx + radius));
    const y0 = Math.max(0, Math.floor(cy - radius)), y1 = Math.min(H - 1, Math.ceil(cy + radius));
    for (let y = y0; y <= y1; y++) for (let x = x0; x <= x1; x++) {
      const dd = ((x - cx) ** 2 + (y - cy) ** 2) / (radius * radius);
      if (dd < 1) { const q = 1 - dd; height[y * W + x] -= height[y * W + x] * q * q * 0.35; }
    }
  };
  const wipe = (a, b) => {
    idle = 0;
    const steps = Math.max(1, Math.floor(Math.hypot(b.x - a.x, b.y - a.y) / (radius * 0.35)));
    for (let i = 0; i <= steps; i++) stamp(a.x + (b.x - a.x) * i / steps, a.y + (b.y - a.y) * i / steps);
  };
  const toGrid = (e) => { const r = canvas.getBoundingClientRect(); return { x: (e.clientX - r.left) / r.width * W, y: (e.clientY - r.top) / r.height * H }; };
  canvas.addEventListener("pointerdown", (e) => { wakeAudio(); canvas.setPointerCapture(e.pointerId); prev = toGrid(e); wipe(prev, prev); buzz(5); });
  canvas.addEventListener("pointermove", (e) => {
    if (!prev) return;
    const p = toGrid(e); wipe(prev, p);
    travelled += Math.hypot(p.x - prev.x, p.y - prev.y); if (travelled > 14) { travelled = 0; buzz(4); }
    prev = p;
  });
  const up = () => { prev = null; };
  canvas.addEventListener("pointerup", up); canvas.addEventListener("pointercancel", up);
  massRunning = true;
  const tick = () => {
    if (!massRunning || !document.body.contains(canvas)) return;
    requestAnimationFrame(tick);
    idle++; frame++;
    const heal = idle > 180 ? 0.00025 : 0;
    for (let y = 1; y < H - 1; y++) for (let x = 1; x < W - 1; x++) {
      const i = y * W + x, lap = height[i - 1] + height[i + 1] + height[i - W] + height[i + W] - 4 * height[i];
      const v = height[i] + 0.006 * lap; scratch[i] = Math.max(0, v + (rest[i] - v) * heal);
    }
    for (let y = 1; y < H - 1; y++) height.set(scratch.subarray(y * W + 1, y * W + W - 1), y * W + 1);
    let covered = 0;
    for (let i = 0; i < W * H; i++) {
      const frac = Math.min(1, Math.max(0, height[i] / rest[i]));
      if (frac > 0.3) covered++;
      let a = Math.min(1, Math.max(0, (frac - 0.04) / 0.6)); a = a * a * (3 - 2 * a);
      px[i * 4] = base[i * 3]; px[i * 4 + 1] = base[i * 3 + 1]; px[i * 4 + 2] = base[i * 3 + 2]; px[i * 4 + 3] = a * 255;
    }
    ctx.putImageData(img, 0, 0);
    if (!ready && frame % 30 === 0 && covered / (W * H) < 0.88) {
      ready = true;
      const slot = document.getElementById("enterSlot");
      if (slot) slot.innerHTML = `<button class="enter" onclick="enterApp()">${esc(t("Войти"))}</button>`;
    }
  };
  requestAnimationFrame(tick);
}

// ---------------------------------------------------------------- home
routes["home"] = () => {
  ambientLevel = 0.22; Sound.ambient(ambientLevel);
  const last = S.marks[S.marks.length - 1];
  Velvet.setMood(last ? colorOfEmotion(last.id) : "#e9c9a0");
  const sel = new Set(S.marks.map((m) => m.id));
  const groups = META.groups.map((g, gi) => `
    <div class="section-title" style="color:${g.color};animation:appear .6s ${gi * 60}ms both"><span class="dot"></span>${esc(nameOf(g))}</div>
    <div class="chips">${META.emotions.filter((e) => e.group === g.id).map((e) =>
      `<button class="chip ${sel.has(e.id) ? "on" : ""}" style="--c:${g.color}" data-glow="${g.color}" onclick="toggleMark('${e.id}')">${esc(nameOf(e))}</button>`).join("")}</div>`).join("");
  const intensity = S.marks.length ? `<div class="section-title">${esc(t("Насколько сильно?"))}</div>` + S.marks.map((m) => {
    const c = colorOfEmotion(m.id);
    return `<div class="intensity"><span class="name">${esc(nameOf(emotion(m.id)))}</span>${[1, 2, 3].map((l) =>
      `<button class="chip ${m.intensity === l ? "on" : ""}" style="--c:${c}" data-glow="${c}" onclick="setIntensity('${m.id}',${l})">${esc(intensityLabel(l))}</button>`).join("")}</div>`;
  }).join("") : "";
  $app.innerHTML = `<div class="screen">
    ${bar("Sincerer", false, `<button class="link" onclick="go('#/journal')">${esc(t("Дневник"))}</button><button class="link" onclick="go('#/library')">${esc(t("Школы"))}</button><button class="link" onclick="go('#/settings')" aria-label="${esc(t("Настройки"))}">⚙</button>`)}
    <h2 style="font-size:26px;margin-top:8px">${esc(t("Что вы чувствуете сейчас?"))}</h2>
    <p class="lead">${esc(t("Можно выбрать несколько. Любое чувство имеет право быть."))}</p>
    ${groups}${intensity}
    <label class="field">${esc(t("Своими словами"))}</label>
    <textarea id="note" placeholder="${esc(t("Что происходит? Что вы замечаете в себе?"))}">${esc(S.note)}</textarea>
    <div id="suggest"></div>
    <button class="pearl" id="reflect" data-glow="#ffffff" onclick="submit()">${esc(t("Осмыслить"))}</button>
    <div id="reflectHint" class="hint" style="margin-top:6px"></div>
    <button class="ghost" data-glow="#ff7a6b" onclick="go('#/help')">${esc(t("Мне очень плохо — нужна помощь"))}</button>
    <p class="hint" style="margin-top:16px">${esc(t3("Это пространство для размышлений, а не замена психологу. Всё, что вы пишете, хранится только в этом браузере.", "This is a space for reflection, not a replacement for a psychologist. Everything you write stays in this browser.", "Este es un espacio para pensar, no un reemplazo de un psicólogo. Todo lo que escribís queda solo en este navegador."))}</p>
  </div>`;
  const note = document.getElementById("note");
  note.addEventListener("input", () => { S.note = note.value; updateSuggest(); });
  note.addEventListener("focus", () => setTimeout(() => note.scrollIntoView({ block: "center", behavior: "smooth" }), 300));
  updateSuggest();
};
function currentSuggestions() { const sel = new Set(S.marks.map((m) => m.id)); return guess(S.note).filter((id) => !sel.has(id)); }
function updateSuggest() {
  const sug = currentSuggestions(), box = document.getElementById("suggest");
  box.innerHTML = sug.length ? `<p class="hint" style="margin:10px 0 6px">${esc(S.marks.length ? t("Возможно, ещё:") : t("Похоже на это — нажмите, чтобы отметить:"))}</p><div class="chips">${sug.map((id) => {
    const c = colorOfEmotion(id); return `<button class="chip" style="--c:${c}" data-glow="${c}" onclick="toggleMark('${id}')">+ ${esc(nameOf(emotion(id)))}</button>`;
  }).join("")}</div>` : "";
  const can = S.marks.length > 0 || sug.length > 0 || isCrisis(S.note);
  document.getElementById("reflect").disabled = !can;
  document.getElementById("reflectHint").textContent = can ? "" : S.note.trim()
    ? t("Не получилось узнать чувство по тексту — отметьте его выше, и я подберу взгляды философов.") : t("Отметьте чувство или опишите, что происходит.");
}
window.toggleMark = (id) => {
  const i = S.marks.findIndex((m) => m.id === id);
  if (i >= 0) S.marks.splice(i, 1); else S.marks.push({ id, intensity: 2 });
  fx.select(); wakeAudio(); const y = scrollY; routes.home(); scrollTo(0, y);
};
window.setIntensity = (id, l) => { const m = S.marks.find((x) => x.id === id); if (m) m.intensity = l; fx.tap(); const y = scrollY; routes.home(); scrollTo(0, y); };
window.submit = () => {
  const chosen = S.marks.length ? S.marks.slice() : currentSuggestions().map((id) => ({ id, intensity: 2 }));
  const crisis = isCrisis(S.note);
  const entry = { id: Date.now(), emotions: chosen, note: S.note.trim(), reflection: "", saved: [] };
  if (chosen.length) { S.journal.push(entry); store.set("journal", S.journal); }
  fx.confirm(); S.marks = []; S.note = "";
  if (crisis) go(`#/help/${chosen.length ? entry.id : ""}`); else go(`#/result/${entry.id}`);
};

// ---------------------------------------------------------------- results: three contrasting views
function rng(seed) { let s = seed % 2147483647; if (s <= 0) s += 2147483646; return () => (s = s * 16807 % 2147483647) / 2147483647; }
function pickContrasting(all, savedIds, seed) {
  const r = rng(seed), rest = all.filter((p) => !savedIds.includes(p.s.id));
  for (let i = rest.length - 1; i > 0; i--) { const j = Math.floor(r() * (i + 1)); [rest[i], rest[j]] = [rest[j], rest[i]]; }
  const picked = all.filter((p) => savedIds.includes(p.s.id)).slice(0, 3), fams = new Set(picked.map((p) => p.s.family));
  for (const p of rest) { if (picked.length >= 3) break; if (!fams.has(p.s.family)) { picked.push(p); fams.add(p.s.family); } }
  for (const p of rest) { if (picked.length >= 3) break; if (!picked.includes(p)) picked.push(p); }
  return picked;
}
const view = { tab: 0, shuffle: 0, all: false, family: null, open: new Set() };
routes["result"] = (id) => {
  const entry = S.journal.find((e) => String(e.id) === String(id));
  if (!entry) return go("#/home");
  const emoId = (entry.emotions[view.tab] || entry.emotions[0]).id, c = colorOfEmotion(emoId);
  Velvet.setMood(c);
  const all = lib().schools.filter((s) => s.lenses[emoId]).map((s) => ({ s, l: s.lenses[emoId] }));
  const saved = entry.saved.filter((x) => x.emotion === emoId).map((x) => x.school);
  const list = view.all ? all.filter((p) => !view.family || p.s.family === view.family) : pickContrasting(all, saved, entry.id + view.shuffle * 7919 + emoId.length * 131);
  const tabs = entry.emotions.length > 1 ? `<div class="tabs">${entry.emotions.map((m, i) =>
    `<button class="chip ${i === view.tab ? "on" : ""}" style="--c:${colorOfEmotion(m.id)}" onclick="view.tab=${i};view.open.clear();routes.result('${entry.id}')">${esc(nameOf(emotion(m.id)))}</button>`).join("")}</div>` : "";
  const fams = view.all ? `<div class="tabs"><button class="chip ${!view.family ? "on" : ""}" style="--c:${c}" onclick="view.family=null;routes.result('${entry.id}')">${esc(t("Все"))}</button>${META.families.filter((f) => all.some((p) => p.s.family === f.id)).map((f) =>
    `<button class="chip ${view.family === f.id ? "on" : ""}" style="--c:${c}" onclick="view.family='${f.id}';routes.result('${entry.id}')">${esc(nameOf(f))}</button>`).join("")}</div>` : "";
  const heavy = isCrisis(entry.note) || isHeavy(entry.emotions) ? `<div class="panel">${esc(t("Похоже, сейчас вам по-настоящему тяжело. Философия может помочь осмыслить, но не обязана справляться с этим одна — живой человек рядом или на линии поддержки тоже может помочь."))}<br><button class="text-btn" onclick="go('#/help')">${esc(t("Куда обратиться"))}</button></div>` : "";
  const addiction = ["craving", "addiction", "hangover"].includes(emoId) ? `<p class="hint">${esc(t("Зависимость — не слабость характера. Если захочется поддержки, помогают люди, которые прошли через то же: группы взаимопомощи (например, АА или АН, есть и онлайн-встречи) и врач-нарколог — в том числе анонимно."))}</p>` : "";
  $app.innerHTML = `<div class="screen">${bar(t("Взгляды на чувства"))}${tabs}
    <div class="orb" style="--c:${c}"><span>${esc(nameOf(emotion(emoId)))}</span></div>
    ${heavy}<p class="hint">${esc(t("Три очень разных взгляда. Здесь нет правильного ответа — заметьте, какой отзывается."))}</p>${fams}
    ${list.map((p, i) => lensCard(p.s, p.l, emoId, c, i, entry)).join("")}
    <div class="row">${view.all ? "" : `<button class="text-btn" onclick="fx.tap();view.shuffle++;view.open.clear();routes.result('${entry.id}')">${esc(t("↻ Другие взгляды"))}</button>`}
      <button class="text-btn" onclick="view.all=!view.all;view.family=null;routes.result('${entry.id}')">${esc(view.all ? t("Только три") : t3(`Все школы (${all.length})`, `All schools (${all.length})`, `Todas las escuelas (${all.length})`))}</button></div>
    ${addiction}
    <div class="panel"><h3 style="font-size:20px">${esc(t("Что откликнулось?"))}</h3><p class="hint">${esc(t("Одна-две фразы для себя: какая мысль задела, что стало чуть понятнее."))}</p>
      <textarea id="refl">${esc(entry.reflection || "")}</textarea><button class="text-btn" onclick="saveReflection('${entry.id}')" id="reflSave">${esc(t("Сохранить в дневник"))}</button></div>
    <p class="hint">${esc(t3("ИИ-разбор доступен в Android-версии.", "AI reflection is available in the Android version.", "La reflexión con IA está en la versión de Android."))}</p>
  </div>`;
  const refl = document.getElementById("refl");
  refl.addEventListener("focus", () => setTimeout(() => refl.scrollIntoView({ block: "center", behavior: "smooth" }), 300));
};
function lensCard(s, l, emoId, c, i, entry) {
  const key = s.id + ":" + emoId, open = view.open.has(key);
  const saved = entry && entry.saved.some((x) => x.school === s.id && x.emotion === emoId);
  const quote = open && l.quote ? `<div class="quote">«${esc(l.quote.text.replace(/^[«"“]|[»"”]$/g, ""))}»<span class="who">${esc([l.quote.author, l.quote.source].filter(Boolean).join(", "))}</span></div>` : "";
  const practice = open && l.practice ? `<div class="practice"><b>${esc(t("Попробуйте"))}</b>${esc(l.practice)}</div>` : "";
  const save = open && entry ? `<button class="save" onclick="event.stopPropagation();toggleSave('${entry.id}','${s.id}','${emoId}')">${esc(saved ? t("★ Мысль сохранена в дневник") : t("☆ Сохранить эту мысль"))}</button>` : "";
  return `<div class="card ${open ? "open" : ""}" style="--c:${c};--i:${i}" data-glow="${c}" onclick="toggleCard('${key}')">
    <h3>${esc(s.title)}</h3><div class="sub">${esc(s.period)} · ${esc(s.tradition)}</div>
    <div class="body">${esc(l.text)}</div>${open ? "" : `<div class="more">${esc(t("Читать дальше"))}</div>`}${quote}${practice}${save}</div>`;
}
window.view = view;
window.toggleCard = (key) => { fx.tap(); view.open.has(key) ? view.open.delete(key) : view.open.add(key); const y = scrollY; render().then(() => scrollTo(0, y)); };
window.toggleSave = (id, school, emo) => {
  const e = S.journal.find((x) => String(x.id) === id); if (!e) return;
  const i = e.saved.findIndex((x) => x.school === school && x.emotion === emo);
  if (i >= 0) e.saved.splice(i, 1); else e.saved.push({ school, emotion: emo });
  store.set("journal", S.journal); fx.select(); const y = scrollY; render().then(() => scrollTo(0, y));
};
window.saveReflection = (id) => {
  const e = S.journal.find((x) => String(x.id) === id); if (!e) return;
  e.reflection = document.getElementById("refl").value.trim(); store.set("journal", S.journal); fx.confirm();
  document.getElementById("reflSave").textContent = t("Сохранено ✓");
};

// ---------------------------------------------------------------- journal
const fmtDate = (ms) => new Date(ms).toLocaleString(S.lang === "es" ? "es-AR" : S.lang, { day: "numeric", month: "long", year: "numeric", hour: "2-digit", minute: "2-digit" });
routes["journal"] = () => {
  Velvet.setMood("#e9c9a0");
  const items = S.journal.slice().sort((a, b) => b.id - a.id);
  $app.innerHTML = `<div class="screen">${bar(t("Дневник"))}${items.length ? items.map((e, i) => `
    <div class="card" style="--i:${i}" data-glow="#f3e9da" onclick="go('#/entry/${e.id}')"><div class="sub">${esc(fmtDate(e.id))}</div>
    <h3 style="font-size:19px;margin-top:4px">${esc(e.emotions.map((m) => nameOf(emotion(m.id))).join(" · "))}</h3>
    ${(e.reflection || e.note) ? `<div class="body">${esc(e.reflection || e.note)}</div>` : ""}</div>`).join("")
    : `<p class="lead" style="margin-top:40px;text-align:center">${esc(t("Здесь будут ваши записи: что вы чувствовали, какие мысли откликнулись."))}</p>`}</div>`;
};
routes["entry"] = (id) => {
  const e = S.journal.find((x) => String(x.id) === id); if (!e) return go("#/journal");
  const savedCards = e.saved.map((x, i) => { const s = lib().byId[x.school]; return s && s.lenses[x.emotion] ? lensCard(s, s.lenses[x.emotion], x.emotion, colorOfEmotion(x.emotion), i, null) : ""; }).join("");
  $app.innerHTML = `<div class="screen">${bar(fmtDate(e.id))}
    <div class="panel"><b>${esc(t("Чувства"))}</b><p>${esc(e.emotions.map((m) => `${nameOf(emotion(m.id))} (${intensityLabel(m.intensity)})`).join(", "))}</p>
    ${e.note ? `<b>${esc(t("Своими словами"))}</b><p>${esc(e.note)}</p>` : ""}${e.reflection ? `<b>${esc(t("Что откликнулось"))}</b><p>${esc(e.reflection)}</p>` : ""}</div>
    ${e.saved.length ? `<div class="section-title">${esc(t("Сохранённые мысли"))}</div>${savedCards}` : ""}
    <button class="pearl" data-glow="#fff" onclick="view.tab=0;view.open.clear();go('#/result/${e.id}')">${esc(t("Открыть взгляды снова"))}</button>
    <button class="ghost" onclick="deleteEntry('${e.id}')">${esc(t("Удалить запись"))}</button></div>`;
};
window.deleteEntry = (id) => {
  if (!confirm(t("Удалить запись?") + "\n" + t("Её нельзя будет восстановить."))) return;
  S.journal = S.journal.filter((x) => String(x.id) !== id); store.set("journal", S.journal); go("#/journal");
};

// ---------------------------------------------------------------- library
routes["library"] = () => {
  Velvet.setMood("#e9c9a0");
  let i = 0;
  $app.innerHTML = `<div class="screen">${bar(t("Школы и мыслители"))}${META.families.map((f) => {
    const list = lib().schools.filter((s) => s.family === f.id); if (!list.length) return "";
    return `<div class="section-title">${esc(nameOf(f))}</div>` + list.map((s) => `<div class="card" style="--i:${i++ % 8}" data-glow="#f3e9da" onclick="go('#/school/${s.id}')">
      <h3>${esc(s.title)}</h3><div class="sub">${esc(s.period)} · ${esc(s.tradition)}</div><div class="body">${esc(s.summary)}</div></div>`).join("");
  }).join("")}</div>`;
};
routes["school"] = (id) => {
  const s = lib().byId[id]; if (!s) return go("#/library");
  $app.innerHTML = `<div class="screen">${bar(s.title)}<p class="hint">${esc(s.period)} · ${esc(s.tradition)}</p>
    <h3 style="font-size:18px">${esc(s.thinkers.join(", "))}</h3><p class="hint">${esc(s.keyWorks.join(" · "))}</p>
    <p style="line-height:1.55">${esc(s.summary)}</p><div class="section-title">${esc(t("Как эта школа видит чувства"))}</div><p style="line-height:1.55">${esc(s.onEmotions)}</p>
    <div class="section-title">${esc(t("Взгляд на каждое чувство"))}</div>
    ${META.emotions.filter((e) => s.lenses[e.id]).map((e, i) => lensCard({ ...s, title: nameOf(e), period: "", tradition: nameOf(META.groups.find((g) => g.id === e.group)) }, s.lenses[e.id], e.id, groupColor(e.group), i % 8, null)).join("")}</div>`;
};

// ---------------------------------------------------------------- settings & help
routes["settings"] = () => {
  Velvet.setMood("#e9c9a0");
  const row = (key, title, sub) => `<div class="switch-row"><div>${esc(title)}<small>${esc(sub)}</small></div><button class="toggle ${S[key] ? "on" : ""}" onclick="toggleSetting('${key}')"></button></div>`;
  $app.innerHTML = `<div class="screen">${bar(t("Настройки"))}
    <div class="switch-row"><div>${esc(t("Язык"))}</div>${langSwitch()}</div>
    ${row("music", t("Фоновая музыка"), t("Тихий эмбиент, пока открыто приложение"))}
    ${row("sounds", t("Звуки касаний"), t("Мягкие звуки при выборе и нажатиях"))}
    ${row("haptics", t("Вибрация"), t("Лёгкий тактильный отклик"))}
    ${row("splash", t("Заставка при запуске"), t("Живая масса, которую можно стереть пальцем"))}
    <p class="install">${esc(t3(
      "Установить как приложение: в Safari нажмите «Поделиться» → «На экран „Домой“»; в Chrome — меню ⋮ → «Установить приложение».",
      "Install as an app: in Safari tap Share → Add to Home Screen; in Chrome open the ⋮ menu → Install app.",
      "Instalala como app: en Safari tocá Compartir → Agregar a inicio; en Chrome abrí el menú ⋮ → Instalar app."))}</p>
    <p class="hint">${esc(t3("ИИ-разбор доступен в Android-версии.", "AI reflection is available in the Android version.", "La reflexión con IA está en la versión de Android."))}</p></div>`;
};
window.toggleSetting = (k) => { S[k] = !S[k]; store.set(k, S[k]); fx.tap(); if (k === "music") { wakeAudio(); } routes.settings(); };

const HELP = {
  ru: [["Экстренные службы — 112", "Если есть непосредственная опасность. Работает в России, Украине, Казахстане, странах ЕС и многих других.", "tel:112"],
    ["Россия · Экстренная психологическая помощь МЧС", "+7 495 989-50-50 · круглосуточно, бесплатно", "tel:+74959895050"],
    ["Россия · Телефон доверия", "8 800 2000 122 · для детей, подростков и их родителей, бесплатно", "tel:88002000122"],
    ["Украина · Lifeline Ukraine", "7333 · круглосуточно, бесплатно с мобильных", "tel:7333"],
    ["Другие страны", "findahelpline.com — бесплатные линии помощи по всему миру", "https://findahelpline.com"]],
  en: [["Emergency services — 112 / 911", "If there is immediate danger. 112 works across the EU and many countries; 911 in the US and Canada.", "tel:112"],
    ["US · 988 Suicide & Crisis Lifeline", "Call or text 988 · 24/7, free, confidential", "tel:988"],
    ["UK & Ireland · Samaritans", "116 123 · 24/7, free", "tel:116123"],
    ["Anywhere else", "findahelpline.com — free helplines worldwide", "https://findahelpline.com"]],
  es: [["Emergencias — 911", "Si hay un peligro inmediato. En Argentina: 911; en muchos otros países, 112.", "tel:911"],
    ["Argentina · Centro de Asistencia al Suicida", "135 · gratis desde CABA y Gran Buenos Aires · de 8 a 0 h, anónimo", "tel:135"],
    ["Argentina · CAS desde todo el país", "0800 345 1435 o (011) 5275-1135 · de 8 a 0 h", "tel:08003451435"],
    ["Otros países", "findahelpline.com: líneas de ayuda gratuitas en todo el mundo", "https://findahelpline.com"]],
};
routes["help"] = (continueTo) => {
  Velvet.setMood("#e9c9a0");
  $app.innerHTML = `<div class="screen">${bar(t("Поддержка"))}
    <h2 style="font-size:24px;margin:8px 0">${esc(continueTo !== undefined ? t("Похоже, сейчас очень тяжело. Спасибо, что написали об этом.") : t("Вы не обязаны справляться с этим в одиночку."))}</h2>
    <p style="line-height:1.55">${esc(t("Если есть мысли причинить себе вред или кажется, что не выдержать, — пожалуйста, поговорите с живым человеком прямо сейчас. Звонок анонимный, и вам не нужно заранее знать, что сказать."))}</p>
    ${HELP[S.lang].map(([title, details, href]) => `<a class="help" href="${href}" ${href.startsWith("http") ? 'target="_blank" rel="noopener"' : ""}><b>${esc(title)}</b><span>${esc(details)}</span><br><em>${esc(href.startsWith("tel") ? t("Позвонить") : t("Открыть сайт"))}</em></a>`).join("")}
    <p style="line-height:1.55">${esc(t("Можно также написать или позвонить тому, кому вы доверяете: другу, родственнику, врачу. Просто «мне сейчас плохо, побудь со мной» — уже достаточно."))}</p>
    <div class="panel"><b>${esc(t("Прямо сейчас, на минуту"))}</b><p>${esc(t("Поставьте ноги на пол и почувствуйте опору. Сделайте медленный вдох на 4 счёта и ещё более медленный выдох на 6. Повторите пять раз. Назовите про себя пять вещей, которые вы видите вокруг."))}</p></div>
    ${continueTo ? `<button class="ghost" style="color:var(--pearl);border-color:rgba(243,233,218,.4)" onclick="go('#/result/${continueTo}')">${esc(t("Продолжить к размышлениям"))}</button>` : ""}</div>`;
};

routes[""] = () => go(S.splash ? "#/splash" : "#/home");

// ---------------------------------------------------------------- boot
(async function boot() {
  META = await (await fetch("data/meta.json")).json();
  CRISIS = META.crisis.map((p) => { try { return new RegExp(p, "u"); } catch { return null; } }).filter(Boolean);
  document.documentElement.lang = S.lang;
  Velvet.start();
  document.addEventListener("pointerdown", () => wakeAudio(), { once: true, passive: true });
  if (!location.hash || location.hash === "#/") location.hash = S.splash ? "#/splash" : "#/home";
  else render();
  if ("serviceWorker" in navigator) navigator.serviceWorker.register("sw.js").catch(() => {});
})();
window.go = go; window.routes = routes; window.fx = fx;
