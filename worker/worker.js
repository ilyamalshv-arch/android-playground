// Vnutri AI proxy — a Cloudflare Worker.
//
// The app sends: chosen feelings, the person's own words, and 2–4 school texts from the
// app's offline library. The model links those texts to the person's situation.
// Nothing is stored or logged here. Paste this whole file into the Cloudflare dashboard editor.
//
// Required settings on the Worker:
//   Binding  "AI"         — Workers AI
//   Secret   "APP_TOKEN"  — any long random string; the same string goes into the app settings
// Optional:
//   Variable "MODEL"      — overrides the default model id

// The app picks one of these by key; anything else falls back to the default.
const MODELS = {
  llama33: "@cf/meta/llama-3.3-70b-instruct-fp8-fast",
  llama4: "@cf/meta/llama-4-scout-17b-16e-instruct",
  mistral: "@cf/mistralai/mistral-small-3.1-24b-instruct",
  gemma: "@cf/google/gemma-3-12b-it",
};
const DEFAULT_MODEL = MODELS.llama33;

const LIMITS = { note: 2000, lenses: 4, lensText: 1600, emotions: 8, turns: 8, turnText: 1500 };

const STYLES = {
  gentle: "Тон: особенно мягкий и тёплый. Больше признания и поддержки, меньше анализа.",
  deep: "Тон: глубже и философичнее. Можно смелее разворачивать понятия школ, показывать напряжение между ними и неочевидные связи с ситуацией. Объём до 300 слов.",
  practical: "Тон: практичнее. После связи со школами предложи 1–2 маленьких конкретных шага на сегодня, вытекающих из этих взглядов (не советы о крупных решениях).",
};

// Same idea as Safety.kt in the app: when in doubt, point to real help instead of philosophy.
const CRISIS = [
  /суицид/, /самоубий/, /самоповрежд/, /поконч\p{L}*\s+с\s+собой/u, /(убить|убью|убиваю)\s+себя/,
  /не\s+(хочу|хочется|могу|желаю)\s+(больше\s+)?жить/, /жить\s+(больше\s+)?не\s+(хочу|хочется)/,
  /нет\s+смысла\s+жить/, /зачем\s+(мне\s+)?(дальше\s+)?жить/, /(хочу|хочется)\s+(просто\s+)?(умереть|сдохнуть|исчезнуть)/,
  /лучше\s+бы\s+меня\s+не\s+было/, /(всем|им)\s+будет\s+лучше\s+без\s+меня/, /повес\p{L}*ся/u, /повешусь/,
  /вскр\p{L}*\s+вены/u, /(порезать|порежу|резать|режу|резала|резал)\s+себя/, /причин\p{L}*\s+себе\s+(боль|вред)/u,
  /наглотат\p{L}*\s+таблет/u, /выпилит\p{L}*ся/u, /выпилюсь/, /(выйти|шагнуть|выпрыгнуть)\s+(в|из)\s+окн/,
  /(спрыгнуть|прыгнуть)\s+с\s+(крыши|моста)/, /не\s+проснуться/,
  /suicid/, /kill\s+myself/, /want\s+to\s+die/, /self[- ]?harm/, /end\s+my\s+life/,
];

