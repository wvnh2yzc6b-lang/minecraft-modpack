"""Builds the Warfront Gallery: the current picture of every model, skin, tower, block and item, newest first.

Renders every model with render_gallery.js (Chromium + three.js, the game's own cube rules), then writes
build/codex/warfront-gallery.html. Run after `python3 mods/warfront/tools/units.py`. Needs three.js:
  npm install three@0.160.0 --prefix <scratchpad>/render   and pass its three.min.js as THREE_JS, e.g.
  THREE_JS=<scratchpad>/render/node_modules/three/build/three.min.js python3 tools/codex/build_gallery.py
Without THREE_JS it reuses the renders already in build/gallery/ (and fails on any that are missing).
"""
import base64, html, json, os, re, subprocess, sys
from datetime import date
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
MOD = ROOT / "mods" / "warfront"
TEX = MOD / "src" / "main" / "resources" / "assets" / "warfront" / "textures"
ENT = TEX / "entity"
OUT = ROOT / "build" / "codex" / "warfront-gallery.html"
RENDERS = ROOT / "build" / "gallery"
LANG = json.loads((MOD / "src" / "main" / "resources" / "assets" / "warfront" / "lang" / "en_us.json").read_text())
NEEDS = json.loads((HERE / "needs.json").read_text())["cells"]

RACES = ["human", "elf", "dwarf", "orc", "demon", "angel", "hive"]
FACTIONS = {"marauders": ("Marauder Horde", "orc"), "black_legion": ("Black Legion", "human"),
            "burning_horde": ("Burning Horde", "demon"), "the_swarm": ("The Swarm", "hive"),
            "silverwood_reavers": ("Silverwood Reavers", "elf"), "ironbeard_clan": ("Ironbeard Clan", "dwarf"),
            "fallen_host": ("Fallen Host", "angel")}
WORKERS = {"human": ("Farmhand", "Mason"), "elf": ("Grovetender", "Treewright"), "dwarf": ("Hearthfarmer", "Stonemason"),
           "orc": ("Goblin Grubber", "Goblin Tinker"), "demon": ("Ash Tiller", "Brimstone Mason"),
           "angel": ("Lightgardener", "Sanctum Builder"), "hive": ("Harvester Drone", "Builder Drone")}


def t(folder, model):
    return str(ENT / "soldier" / folder / f"{model}.png")


def layer(model, tex):
    return {"model": model, "tex": tex, "glow": tex.replace(".png", "_glow.png")}


def body(skin_png):
    return {"model": "player_base", "tex": str(skin_png)}


def status(key):
    cell = NEEDS.get(key) or NEEDS.get("*/" + key.split("/")[1], {})
    s = cell.get("status")
    note = cell.get("note", "")
    if s == "done":
        return "approved"
    if "placeholder" in note.lower():
        return "placeholder"
    return "review" if s == "partial" else "placeholder"


