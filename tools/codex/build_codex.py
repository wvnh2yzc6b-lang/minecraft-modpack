#!/usr/bin/env python3
"""Builds the Warfront Codex page from the repository.

Everything on the page is read from the source tree: unit names and champion abilities from
UnitNames.java, game tests from WarfrontGameTests.java, item and block counts from the language
file, models from tools/units.py output, textures and renders from the asset and docs folders.
The build log and test descriptions live in progress.json; edit that file and re-run:

    python3 mods/warfront/tools/units.py      # refresh models and textures first
    python3 tools/codex/build_codex.py        # writes build/codex/warfront-codex.html
"""
import base64
import io
import json
import re
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
REPO = HERE.parent.parent
MOD = REPO / "mods" / "warfront"
ASSETS = MOD / "src" / "main" / "resources" / "assets" / "warfront"
T = ASSETS / "textures"
JAVA = MOD / "src" / "main" / "java" / "com" / "warfront"
ART = REPO / "docs" / "art-reference"
OUT = REPO / "build" / "codex" / "warfront-codex.html"
DATA = json.loads((HERE / "progress.json").read_text())


def uri(img, fmt="PNG"):
    b = io.BytesIO()
    img.save(b, fmt, **({"quality": 88} if fmt == "JPEG" else {}))
    return f"data:image/{fmt.lower()};base64," + base64.b64encode(b.getvalue()).decode()


