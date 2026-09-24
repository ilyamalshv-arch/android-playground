#!/usr/bin/env python3
"""Check a school file for formulaic writing. Usage: python3 tools/variety.py FILE [FILE...]

Fails (exit 1) if any rule is broken; prints what to fix.
"""
import json, re, sys, collections

HEDGES = ["с этой точки зрения", "можно сказать", "from this point of view", "one could say", "from this angle"]
CLICHES = ["имеет право быть", "понятная реакция", "это нормально", "знакомо многим", "это не слабость",
           "has a right to exist", "understandable reaction", "is normal", "familiar to many"]
STATE_WORDS = {'anxiety': ('тревога', 'anxiety'), 'fear': ('страх', 'fear'), 'panic': ('паника', 'panic'), 'dread': ('экзистенциальный', 'existential'), 'fomo': ('страх', 'fear'), 'sleepless': ('бессонница', 'sleepless'), 'social_anxiety': ('стеснение', 'shyness'), 'uncertainty': ('неопределённость', 'uncertainty'), 'waiting': ('ожидание', 'waiting'), 'sadness': ('грусть', 'sadness'), 'grief': ('горе', 'grief'), 'loneliness': ('одиночество', 'loneliness'), 'heartbreak': ('разбитое', 'heartbreak'), 'uprooted': ('оторванность', 'uprooted'), 'regret': ('сожаление', 'regret'), 'longing': ('тоска', 'missing'), 'world_pain': ('боль', 'pain'), 'anger': ('злость', 'anger'), 'resentment': ('обида', 'resentment'), 'betrayal': ('предательство', 'betrayal'), 'jealousy': ('ревность', 'jealousy'), 'envy': ('зависть', 'envy'), 'rejection': ('отвержение', 'rejection'), 'abandonment': ('страх', 'fear'), 'unrequited': ('безответная', 'unrequited'), 'humiliation': ('унижение', 'humiliation'), 'shame': ('стыд', 'shame'), 'guilt': ('вина', 'guilt'), 'self_hatred': ('ненависть', 'self-hatred'), 'impostor': ('синдром', 'impostor'), 'helplessness': ('беспомощность', 'helplessness'), 'confusion': ('растерянность', 'confusion'), 'body_shame': ('недовольство', 'unhappy'), 'fatigue': ('усталость', 'fatigue'), 'emptiness': ('пустота', 'emptiness'), 'boredom': ('скука', 'boredom'), 'overload': ('перегруз', 'overload'), 'procrastination': ('не', "can't"), 'unreality': ('всё', 'nothing'), 'craving': ('тяга', 'craving'), 'addiction': ('зависимость', 'addiction'), 'hangover': ('похмелье', 'hangover'), 'euphoria': ('вспышка', 'burst'), 'crash': ('резкий', 'sudden'), 'illness': ('болезнь', 'illness'), 'schadenfreude': ('злорадство', 'schadenfreude'), 'contempt': ('презрение', 'contempt'), 'disgust': ('отвращение', 'disgust'), 'revenge': ('желание', 'wanting'), 'anger_at_loved': ('злость', 'anger'), 'ambivalence': ('люблю', 'love'), 'guilty_relief': ('облегчение', 'relief'), 'indifference': ('равнодушие', 'indifference'), 'escape': ('всё', 'wanting'), 'forbidden_attraction': ('влечение', 'an'), 'nostalgia': ('ностальгия', 'nostalgia'), 'melancholy': ('светлая', 'bright'), 'pride': ('гордость', 'pride'), 'joy': ('радость', 'joy'), 'love': ('любовь', 'love'), 'gratitude': ('благодарность', 'gratitude'), 'hope': ('надежда', 'hope'), 'awe': ('трепет', 'awe'), 'relief': ('облегчение', 'relief'), 'calm': ('покой', 'calm'), 'inspiration': ('вдохновение', 'inspiration')}

WRITE_VERBS = ["запишите", "напишите", "допишите", "write down", "write ", "jot"]


def first_sentence(s):
    return re.split(r"(?<=[.!?…])\s", s.strip())[0]


def check(path):
    d = json.load(open(path, encoding="utf-8"))
    entries = d["entries"]
    problems = []
    n = len(entries)
    # 1) No entry may open by defining the state: its first words must not be shared with 3+ other entries' openers
    openers = collections.Counter(" ".join(first_sentence(e["text"]).lower().split()[:2]) for e in entries.values())
    for k, e in entries.items():
        o = " ".join(first_sentence(e["text"]).lower().split()[:2])
        if openers[o] >= 4:
            problems.append(f"{k}: opener «{o}…» used {openers[o]} times in this file")
    # 1b) No entry may open with the name of the state itself («Тоска по…», «Гордость — …»)
    for k, e in entries.items():
        head = " ".join(e["text"].lower().split()[:3])
        for w in STATE_WORDS.get(k, ()):
            if len(w) > 3 and w[:5] in head:
                problems.append(f"{k}: opens with the state's own name («{head}…») — start from the school instead")
    # 2) Hedges: at most 8 per file
    hedges = sum(e["text"].lower().count(h) for e in entries.values() for h in HEDGES)
    if hedges > 8:
        problems.append(f"hedges («с этой точки зрения»/«можно сказать») used {hedges} times; max 8")
    # 3) Clichés: none
    for k, e in entries.items():
        for c in CLICHES:
            if c in e["text"].lower():
                problems.append(f"{k}: cliché «{c}»")
    # 4) Practices: writing-type at most 35 %, and no 3-word practice opening repeated more than twice
    writing = sum(any(v in e["practice"].lower() for v in WRITE_VERBS) for e in entries.values())
    if writing > 0.35 * n:
        problems.append(f"{writing}/{n} practices are writing tasks; max {int(0.35 * n)}")
    popen = collections.Counter(" ".join(re.sub(r"[^\w ]", "", e["practice"].lower()).split()[:3]) for e in entries.values())
    for p, c in popen.items():
        if c > 2:
            problems.append(f"practice opening «{p}…» used {c} times; max 2")
    return problems


if __name__ == "__main__":
    bad = 0
    for f in sys.argv[1:]:
        pr = check(f)
        print(("OK  " if not pr else "FIX ") + f + ("" if not pr else f"  ({len(pr)} issues)"))
        for p in pr[:40]:
            print("    -", p)
        bad += bool(pr)
    sys.exit(1 if bad else 0)