def subjects():
    """Every picture on the page: (section, id, name, line, status, layers, view)."""
    out = []
    add = lambda sec, sid, name, line, st, layers, view="three": out.append(
        dict(section=sec, id=sid, name=name, line=line, status=st, layers=layers, view=view))
    # War beasts
    for sid, model, folder, name, line, key in [
        ("beast_hive", "hive_beast", "hive", "Deepmaw", "Hive war beast", "hive/beast"),
        ("beast_swarm", "hive_beast", "the_swarm", "Deepmaw", "The Swarm", "hive/beast"),
        ("beast_demon", "demon_beast", "demon", "Bone Stalker", "Demon war beast", "demon/beast"),
        ("beast_horde", "demon_beast", "burning_horde", "Bone Stalker", "Burning Horde", "demon/beast"),
        ("beast_angel", "angel_moa", "angel", "Armored Moa", "Angel war beast", "angel/beast"),
        ("beast_fallen", "angel_moa", "fallen_host", "Armored Moa", "Fallen Host", "angel/beast")]:
        add("beasts", sid, name, line, status(key), [layer(model, t(folder, model))])
    # Soldiers with their own models or armor
    for sid, model, folder, name, line, key in [
        ("imp_impaler", "imp_impaler", "demon", "Imp Impaler", "Demon spearman", "demon/spearman"),
        ("imp_firecaster", "imp_firecaster", "demon", "Imp Firecaster", "Demon archer", "demon/archer"),
        ("imp_impaler_horde", "imp_impaler", "burning_horde", "Imp Impaler", "Burning Horde", "demon/spearman"),
        ("imp_firecaster_horde", "imp_firecaster", "burning_horde", "Imp Firecaster", "Burning Horde", "demon/archer"),
        ("lancer_hive", "hive_lancer", "hive", "Lancer-Drone", "Hive spearman", "hive/spearman"),
        ("lancer_swarm", "hive_lancer", "the_swarm", "Lancer-Drone", "The Swarm", "hive/spearman")]:
        add("soldiers", sid, name, line, status(key), [layer(model, t(folder, model))])
    for role, name in [("shieldbearer", "Hellguard"), ("swordsman", "Fiend"), ("captain", "Archfiend"),
                       ("champion", "Hellknight"), ("healer", "Blood Witch")]:
        add("soldiers", f"demon_{role}", name, f"Demon {role}", status(f"demon/{role}"),
            [body(ENT / "soldier" / "demon.png"), layer(f"gear_{role}_demon", t("demon", f"gear_{role}_demon"))])
    # Players
    add("players", "player_demon", "Demon player", "Horns, wings, tail and claws", "approved",
        [body(ENT / "player" / "demon_skin.png"), {"model": "demon_player", "tex": str(ENT / "player" / "demon_extras.png"),
                                                   "glow": str(ENT / "player" / "demon_extras_glow.png")}])
    for r in RACES:
        add("players", f"skin_{r}", r.capitalize(), "Race skin", "placeholder", [body(ENT / "soldier" / f"{r}.png")], "front")
    # Workers
    for r in RACES:
        farmer, builder = WORKERS[r]
        base = ENT / "soldier" / ("goblin.png" if r == "orc" else f"{r}.png")
        gear = "goblin" if r == "orc" else r
        for role, name in (("farmer", farmer), ("builder", builder)):
            add("workers", f"{role}_{r}", name, f"{r.capitalize()} {role}", status(f"{r}/{role}"),
                [body(base), layer(f"gear_{role}_{gear}", t(r, f"gear_{role}_{gear}"))])
    # Enemy factions
    for f, (name, race) in FACTIONS.items():
        add("factions", f"trim_{f}", name, f"{race.capitalize()} raiders", "review",
            [body(ENT / "soldier" / f"{f}.png"), layer(f"trim_{f}", t(f, f"trim_{f}"))])
    # Flight gear
    for sid, model, race, name in [("glider", "mana_glider", "human", "Mana Glider"), ("rocket", "rocket_pack", "orc", "Orc Rocket Pack"),
                                   ("wings", "angel_wings", "angel", "Angel Wings")]:
        add("flight", sid, name, f"{race.capitalize()} flight gear", "placeholder",
            [body(ENT / "soldier" / f"{race}.png"), layer(model, t("glider", model))])
    # Towers
    for model, key in [("tower_arrow", "arrow_tower"), ("tower_arcane", "arcane_spire"), ("tower_healing", "healing_shrine")]:
        for r in RACES:
            add("towers", f"{model}_{r}", LANG.get(f"block.warfront.{key}", key), r.capitalize(), "placeholder",
                [layer(model, t(f"tower_{r}", model))])
    for r in RACES:
        for png in sorted((ENT / "soldier" / f"tower_{r}").glob("rt_*.png")):
            if png.stem.endswith("_glow"):
                continue
            key = png.stem[3:]
            add("racetowers", png.stem, LANG.get(f"block.warfront.{key}", key.replace("_", " ").title()), r.capitalize(),
                "placeholder", [layer(png.stem, str(png))])
    return out


SECTIONS = [("beasts", "War beasts", "Rare altar units, up to the beast limit. Each race's version, then its enemy faction's."),
            ("soldiers", "Soldiers", "Units with their own model or armor. The rest still wear their plain race skin."),
            ("players", "Players", "The demon player model, and the plain skin every other race wears for now."),
            ("workers", "Workers", "Plain humanoid farmers and builders in every race; orc workers are goblins."),
            ("factions", "Enemy factions", "Each raiding faction's skin with its signature trim piece."),
            ("flight", "Flight gear", "How each race takes to the air."),
            ("towers", "Shared towers", "The three towers every race can build, styled in the owner's race."),
            ("racetowers", "Race towers", "Each race's own towers at base levels 2, 3 and 5."),
            ("blocks", "Blocks and items", "Every block and item texture in the mod.")]
STATUS = {"approved": "Approved", "review": "Waiting on review", "placeholder": "Placeholder"}


def git_date(paths):
    """Newest commit date that touched any of these files (today if uncommitted)."""
    newest = ""
    for p in paths:
        rel = os.path.relpath(p, ROOT)
        dirty = subprocess.run(["git", "status", "--porcelain", "--", rel], cwd=ROOT, capture_output=True, text=True).stdout.strip()
        d = date.today().isoformat() if dirty else subprocess.run(
            ["git", "log", "-1", "--format=%cs", "--", rel], cwd=ROOT, capture_output=True, text=True).stdout.strip()
        newest = max(newest, d)
    return newest or date.today().isoformat()