const SYSTEM = `Ты — бережный и умный собеседник в приложении Sincerer. Приложение помогает человеку осмыслить и принять свои чувства через взгляды философских школ — не «исправить» их.

Тебе дают: чувства человека (с силой), его собственные слова о ситуации и тексты нескольких философских школ из библиотеки приложения.

Как отвечать на первое сообщение:
1. Одно-два предложения признания. Назови чувство и то, что за ним, опираясь на конкретные детали из слов человека (не пересказывай всё, выбери главное). Без шаблонов вроде «я понимаю, как вам тяжело».
2. Затем 2–3 абзаца, каждый — одна школа, названная по имени («Спиноза сказал бы…», «Для Хан это…»). Бери из текста школы одно понятие и покажи, как оно меняет взгляд именно на эту ситуацию. Школы могут спорить друг с другом — это хорошо.
3. Закончи одним точным вопросом для размышления, который вытекает из сказанного, а не общим «что вы чувствуете?».

Правила:
- Пиши по-русски, на «вы», живым простым языком, короткими абзацами, без заголовков, списков и markdown.
- Опирайся ТОЛЬКО на данные тексты школ. Не приписывай философам того, чего в них нет. Цитат не приводи.
- Если чувство «неудобное» (злорадство, зависть, злость на близких и т. п.) — не стыди: такое чувство человечно; помогай понять, что оно защищает или о чём сигналит. Никогда не одобряй действия во вред кому-либо.
- Объём: 150–250 слов, если в тоне не сказано иное.

Если это продолжение разговора: отвечай на последнее сообщение человека, 80–180 слов, можно задавать уточняющий вопрос, продолжай опираться на те же школы. Не повторяй уже сказанное.

Чего нельзя:
- Ставить диагнозы, говорить о лекарствах, давать медицинские, диетические или юридические советы.
- Давать советы о крупных жизненных решениях (уйти с работы, расстаться и т. п.) — только помогать думать.
- Морализировать, стыдить, обесценивать («бывает хуже», «просто не думайте об этом»).
- Представлять смерть, самоповреждение или отказ от жизни как выход — ни в каком виде.

Если в словах человека есть признаки опасности для жизни или сильного отчаяния — не философствуй: коротко и тепло скажи, что сейчас важнее поговорить с живым человеком, предложи позвонить на линию помощи или 112, и что приложение покажет номера.

Если человек пишет не о чувствах, а просит что-то другое (код, рецепт, домашнее задание) — вежливо скажи, что здесь можно поговорить только о том, что он чувствует.`;

function json(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" },
  });
}

function clip(s, n) {
  return String(s ?? "").slice(0, n);
}

function tokensEqual(a, b) {
  if (typeof a !== "string" || typeof b !== "string" || a.length !== b.length || a.length < 16) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i++) diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return diff === 0;
}

