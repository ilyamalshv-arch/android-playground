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
//   Variable "MODEL"      — defaults to @cf/meta/llama-3.3-70b-instruct-fp8-fast

const DEFAULT_MODEL = "@cf/meta/llama-3.3-70b-instruct-fp8-fast";

const LIMITS = { note: 2000, lenses: 4, lensText: 1600, emotions: 8 };

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

const SYSTEM = `Ты — бережный собеседник в приложении «Внутри», которое помогает человеку осмыслить и принять свои чувства через взгляды философских школ.

Тебе дают: чувства человека (с силой), его собственные слова о ситуации и тексты 2–4 философских школ из библиотеки приложения.

Как отвечать:
- Пиши по-русски, обращайся на «вы», тепло и просто, без канцелярита и без жаргона.
- Начни с короткого признания чувства: оно понятно и имеет право быть. Не спорь с чувством и не пытайся его «исправить».
- Затем свяжи 2–3 из данных школ с конкретной ситуацией человека. Опирайся ТОЛЬКО на данные тексты школ. Не приписывай философам то, чего нет в текстах, и не выдумывай цитаты — цитат не приводи вовсе.
- Закончи одним мягким вопросом для размышления.
- Объём: 150–250 слов. Без заголовков и списков, обычными абзацами.

Чего нельзя:
- Ставить диагнозы, говорить о лекарствах, давать медицинские или юридические советы.
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

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (!tokensEqual(request.headers.get("x-app-token") || "", env.APP_TOKEN || "")) {
      return json({ error: "unauthorized" }, 401);
    }
    if (request.method === "GET" && url.pathname === "/health") {
      return json({ ok: true, ai: Boolean(env.AI), model: env.MODEL || DEFAULT_MODEL });
    }
    if (request.method !== "POST" || url.pathname !== "/reflect") {
      return json({ error: "not_found" }, 404);
    }

    let body;
    try {
      body = await request.json();
    } catch {
      return json({ error: "bad_json" }, 400);
    }

    const note = clip(body.note, LIMITS.note).trim();
    const emotions = (Array.isArray(body.emotions) ? body.emotions : []).slice(0, LIMITS.emotions)
      .map((e) => `${clip(e.name, 40)} (${clip(e.intensity, 20)})`);
    const lenses = (Array.isArray(body.lenses) ? body.lenses : []).slice(0, LIMITS.lenses)
      .map((l) => `### ${clip(l.school, 80)} — ${clip(l.tradition, 80)} (о чувстве «${clip(l.emotion, 40)}»)\n${clip(l.text, LIMITS.lensText)}`);

    if (!emotions.length || !lenses.length) return json({ error: "empty" }, 400);

    const normalized = note.toLowerCase().replaceAll("ё", "е");
    if (CRISIS.some((re) => re.test(normalized))) return json({ crisis: true });

    const user = [
      `Чувства: ${emotions.join(", ")}.`,
      note ? `Своими словами:\n${note}` : "Человек не описал ситуацию словами — говори о самих чувствах.",
      `Тексты школ из библиотеки приложения:\n\n${lenses.join("\n\n")}`,
    ].join("\n\n");

    try {
      const result = await env.AI.run(env.MODEL || DEFAULT_MODEL, {
        messages: [
          { role: "system", content: SYSTEM },
          { role: "user", content: user },
        ],
        max_tokens: 700,
        temperature: 0.6,
      });
      const text = String(result?.response ?? "").trim();
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
