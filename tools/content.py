#!/usr/bin/env python3
"""Validate school content and regenerate docs/schools.md.

Usage: python3 tools/content.py [--docs]
"""
import json, pathlib, re, sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
SCHOOLS = ROOT / "app/src/main/assets/content/schools"
EMOTIONS = ("anxiety fear sadness grief loneliness anger shame guilt envy emptiness confusion fatigue boredom "
            "joy love gratitude hope nostalgia craving addiction hangover euphoria crash panic dread fomo sleepless "
            "betrayal jealousy resentment heartbreak impostor self_hatred helplessness procrastination uprooted "
            "overload unreality awe relief calm inspiration").split()
FAMILIES = {
    "classic": "Античность и Восток", "early_modern": "XVI–XVIII века", "nineteenth": "XIX век",
    "phenomenology_existential": "Феноменология и экзистенциализм",
    "psychoanalysis_critical": "Психоанализ и критическая теория",
    "analytic": "Аналитическая философия и прагматизм",
    "poststructuralism": "Постструктурализм", "affect_theory": "Теория аффекта и наследники",
    "contemporary": "XXI век",
}
REQUIRED = ["id", "title", "tradition", "family", "period", "sortYear", "thinkers", "keyWorks", "summary", "onEmotions", "entries"]


def load():
    schools, errors = [], []
    for f in sorted(SCHOOLS.glob("*.json")):
        try:
            d = json.loads(f.read_text(encoding="utf-8"))
        except Exception as e:
            errors.append(f"{f.name}: invalid JSON: {e}")
            continue
        for k in REQUIRED:
            if k not in d:
                errors.append(f"{f.name}: missing {k}")
        if d.get("id") != f.stem:
            errors.append(f"{f.name}: id {d.get('id')!r} != file name")
        if d.get("family") not in FAMILIES:
            errors.append(f"{f.name}: unknown family {d.get('family')!r}")
        entries = d.get("entries", {})
        missing = [e for e in EMOTIONS if e not in entries]
        extra = [e for e in entries if e not in EMOTIONS]
        if missing or extra:
            errors.append(f"{f.name}: missing {missing} extra {extra}")
        for eid, e in entries.items():
            if not e.get("text", "").strip() or not e.get("practice", "").strip():
                errors.append(f"{f.name}/{eid}: empty text or practice")
            q = e.get("quote")
            if q is not None and not (isinstance(q, dict) and q.get("text") and q.get("author")):
                errors.append(f"{f.name}/{eid}: malformed quote")
        schools.append(d)
    return schools, errors


def words(s):
    return len(re.findall(r"\w+", s))


def docs(schools):
    order = list(FAMILIES)
    schools = sorted(schools, key=lambda s: (order.index(s["family"]), s["sortYear"]))
    total = sum(len(s["entries"]) for s in schools)
    quotes = sum(1 for s in schools for e in s["entries"].values() if e.get("quote"))
    out = ["# Школы и мыслители", "",
           f"База приложения «Внутри»: {len(schools)} школ × {len(EMOTIONS)} чувств = {total} текстов, {quotes} цитат.",
           "Файл сгенерирован `tools/content.py --docs` из `app/src/main/assets/content/schools/`.", ""]
    for fam in order:
        group = [s for s in schools if s["family"] == fam]
        if not group:
            continue
        out += [f"## {FAMILIES[fam]}", ""]
        for s in group:
            out += [f"### {s['title']} — {s['tradition']}", "",
                    f"*{s['period']}* · {', '.join(s['thinkers'])}", "",
                    f"**Ключевые работы:** {'; '.join(s['keyWorks'])}", "",
                    s["summary"], "", f"**О чувствах.** {s['onEmotions']}", ""]
    (ROOT / "docs").mkdir(exist_ok=True)
    (ROOT / "docs/schools.md").write_text("\n".join(out), encoding="utf-8")


if __name__ == "__main__":
    schools, errors = load()
    for s in schools:
        ws = [words(e["text"]) for e in s["entries"].values()]
        q = sum(1 for e in s["entries"].values() if e.get("quote"))
        print(f"{s['id']:<20} {s['family']:<26} entries={len(s['entries']):>2} words={min(ws)}-{max(ws)} quotes={q}")
    print(f"\n{len(schools)} schools, {len(errors)} errors")
    for e in errors:
        print("ERROR", e)
    if "--docs" in sys.argv and not errors:
        docs(schools)
        print("docs/schools.md written")
    sys.exit(1 if errors else 0)
