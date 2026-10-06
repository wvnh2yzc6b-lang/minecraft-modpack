#!/usr/bin/env python3
"""Builds the Design Needs page: every race and role, which ones are designed, and what the pack owner
still needs to decide. Unit names come from UnitNames.java and roles from SoldierRole.java; statuses
and open questions live in needs.json. Edit that file and re-run:

    python3 tools/codex/build_needs.py        # writes build/codex/design-needs.html
"""
import json
import re

import build_codex as codex
from build_codex import JAVA, esc, figure, uri
from PIL import Image

HERE = codex.HERE
OUT = codex.REPO / "build" / "codex" / "design-needs.html"
NEEDS = json.loads((HERE / "needs.json").read_text())
RACES = ["human", "elf", "dwarf", "orc", "demon", "angel", "hive"]


def roster():
    roles = [m.group(1).lower() for m in
             re.finditer(r"^\s{4}([A-Z_]+)\(", (JAVA / "army" / "SoldierRole.java").read_text(), re.M)]
    src = (JAVA / "army" / "UnitNames.java").read_text()
    names = {m.group(1).lower(): re.findall(r'"([^"]*)"', m.group(2))
             for m in re.finditer(r'String\[\] (\w+)\s*=\s*\{([^}]*)\}', src)}
    return roles, names


def cell(race, role):
    cells = NEEDS["cells"]
    c = cells.get(f"{race}/{role}") or cells.get(f"*/{role}")
    return (c["status"], c["note"]) if c else ("need", NEEDS["default_note"])


def thumb(race):
    skin = Image.open(codex.T / "entity/soldier" / f"{race}.png").convert("RGBA")
    return uri(figure(skin).resize((48, 96), Image.NEAREST))


RACE_LABEL = {"human": "Human", "elf": "Elf", "dwarf": "Dwarf", "orc": "Orc", "demon": "Demon", "angel": "Angel", "hive": "Hive"}
GROUPS = [("need", "Needs a design", "need"), ("partial", "Partly done", "part"), ("done", "Designed", "ok")]


def race_section(race, roles, names, thumb_uri):
    units = {"need": [], "partial": [], "done": []}
    for i, role in enumerate(roles):
        status, note = cell(race, role)
        name = names.get(race, [])[i] if i < len(names.get(race, [])) else role.title()
        ask = NEEDS["role_asks"].get(role, "")
        units[status].append(f'<li><b>{esc(name)}</b> <span class="role">{role.title()}</span>'
                             f'<p>{esc(note if status != "need" else ask)}</p></li>')
    blocks = []
    for key, label, cls in GROUPS:
        if units[key]:
            blocks.append(f'<div class="group {cls}"><h3><span class="chip {cls}">{label}</span> '
                          f'<span class="count">{len(units[key])}</span></h3><ul>{"".join(units[key])}</ul></div>')
    need = len(units["need"]) + len(units["partial"])
    return (f'<article class="race" id="{race}"><header><img src="{thumb_uri}" alt="" width="36" height="72">'
            f'<div><h2>{RACE_LABEL[race]}</h2><p>{need} of {len(roles)} units still need design work</p></div></header>'
            f'{"".join(blocks)}</article>'), units


def main():
    roles, names = roster()
    total = done = partial = 0
    sections, nav = [], []
    for race in RACES:
        html_, units = race_section(race, roles, names, thumb(race))
        sections.append(html_)
        total += len(roles)
        done += len(units["done"])
        partial += len(units["partial"])
        nav.append(f'<a href="#{race}">{RACE_LABEL[race]} <span>{len(units["need"]) + len(units["partial"])}</span></a>')

    order = {"high": 0, "medium": 1, "low": 2}
    qs = sorted(NEEDS["questions"], key=lambda q: order[q["priority"]])
    questions = "\n".join(
        f'<li class="q {q["priority"]}"><span class="pri">{q["priority"].title()}</span>'
        f'<div><span class="topic">{esc(q["topic"])}</span><h3>{esc(q["question"])}</h3><p>{esc(q["why"])}</p></div></li>'
        for q in qs)
    settled = "\n".join(f'<li><span class="topic">{esc(s["topic"])}</span> {esc(s["answer"])}</li>'
                        for s in NEEDS["settled"])
    high = sum(q["priority"] == "high" for q in qs)

    html = (HERE / "needs_template.html").read_text()
    for k, v in {"%%UPDATED%%": NEEDS["updated"], "%%RACES%%": "\n".join(sections), "%%NAV%%": "".join(nav),
                 "%%QUESTIONS%%": questions, "%%SETTLED%%": settled, "%%DONE%%": str(done),
                 "%%NEED%%": str(total - done - partial), "%%PARTIAL%%": str(partial), "%%TOTAL%%": str(total), "%%OPEN%%": str(len(qs)),
                 "%%HIGH%%": str(high)}.items():
        html = html.replace(k, v)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(html)
    print(OUT, f"{OUT.stat().st_size // 1024} KB")


if __name__ == "__main__":
    main()