// Reads the whole text and picks 1–4 states from the app's list. Returns only known ids.
async function classify(body, env) {
  const text = clip(body.text, LIMITS.note).trim();
  const states = (Array.isArray(body.states) ? body.states : []).slice(0, 100)
    .map((s) => ({ id: clip(s.id, 40), name: clip(s.name, 60) }))
    .filter((s) => /^[a-z_]+$/.test(s.id));
  if (!text || !states.length) return json({ error: "empty" }, 400);

  const normalized = text.toLowerCase().replaceAll("ё", "е");
  if (CRISIS.some((re) => re.test(normalized))) return json({ crisis: true });

  const list = states.map((s) => `${s.id} — ${s.name}`).join("\n");
  const prompt = `Список состояний (id — название):\n${list}\n\nТекст человека:\n"""\n${text}\n"""\n\n` +
    "Выбери от 1 до 4 состояний из списка, которые точнее всего описывают то, что человек переживает в этом тексте. " +
    "Учитывай весь текст, а не отдельные слова; конкретные состояния (например, похмелье, предательство) важнее общих. " +
    'Ответь ТОЛЬКО JSON-массивом id по убыванию важности, например ["hangover","shame"]. Без пояснений.';
  const model = MODELS[body.model] || env.MODEL || DEFAULT_MODEL;
  try {
    const result = await env.AI.run(model, {
      messages: [
        { role: "system", content: "Ты классификатор эмоциональных состояний. Отвечаешь только JSON-массивом строк." },
        { role: "user", content: prompt },
      ],
      max_tokens: 60,
      temperature: 0,
    });
    const raw = String(result?.response ?? result?.choices?.[0]?.message?.content ?? "");
    const known = new Set(states.map((s) => s.id));
    let ids = [];
    const match = raw.match(/\[[\s\S]*?\]/);
    if (match) {
      try { ids = JSON.parse(match[0]); } catch { ids = []; }
    }
    if (!Array.isArray(ids) || !ids.length) ids = raw.match(/[a-z_]+/g) || [];
    ids = [...new Set(ids.map(String))].filter((id) => known.has(id)).slice(0, 4);
    return json({ ids });
  } catch (e) {
    const message = String(e?.message || e);
    const quota = /4006|daily free allocation|neurons/i.test(message);
    return json({ error: quota ? "quota" : "ai_failed" }, quota ? 429 : 502);
  }
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (!tokensEqual(request.headers.get("x-app-token") || "", env.APP_TOKEN || "")) {
      return json({ error: "unauthorized" }, 401);
    }
    if (request.method === "GET" && url.pathname === "/health") {
      return json({ ok: true, ai: Boolean(env.AI), models: Object.keys(MODELS) });
    }
    if (request.method !== "POST" || (url.pathname !== "/reflect" && url.pathname !== "/classify")) {
      return json({ error: "not_found" }, 404);
    }

    let body;
    try {
      body = await request.json();
    } catch {
      return json({ error: "bad_json" }, 400);
    }

    if (url.pathname === "/classify") return classify(body, env);

    const note = clip(body.note, LIMITS.note).trim();
    const emotions = (Array.isArray(body.emotions) ? body.emotions : []).slice(0, LIMITS.emotions)
      .map((e) => `${clip(e.name, 40)} (${clip(e.intensity, 20)})`);
    const lenses = (Array.isArray(body.lenses) ? body.lenses : []).slice(0, LIMITS.lenses)
      .map((l) => `### ${clip(l.school, 80)} — ${clip(l.tradition, 80)} (о чувстве «${clip(l.emotion, 40)}»)\n${clip(l.text, LIMITS.lensText)}`);
    // Earlier turns of this conversation, oldest first: [{role: "user"|"assistant", text}]
    const history = (Array.isArray(body.history) ? body.history : []).slice(-LIMITS.turns)
      .filter((t) => (t.role === "user" || t.role === "assistant") && String(t.text ?? "").trim())
      .map((t) => ({ role: t.role, content: clip(t.text, LIMITS.turnText) }));

    if (!emotions.length || !lenses.length) return json({ error: "empty" }, 400);

    const userTexts = [note, ...history.filter((t) => t.role === "user").map((t) => t.content)];
    const normalized = userTexts.join("\n").toLowerCase().replaceAll("ё", "е");
    if (CRISIS.some((re) => re.test(normalized))) return json({ crisis: true });

    const first = [
      `Чувства: ${emotions.join(", ")}.`,
      note ? `Своими словами:\n${note}` : "Человек не описал ситуацию словами — говори о самих чувствах.",
      `Тексты школ из библиотеки приложения:\n\n${lenses.join("\n\n")}`,
    ].join("\n\n");

    const style = STYLES[body.style] || STYLES.gentle;
    const model = MODELS[body.model] || env.MODEL || DEFAULT_MODEL;

    try {
      const result = await env.AI.run(model, {
        messages: [
          { role: "system", content: `${SYSTEM}\n\n${style}` },
          { role: "user", content: first },
          ...history,
        ],
        max_tokens: body.style === "deep" ? 900 : 700,
        temperature: 0.6,
      });
      const text = String(result?.response ?? result?.choices?.[0]?.message?.content ?? "").trim();
      if (!text) return json({ error: "empty_response" }, 502);
      return json({ text });
    } catch (e) {
      const message = String(e?.message || e);
      // Free plan: the daily allocation is a hard cap, requests fail instead of being billed.
      const quota = /4006|daily free allocation|neurons/i.test(message);
      return json({ error: quota ? "quota" : "ai_failed" }, quota ? 429 : 502);
    }
  },
};