def data_uri(path):
    return "data:image/png;base64," + base64.b64encode(Path(path).read_bytes()).decode()


def render(subs):
    three = os.environ.get("THREE_JS")
    if not three:
        missing = [s["id"] for s in subs if not (RENDERS / f"{s['id']}.png").exists()]
        if missing:
            sys.exit(f"No THREE_JS set and {len(missing)} renders are missing (e.g. {missing[0]}). See the docstring.")
        return
    jobs = RENDERS.parent / "gallery-jobs.json"
    jobs.parent.mkdir(parents=True, exist_ok=True)
    jobs.write_text(json.dumps([{"id": s["id"], "layers": s["layers"], "view": s["view"]} for s in subs]))
    subprocess.run(["node", str(HERE / "render_gallery.js"), str(jobs), str(RENDERS), three], check=True)


def tiles():
    out = []
    for kind in ("block", "item"):
        for png in sorted((TEX / kind).glob("*.png")):
            if png.stem.endswith(("_glow", "_side", "_bottom")) or re.search(r"_stage\d", png.stem):
                continue
            stem = re.sub(r"_(top|front)$", "", png.stem)
            name = LANG.get(f"{kind}.warfront.{stem}") or LANG.get(f"block.warfront.{stem}") or LANG.get(f"item.warfront.{stem}")
            if not name:
                continue
            out.append(dict(name=name, kind=kind, uri=data_uri(png), date=git_date([png])))
    seen, uniq = set(), []
    for x in out:
        if x["name"] not in seen:
            seen.add(x["name"]); uniq.append(x)
    return uniq


def main():
    subs = subjects()
    render(subs)
    for s in subs:
        s["date"] = git_date([l["tex"] for l in s["layers"]])
        s["uri"] = data_uri(RENDERS / f"{s['id']}.png")
    tl = tiles()
    latest = sorted(subs, key=lambda s: s["date"], reverse=True)[:8]

    def card(s, small=False):
        return (f'<figure class="card{" small" if small else ""}"><button class="plate" type="button" data-full="{s["id"]}" '
                f'aria-label="Enlarge {html.escape(s["name"])}"><img src="{s["uri"]}" alt="{html.escape(s["name"])}, {html.escape(s["line"])}" '
                f'width="360" height="360" loading="lazy" id="img-{s["id"]}"></button><figcaption><b>{html.escape(s["name"])}</b>'
                f'<span class="line">{html.escape(s["line"])}</span><span class="meta"><span class="st {s["status"]}">'
                f'{STATUS[s["status"]]}</span><time datetime="{s["date"]}">{fmt(s["date"])}</time></span></figcaption></figure>')

    sections = []
    for key, title, blurb in SECTIONS:
        if key == "blocks":
            body_html = '<div class="tiles">' + "".join(
                f'<figure class="tile"><img src="{x["uri"]}" alt="" width="16" height="16"><figcaption>{html.escape(x["name"])}'
                f'<span>{x["kind"]}</span></figcaption></figure>' for x in tl) + "</div>"
            count = len(tl)
        else:
            items = [s for s in subs if s["section"] == key]
            count = len(items)
            body_html = f'<div class="grid{" dense" if key in ("towers", "racetowers", "players") else ""}">' + "".join(
                card(s, key in ("towers", "racetowers")) for s in items) + "</div>"
        sections.append(f'<section id="{key}"><header><h2>{title}</h2><span class="count">{count}</span><p>{blurb}</p></header>{body_html}</section>')

    nav = "".join(f'<a href="#{k}">{t_}</a>' for k, t_, _ in SECTIONS)
    counts = {k: sum(1 for s in subs if s["status"] == k) for k in STATUS}
    page = (HERE / "gallery_template.html").read_text()
    for k, v in {"%%UPDATED%%": fmt(date.today().isoformat()), "%%TOTAL%%": str(len(subs) + len(tl)),
                 "%%MODELS%%": str(len(subs)), "%%APPROVED%%": str(counts["approved"]), "%%REVIEW%%": str(counts["review"]),
                 "%%PLACEHOLDER%%": str(counts["placeholder"]), "%%TILES%%": str(len(tl)),
                 "%%LATEST%%": "".join(card(s) for s in latest), "%%NAV%%": nav, "%%SECTIONS%%": "".join(sections)}.items():
        page = page.replace(k, v)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(page)
    print(OUT, f"{len(page) / 1e6:.1f} MB,", len(subs), "renders,", len(tl), "tiles")


def fmt(iso):
    d = date.fromisoformat(iso)
    return d.strftime("%b ") + str(d.day)


if __name__ == "__main__":
    main()
