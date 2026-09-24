#!/usr/bin/env python3
"""Build the web (PWA) version into web/dist from web/src plus the app's own data.

Content, the 66 states, UI strings, offline recognition cues and crisis patterns are all taken from the Android
sources, so both versions always say the same thing. Usage: python3 tools/build_web.py
"""
import hashlib
import json, pathlib, re, shutil

ROOT = pathlib.Path(__file__).resolve().parent.parent
KT = ROOT / "app/src/main/java/com/ilyamalshv/vnutri"
ASSETS = ROOT / "app/src/main/assets/content"
RES = ROOT / "app/src/main/res"
SRC = ROOT / "web/src"
DIST = ROOT / "web/dist"

LANGS = {"ru": "schools", "en": "schools_en", "es": "schools_es"}
GROUP_COLORS = {"fear": "#8C9BFF", "loss": "#63C9DA", "others": "#FF6B5E", "self": "#FFB84D",
                "drained": "#B9ADE0", "body": "#FF63A8", "awkward": "#A6E77F", "light": "#FFDF8A"}


def kt_unescape(s):
    return s.encode("utf-8").decode("unicode_escape").encode("latin-1").decode("utf-8")


def content(lang):
    folder = ASSETS / LANGS[lang]
    out = []
    for f in sorted(folder.glob("*.json")):
        d = json.loads(f.read_text(encoding="utf-8"))
        d["lenses"] = d.pop("entries")
        out.append(d)
    return {"schools": out}


def meta():
    src = (KT / "data/Content.kt").read_text(encoding="utf-8")
    emotions = [{"id": i, "group": g, "name": {"ru": ru, "en": en, "es": es}}
                for i, ru, g, en, es in re.findall(r'Emotion\("(\w+)", "([^"]+)", "(\w+)", "([^"]+)", "([^"]+)"\)', src)]
    groups = []
    for g, ru, en in re.findall(r'"(\w+)" to tr\("([^"]+)", "([^"]+)"\)', src):
        groups.append({"id": g, "name": {"ru": ru, "en": en}, "color": GROUP_COLORS[g]})
    families = [{"id": i, "name": {"ru": ru, "en": en, "es": es}}
                for i, ru, en, es in re.findall(r'Family\("(\w+)", "([^"]+)", "([^"]+)", "([^"]+)"\)', src)]
    # UI strings: every tr("ru", "en") in the app + the Spanish dictionary keyed by English
    strings = {}
    for f in KT.rglob("*.kt"):
        for ru, en in re.findall(r'tr\(\s*"((?:[^"\\]|\\.)*)"\s*,\s*"((?:[^"\\]|\\.)*)"', f.read_text(encoding="utf-8")):
            if "$" not in ru:
                strings[kt_unescape(ru)] = {"en": kt_unescape(en)}
    es_src = (KT / "data/EsStrings.kt").read_text(encoding="utf-8")
    es_map = {kt_unescape(a): kt_unescape(b) for a, b in re.findall(r'^\s*"((?:[^"\\]|\\.)*)" to "((?:[^"\\]|\\.)*)",$', es_src, re.M)}
    for v in strings.values():
        v["es"] = es_map.get(v["en"], v["en"])
    for gr in groups:
        gr["name"]["es"] = es_map.get(gr["name"]["en"], gr["name"]["en"])
    # Offline recognition cues, per language
    guess_src = (KT / "data/EmotionGuess.kt").read_text(encoding="utf-8")
    cues = {}
    for name, lang in [("rules", "ru"), ("rulesEn", "en"), ("rulesEs", "es")]:
        block = guess_src[guess_src.index(f"val {name}: List"):]
        block = block[:block.index("\n    )\n")]
        cues[lang] = {i: [kt_unescape(x) for x in re.findall(r'"((?:[^"\\]|\\.)*)"', body)]
                      for i, body in re.findall(r'"(\w+)" to [we]\((.*?)\),\n', block)}
    broad = re.findall(r'"(\w+)"', guess_src[guess_src.index("private val broad"):guess_src.index(")", guess_src.index("private val broad"))])
    # Crisis patterns
    safety = (KT / "data/Safety.kt").read_text(encoding="utf-8")
    block = safety[safety.index("private val patterns = listOf("):safety.index(").map { Regex(it) }")]
    crisis = [kt_unescape(x) for x in re.findall(r'"((?:[^"\\]|\\.)*)"', block)]
    heavy = re.findall(r'"(\w+)"', safety[safety.index("private val heavy"):safety.index(")", safety.index("private val heavy"))])
    assert len(emotions) == 66 and len(groups) == 8 and families, (len(emotions), len(groups))
    return {"emotions": emotions, "groups": groups, "families": families, "strings": strings,
            "cues": cues, "broad": broad, "crisis": crisis, "heavy": heavy}


def main():
    if DIST.exists():
        shutil.rmtree(DIST)
    shutil.copytree(SRC, DIST)
    data = DIST / "data"
    data.mkdir(exist_ok=True)
    for lang in LANGS:
        (data / f"content_{lang}.json").write_text(json.dumps(content(lang), ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    (data / "meta.json").write_text(json.dumps(meta(), ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    snd = DIST / "sound"; snd.mkdir(exist_ok=True)
    for n in ["ambient", "tap", "select", "confirm"]:
        shutil.copy(RES / f"raw/{n}.ogg", snd / f"{n}.ogg")
        mp3 = ROOT / f"web/audio/{n}.mp3"
        if mp3.exists():
            shutil.copy(mp3, snd / f"{n}.mp3")
    icons = DIST / "icons"; icons.mkdir(exist_ok=True)
    for size, d in [(192, "xxxhdpi"), (512, None)]:
        src = ROOT / "web/icons" / f"icon-{size}.png"
        if src.exists():
            shutil.copy(src, icons / f"icon-{size}.png")
    # A new cache name whenever anything changes, so installed copies pick up new texts.
    h = hashlib.sha1()
    for f in sorted(DIST.rglob("*")):
        if f.is_file() and f.name != "sw.js":
            h.update(f.relative_to(DIST).as_posix().encode()); h.update(f.read_bytes())
    sw = DIST / "sw.js"
    sw.write_text(sw.read_text(encoding="utf-8").replace("sincerer-v1", "sincerer-" + h.hexdigest()[:10]), encoding="utf-8")
    total = sum(f.stat().st_size for f in DIST.rglob("*") if f.is_file())
    print(f"web/dist built: {total / 1e6:.1f} MB")


if __name__ == "__main__":
    main()