def file_uri(path, max_w=2400, animate=True):
    """Embeds a render: GIFs as-is (or a still of their middle frame), stills as lossless PNG at full
    resolution so the pixel edges stay sharp, only scaled down when wider than max_w."""
    if path.suffix == ".gif" and animate:
        return "data:image/gif;base64," + base64.b64encode(path.read_bytes()).decode()
    img = Image.open(path)
    if path.suffix == ".gif":
        img.seek(getattr(img, "n_frames", 1) // 2)
    img = img.convert("RGBA")
    flat = Image.new("RGB", img.size, (26, 23, 24))
    flat.paste(img, mask=img.split()[3])
    if flat.width > max_w:
        flat = flat.resize((max_w, round(flat.height * max_w / flat.width)), Image.LANCZOS)
    return uri(flat, "PNG")


def crop(img, x, y, w, h):
    return img.crop((x, y, x + w, y + h))


def figure(skin, back=False):
    """Front or back orthographic view of a player-model skin (16x32 px)."""
    out = Image.new("RGBA", (16, 32), (0, 0, 0, 0))
    if not back:
        head, hat, body = crop(skin, 8, 8, 8, 8), crop(skin, 40, 8, 8, 8), crop(skin, 20, 20, 8, 12)
        la, ra = crop(skin, 44, 20, 4, 12), crop(skin, 36, 52, 4, 12)
        ll, rl = crop(skin, 4, 20, 4, 12), crop(skin, 20, 52, 4, 12)
    else:
        head, hat, body = crop(skin, 24, 8, 8, 8), crop(skin, 56, 8, 8, 8), crop(skin, 32, 20, 8, 12)
        la, ra = crop(skin, 44, 52, 4, 12), crop(skin, 52, 20, 4, 12)
        ll, rl = crop(skin, 28, 52, 4, 12), crop(skin, 12, 20, 4, 12)
    out.paste(head, (4, 0))
    out.alpha_composite(hat, (4, 0))
    out.paste(body, (4, 8))
    out.paste(la, (0, 8))
    out.paste(ra, (12, 8))
    out.paste(ll, (4, 20))
    out.paste(rl, (8, 20))
    return out


def pair(name):
    skin = Image.open(T / "entity/soldier" / f"{name}.png").convert("RGBA")
    canvas = Image.new("RGBA", (36, 32), (0, 0, 0, 0))
    canvas.paste(figure(skin), (0, 0))
    canvas.paste(figure(skin, True), (20, 0))
    return uri(canvas.resize((216, 192), Image.NEAREST)), uri(skin.resize((256, 256), Image.NEAREST))


def tex_tile(rel):
    return uri(Image.open(T / rel).convert("RGBA").resize((64, 64), Image.NEAREST))


def esc(s):
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


RACES = [
    ("human", "Human", "Balanced and disciplined", "0 hp · 0% speed · +0 dmg · scale 1.00", "Morale recovers fastest (1.5×). Human healers heal 5 instead of 4.", "1.0×"),
    ("elf", "Elf", "Swift and keen-eyed", "−2 hp · +10% speed · scale 1.05", "Elven archers are far more accurate (spread 2 vs 6) and deal +1 arrow damage.", "1.0×"),
    ("dwarf", "Dwarf", "Stout and stubborn", "+4 hp · −8% speed · +2 armor · scale 0.85", "Dwarven soldiers never rout.", "1.0×"),
    ("orc", "Orc", "Brutal and towering", "+2 hp · +1.5 dmg · scale 1.10", "Hits hardest of the base races, but morale regenerates slowly (0.6×).", "1.0×"),
    ("demon", "Demon", "Born of hellfire", "+2 hp · +1 dmg · +1 armor · scale 1.00", "Immune to fire and lava. Demon soldiers set targets on fire for 3s on every hit. Imps, a small winged demon species, fight as Impalers and Firecasters.", "1.15×"),
    ("angel", "Angel", "Radiant and unbreakable", "0 hp · +5% speed · scale 1.08", "No fall damage. Regenerates (players 1 HP/3s, soldiers 0.5 HP/s). +50% damage vs undead and demons. Never routs.", "1.2×"),
    ("hive", "Hive", "One mind, many bodies", "−4 hp · +12% speed · +1 armor · scale 0.90", "Never routs. +0.5 damage per nearby hive ally (max +3).", "0.6×"),
]
FACTIONS = [
    ("marauders", "Marauder Horde", "Orc", "Plains, savanna", "1.0×"),
    ("black_legion", "Black Legion", "Human", "Plains, taiga", "1.0×"),
    ("burning_horde", "Burning Horde", "Demon", "Badlands, desert, the Nether", "1.0×"),
    ("the_swarm", "The Swarm", "Hive", "Jungle, swamp, caves", "1.6×"),
    ("silverwood_reavers", "Silverwood Reavers", "Elf", "Forests", "1.0×"),
    ("ironbeard_clan", "Ironbeard Clan", "Dwarf", "Mountains, snowy biomes", "0.9×"),
    ("fallen_host", "Fallen Host", "Angel", "Mountain peaks, the End", "0.8×"),
]
ITEMS = [
    ("item/commander_baton.png", "Commander's Baton", "Issue orders and change formations"),
    ("item/war_horn.png", "War Horn", "Start or stand down a wave campaign"),
    ("item/healing_staff.png", "Healing Staff", "Heal yourself and allies within 6 blocks; 128 uses"),
    ("item/war_mark.png", "War Mark", "Currency from raiders and won waves"),
    ("item/mana_shard.png", "Mana Shard", "+10 mana in a Mana Well"),
    ("item/mana_crystal.png", "Mana Crystal", "+50 mana; builds wells and altars"),
    ("item/manabloom_seeds.png", "Manabloom Seeds", "Plant on farmland"),
]
BLOCKS = [
    ("block/arrow_tower_side.png", "Arrow Tower", "side"), ("block/arrow_tower_top.png", "Arrow Tower", "top"),
    ("block/arcane_spire_side.png", "Arcane Spire", "side"), ("block/arcane_spire_top.png", "Arcane Spire", "top"),
    ("block/healing_shrine_side.png", "Healing Shrine", "side"), ("block/healing_shrine_top.png", "Healing Shrine", "top"),
    ("block/war_standard_flag.png", "War Standard", "flag"), ("block/war_standard_pole.png", "War Standard", "pole"),
    ("block/mana_well_side.png", "Mana Well", "side"), ("block/mana_well_top_4.png", "Mana Well", "top, full"),
    ("block/mana_pylon.png", "Mana Pylon", "stone and crystal"), ("block/mana_brazier.png", "Mana Brazier", "iron and crystal"),
    ("block/summoning_altar_side.png", "Summoning Altar", "side"), ("block/summoning_altar_top.png", "Summoning Altar", "top"),
    ("block/mana_ore.png", "Mana Ore", "stone"), ("block/deepslate_mana_ore.png", "Mana Ore", "deepslate"),
    ("block/manabloom_stage0.png", "Manabloom", "age 0–1"), ("block/manabloom_stage1.png", "Manabloom", "age 2–3"),
    ("block/manabloom_stage2.png", "Manabloom", "age 4–6"), ("block/manabloom_stage3.png", "Manabloom", "ripe (7)"),
]


def race_cards():
    out = []
    for key, name, motto, stats, trait, mana in RACES:
        fig, raw = pair(key)
        out.append(f'''<article class="skin">
  <div class="plate"><img class="fig" src="{fig}" alt="{name} soldier skin, front and back" width="216" height="192"></div>
  <div class="meta"><h3>{name}</h3><p class="motto">{motto}</p><p class="stat">{stats}</p><p>{trait}</p>
    <p class="cost">Recruit cost <b>{mana}</b></p>
    <details><summary>Raw 64×64 texture</summary><img class="raw" src="{raw}" alt="{name} skin texture" width="128" height="128"></details></div>
</article>''')
    return "\n".join(out)


def faction_cards():
    out = []
    for key, name, race, home, size in FACTIONS:
        fig, raw = pair(key)
        out.append(f'''<article class="skin foe">
  <div class="plate"><img class="fig" src="{fig}" alt="{name} soldier skin, front and back" width="216" height="192"></div>
  <div class="meta"><h3>{name}</h3><p class="motto">{race} warband</p><p class="stat">Homeland: {home}</p>
    <p class="cost">Warband size <b>{size}</b></p>
    <details><summary>Raw 64×64 texture</summary><img class="raw" src="{raw}" alt="{name} skin texture" width="128" height="128"></details></div>
</article>''')
    return "\n".join(out)


def tiles(rows):
    return "\n".join(f'<figure class="tile"><img src="{tex_tile(rel)}" alt="{name}" width="64" height="64">'
                     f'<figcaption><b>{name}</b><span>{note}</span></figcaption></figure>' for rel, name, note in rows)


def role_names():
    """Parses the per-race name arrays and champion abilities out of UnitNames.java."""
    src = (JAVA / "army" / "UnitNames.java").read_text()
    roles = [m.group(1) for m in re.finditer(r"^\s{4}([A-Z_]+)\(", (JAVA / "army" / "SoldierRole.java").read_text(), re.M)]
    names = {}
    for m in re.finditer(r'String\[\] (\w+)\s*=\s*\{([^}]*)\}', src):
        names[m.group(1)] = re.findall(r'"([^"]*)"', m.group(2))
    head = "<thead><tr><th>Race</th>" + "".join(f"<th>{r.title()}</th>" for r in roles) + "</tr></thead>"
    body = []
    for race in ("HUMAN", "ELF", "DWARF", "ORC", "DEMON", "ANGEL", "HIVE"):
        row = names.get(race, [])
        soon = '<span class="status plan">soon</span>'
        cells = "".join(f"<td>{esc(row[i]) if i < len(row) else soon}</td>" for i in range(len(roles)))
        body.append(f"<tr><td><b>{race.title()}</b></td>{cells}</tr>")
    champs = re.findall(r'case \w+ -> "([^"]+)";', src.split("championAbility", 1)[1])
    return head + "<tbody>" + "".join(body) + "</tbody>", "\n".join(f"<li>{esc(c)}</li>" for c in champs), len(roles)


def tests():
    src = (JAVA / "gametest" / "WarfrontGameTests.java").read_text()
    found = re.findall(r"@GameTest[^\n]*\n\s*public static void (\w+)\(", src)
    rows = "\n".join(f"      <tr><td><code>{t}</code></td><td>{esc(DATA['tests'].get(t, re.sub(r'(?<!^)(?=[A-Z])', ' ', t).lower()))}</td></tr>"
                     for t in found)
    return rows, len(found)


STATUS = {"done": ("ok", "Done"), "progress": ("prog", "In progress"), "planned": ("plan", "Planned")}


def log():
    out = []
    for e in DATA["log"]:
        cls, label = STATUS[e["status"]]
        img = ""
        if e.get("image") and (ART / e["image"]).exists():
            img = f'<figure><img src="{file_uri(ART / e["image"], animate=False)}" alt="{esc(e["title"])}" loading="lazy"></figure>'
        out.append(f'<article class="entry"><div class="when"><span class="status {cls}">{label}</span></div>'
                   f'<div class="what"><h3>{esc(e["title"])}</h3><p>{esc(e["detail"])}</p>{img}</div></article>')
    return "\n".join(out)


def shots(items):
    out = []
    for name, caption, wide in items:
        p = ART / name
        if p.exists():
            out.append(f'<figure class="{"wide" if wide else ""}"><img src="{file_uri(p)}" alt="{esc(caption)}" loading="lazy">'
                       f'<figcaption>{esc(caption)}</figcaption></figure>')
    return "\n".join(out)


def viewer_data():
    models = {m["id"]: m for m in json.loads((MOD / "build" / "unit-models.json").read_text())}
    tex = {}
    for pal in ("demon", "burning_horde", "hive", "the_swarm"):
        for mid in models:
            if mid.startswith("imp") or mid.startswith("hive_"):
                for suffix in ("", "_glow"):
                    f = T / "entity" / "soldier" / pal / f"{mid}{suffix}.png"
                    if f.exists():
                        tex[f"{pal}/{mid}{suffix}"] = uri(Image.open(f).convert("RGBA"))
    for race in ("human", "elf", "dwarf", "orc", "angel", "hive", "demon"):
        tex[f"skin/{race}"] = uri(Image.open(T / "entity" / "soldier" / f"{race}.png").convert("RGBA"))
        for role in ("farmer", "builder", "guard"):
            for suffix in ("", "_glow"):
                f = T / "entity" / "soldier" / race / f"gear_{role}_{race}{suffix}.png"
                if f.exists():
                    tex[f"{race}/gear_{role}_{race}{suffix}"] = uri(Image.open(f).convert("RGBA"))
    for n in ("demon_skin", "demon_skin_glow", "demon_extras", "demon_extras_glow"):
        tex[f"player/{n}"] = uri(Image.open(T / "entity" / "player" / f"{n}.png").convert("RGBA"))
    # Only ship the models the viewer shows.
    keep = {k: v for k, v in models.items() if k.startswith(("imp", "hive_", "demon_player", "player_base"))
            or k.startswith("gear_") and k.rsplit("_", 1)[1] in ("human", "elf", "dwarf", "orc", "angel", "hive", "demon")}
    return {"models": keep, "tex": tex, "workerNames": worker_names()}


def worker_names():
    src = (JAVA / "army" / "UnitNames.java").read_text()
    out = {}
    for m in re.finditer(r'String\[\] (\w+)\s*=\s*\{([^}]*)\}', src):
        names = re.findall(r'"([^"]*)"', m.group(2))
        if len(names) >= 10:
            out[m.group(1).lower()] = {"farmer": names[7], "builder": names[8], "guard": names[9]}
    return out


PAGES = [
    ("home", "Home", ["progress", "gaps"]),
    ("designs", "Designs", ["designs"]),
    ("models", "Models", ["models", "demon", "imps", "hive", "workers"]),
    ("skins", "Skins", ["skins", "foes", "textures"]),
    ("races", "Races", ["races", "factions"]),
    ("army", "Army", ["roles", "command"]),
    ("mana", "Mana & defense", ["mana", "defense", "breach"]),
    ("items", "Items", ["items", "commands", "config"]),
    ("pack", "Pack", ["pack", "testing"]),
]
DESIGN_STATUS = {"approved": ("ok", "Approved"), "review": ("review", "Built, awaiting your review"),
                 "planned": ("plan", "Approved, not built")}


def designs():
    out = []
    for d in json.loads((HERE / "designs.json").read_text())["designs"]:
        cls, label = DESIGN_STATUS[d["status"]]
        pics = []
        for key, caption in (("reference", "Your reference"), ("render", "In the mod now")):
            if d.get(key) and (ART / d[key]).exists():
                pics.append(f'<figure><img src="{file_uri(ART / d[key], 1000, animate=False)}" alt="{esc(d["title"])}: {caption.lower()}" '
                            f'loading="lazy"><figcaption>{caption}</figcaption></figure>')
        notes = "".join(f"<li>{esc(n)}</li>" for n in d["notes"])
        out.append(f'<article class="design" id="design-{d["id"]}"><header><span class="race">{esc(d["race"])}</span>'
                   f'<h3>{esc(d["title"])}</h3><span class="status {cls}">{label}</span></header>'
                   f'<div class="pics">{"".join(pics)}</div><ul>{notes}</ul></article>')
    return "\n".join(out)


def dedupe_images(html):
    """Stores each embedded picture once: repeats point at it by key and a small script fills them in."""
    seen, order = {}, []

    def swap(m):
        uri_ = m.group(1)
        if uri_ not in seen:
            seen[uri_] = f"i{len(order)}"
            order.append(uri_)
        return f'src="data:image/gif;base64,R0lGODlhAQABAAAAACw=" data-img="{seen[uri_]}"'

    counts = {}
    for u in re.findall(r'src="(data:image/[^"]+)"', html):
        counts[u] = counts.get(u, 0) + 1
    repeated = {u for u, n in counts.items() if n > 1}
    if not repeated:
        return html
    html = re.sub(r'src="(data:image/[^"]+)"', lambda m: swap(m) if m.group(1) in repeated else m.group(0), html)
    table = ",".join(f'"{seen[u]}":"{u}"' for u in order)
    script = ("<script>(function(){const I={" + table + "};document.querySelectorAll('img[data-img]')"
              ".forEach(i=>{i.src=I[i.dataset.img];});})();</script>")
    return html.replace("</footer>", "</footer>\n" + script, 1)


def main():
    lang = json.loads((ASSETS / "lang" / "en_us.json").read_text())
    n_items = sum(k.startswith("item.warfront.") for k in lang)
    n_blocks = sum(k.startswith("block.warfront.") for k in lang)
    names_table, champions, n_roles = role_names()
    test_rows, n_tests = tests()
    models = viewer_data()
    n_models = sum(1 for m in json.loads((MOD / "build" / "unit-models.json").read_text()) if m["id"] != "player_base")
    tally = "".join(f"<span><b>{v}</b>{k}</span>" for k, v in [
        ("playable races", 7), ("enemy factions", 7), ("unit roles", n_roles), ("formations", 5),
        ("custom 3D models", n_models), ("blocks", n_blocks), ("items", n_items), ("game tests", n_tests),
        ("mods in the pack", DATA["pack_mods"])])
    html = (HERE / "template.html").read_text()
    subs = {
        "%%UPDATED%%": DATA["updated"],
        "%%BUILDLINE%%": f"The mod compiles and passes {n_tests} automated in-game tests on a dedicated server. Visuals are checked in preview renders, not yet in a game client.",
        "%%TALLY%%": tally,
        "%%LOG%%": log(),
        "%%DEMON_SHOTS%%": shots([
            ("demon-flight.gif", "Takeoff, climb, cruise and dive, using the game's wing animation.", True),
            ("demon-player-no-claws.png", "Current look: front, three-quarter, side and close-up.", True),
            ("demon-player-wing-fold.png", "Wing states: folded, half-open, full spread, and the up and down strokes.", False),
            ("demon-head-oryx.png", "Head reference.", False),
        ]),
        "%%IMP_SHOTS%%": shots([
            ("imp-model-preview.png", "Imp roles, side by side.", True),
            ("demon-imp.png", "Imp reference.", False),
        ]),
        "%%HIVE_SHOTS%%": shots([
            ("hive-units-render.png", "Lancer-Drone, then the Deepmaw, each in the Hive's colors and the Swarm's: front, side and back.", True),
            ("hive-beast-lobster.png", "The Deepmaw as a lobster centaur: front, side and top in the Hive's and the Swarm's colors, then the face next to the reference.", True),
            ("hive-spearman.png", "Spearman reference.", False),
            ("hive-beast.png", "Beast reference.", False),
        ]),
        "%%DESIGNS%%": designs(),
        "%%PAGENAV%%": "".join(f'<a href="#{pid}" data-page="{pid}">{esc(label)}</a>' for pid, label, _ in PAGES),
        "%%PAGES%%": json.dumps([{"id": pid, "label": label, "sections": secs} for pid, label, secs in PAGES]),
        "%%WORKER_SHOTS%%": shots([("workers-all-races.png", "Farmers, builders and guards for every race, front and back.", True)]),
        "%%RACES%%": race_cards(),
        "%%FACTIONS%%": faction_cards(),
        "%%ITEMS%%": tiles(ITEMS),
        "%%BLOCKS%%": tiles(BLOCKS),
        "%%NAMES%%": names_table,
        "%%CHAMPIONS%%": champions,
        "%%TESTS%%": test_rows,
        "%%CI%%": "Every push to the branch runs the full pipeline.",
        "%%MCJS%%": (HERE / "mcmodel.js").read_text(),
        "%%WINGJS%%": (HERE / "wing_animator.js").read_text(),
        "%%VIEWERJS%%": (HERE / "viewer.js").read_text(),
        "%%DATA%%": json.dumps(models, separators=(",", ":")),
    }
    for k, v in subs.items():
        html = html.replace(k, v)
    html = dedupe_images(html)
    left = re.findall(r"%%[A-Z_]+%%", html)
    if left:
        raise SystemExit(f"unfilled placeholders: {left}")
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(html)
    print(OUT, f"{len(html) / 1e6:.1f} MB")


if __name__ == "__main__":
    main()
