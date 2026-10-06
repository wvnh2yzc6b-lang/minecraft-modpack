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


def main():
    roles, names = roster()
    thumbs = {r: thumb(r) for r in RACES}
    total = done = 0
    rows = []
    for role in roles:
        tds = []
        for race in RACES:
            status, note = cell(race, role)
            total += 1
            done += status == "done"
            name = names.get(race, [])[roles.index(role)] if race in names else "?"
            chip = '<span class="chip ok">Designed</span>' if status == "done" else '<span class="chip need">Needs input</span>'
            pic = "" if status == "done" else f'<img src="{thumbs[race]}" alt="" width="24" height="48">'
            tds.append(f'<td class="{status}"><div class="unit">{pic}<div><b>{esc(name)}</b>{chip}'
                       f'<p>{esc(note)}</p></div></div></td>')
        ask = NEEDS["role_asks"].get(role, "")
        rows.append(f'<tr><th scope="row">{role.title()}<span>{esc(ask)}</span></th>{"".join(tds)}</tr>')
    head = "".join(f"<th scope=\"col\">{r.title()}</th>" for r in RACES)

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
    for k, v in {"%%UPDATED%%": NEEDS["updated"], "%%HEAD%%": head, "%%ROWS%%": "\n".join(rows),
                 "%%QUESTIONS%%": questions, "%%SETTLED%%": settled, "%%DONE%%": str(done),
                 "%%NEED%%": str(total - done), "%%TOTAL%%": str(total), "%%OPEN%%": str(len(qs)),
                 "%%HIGH%%": str(high)}.items():
        html = html.replace(k, v)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(html)
    print(OUT, f"{OUT.stat().st_size // 1024} KB")


if __name__ == "__main__":
    main()
