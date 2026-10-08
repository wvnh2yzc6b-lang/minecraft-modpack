"""3D tower models: the Arrow Tower, Arcane Spire and Healing Shrine, drawn by TowerRenderer on top of the tower
block (the block is the plinth; these rise about two blocks above it). One shared shape per tower, painted in each
race's materials and trim from the owner's race. Imported by units.py.

Model space: pixels, y down; y=24 is the top of the tower block, y=-8 two blocks above it.
"""
# part, box and the pixel helpers are injected by units.py (see bind()).

# race: stone, stone_dark, wood, wood_dark, trim, accent (banner/cloth), roof, glow, glow_core
PALETTES = {
    "human": dict(stone="9a9a96", stone_dark="6c6c68", wood="8a5a30", wood_dark="5c3a1c", trim="d4a017", accent="b8231a",
                  roof="4a5f8a", glow="6ee8ff", glow_core="d8fbff"),
    "elf": dict(stone="b8c4a0", stone_dark="7f8c68", wood="c9b98a", wood_dark="7a6a40", trim="7fd06a", accent="2f7f3a",
                roof="3f8a3a", glow="9ff5a0", glow_core="eaffea"),
    "dwarf": dict(stone="5a5f66", stone_dark="3a3e44", wood="6a4a2a", wood_dark="40281a", trim="b08a3a", accent="2f5fa8",
                  roof="4a4f57", glow="5fd4e8", glow_core="d8fbff"),
    "orc": dict(stone="6b6a5a", stone_dark="45443a", wood="7a5a38", wood_dark="4a3420", trim="a9b0b5", accent="6a8a2a",
                roof="5a3a20", glow="ff8a2a", glow_core="ffe0a0"),
    "demon": dict(stone="3a2e2e", stone_dark="1e1616", wood="2a2222", wood_dark="140e0e", trim="c0392b", accent="7a1010",
                  roof="1a1414", glow="ff5a2a", glow_core="ffd08a"),
    "angel": dict(stone="eeeae0", stone_dark="c8c2b0", wood="e8dcc0", wood_dark="b8a880", trim="d4a017", accent="f5f0e0",
                  roof="d8d0bc", glow="fff2a0", glow_core="ffffff"),
    "hive": dict(stone="1f3f44", stone_dark="102528", wood="2a4a4f", wood_dark="16292c", trim="3fd8d0", accent="0e3a38",
                 roof="16292c", glow="3fe8e0", glow_core="d0fffc"),
}
DEFAULT_RACE = "human"


def bind(ns):
    globals().update(ns)


def arrow_tower():
    """A timber watchtower: four corner posts, a plank floor, a crenellated parapet with an arrow slit, a pennant."""
    posts = [box((x, 0, z), (2, 24, 2), "tw_wood") for x in (-7, 5) for z in (-7, 5)]
    braces = [box((-5, 10, -7), (10, 2, 1), "tw_wood_dark"), box((-5, 10, 6), (10, 2, 1), "tw_wood_dark"),
              box((-7, 10, -5), (1, 2, 10), "tw_wood_dark"), box((6, 10, -5), (1, 2, 10), "tw_wood_dark")]
    floor = [box((-8, -2, -8), (16, 2, 16), "tw_planks")]
    parapet = [box((-8, -7, -8), (16, 5, 1), "tw_slit"), box((-8, -7, 7), (16, 5, 1), "tw_stone"),
               box((-8, -7, -7), (1, 5, 14), "tw_stone"), box((7, -7, -7), (1, 5, 14), "tw_stone")]
    merlons = [box((x, -10, z), (2, 3, 1), "tw_stone") for x in (-8, -3, 1, 6) for z in (-8, 7)] + \
              [box((x, -10, z), (1, 3, 2), "tw_stone") for x in (-8, 7) for z in (-4, 2)]
    pole = part("pennant", pivot=(6, -10, 6), boxes=[box((0, -8, 0), (1, 8, 1), "tw_wood_dark"),
                                                     box((1, -8, 0), (5, 3, 0), "tw_cloth")])
    body = part("body", boxes=posts + braces + floor + parapet + merlons, children=[pole])
    return {"id": "tower_arrow", "tex": [128, 128], "parts": [body]}


def arcane_spire():
    """A tapering stone needle with gold bands and a mana crystal floating above its point."""
    needle = [box((-5, 14, -5), (10, 10, 10), "tw_stone"), box((-4, 4, -4), (8, 10, 8), "tw_stone"),
              box((-3, -4, -3), (6, 8, 6), "tw_stone"), box((-1.5, -9, -1.5), (3, 5, 3), "tw_stone_dark"),
              box((-5.5, 13, -5.5), (11, 1, 11), "tw_trim"), box((-4.5, 3, -4.5), (9, 1, 9), "tw_trim"),
              box((-3.5, -5, -3.5), (7, 1, 7), "tw_trim")]
    runes = [box((-5.2, 16, -5.2), (0.2, 6, 10.4), "tw_rune"), box((5, 16, -5.2), (0.2, 6, 10.4), "tw_rune")]
    crystal = part("crystal", pivot=(0, -15, 0), rot=(0, 0.785, 0), children=[
        part("crystal_core", pivot=(0, 0, 0), rot=(0.615, 0, 0.615), boxes=[box((-2, -2, -2), (4, 4, 4), "tw_crystal")])])
    body = part("body", boxes=needle + runes, children=[crystal])
    return {"id": "tower_arcane", "tex": [128, 128], "parts": [body]}


def healing_shrine():
    """A low altar: a stepped basin of glowing water under four short pillars and a ring, a soft light above."""
    steps = [box((-8, 20, -8), (16, 4, 16), "tw_stone_dark"), box((-6, 17, -6), (12, 3, 12), "tw_stone")]
    basin = [box((-5, 16, -5), (10, 1, 10), "tw_water"),
             box((-6, 14, -6), (12, 3, 1), "tw_trim"), box((-6, 14, 5), (12, 3, 1), "tw_trim"),
             box((-6, 14, -5), (1, 3, 10), "tw_trim"), box((5, 14, -5), (1, 3, 10), "tw_trim")]
    pillars = [box((x, 4, z), (2, 13, 2), "tw_stone") for x in (-8, 6) for z in (-8, 6)]
    ring = [box((-8, 2, -8), (16, 2, 2), "tw_stone_dark"), box((-8, 2, 6), (16, 2, 2), "tw_stone_dark"),
            box((-8, 2, -6), (2, 2, 12), "tw_stone_dark"), box((6, 2, -6), (2, 2, 12), "tw_stone_dark")]
    light = part("light", pivot=(0, 8, 0), boxes=[box((-1.5, -1.5, -1.5), (3, 3, 3), "tw_light")])
    body = part("body", boxes=steps + basin + pillars + ring, children=[light])
    return {"id": "tower_healing", "tex": [128, 128], "parts": [body]}


def models():
    return [arrow_tower(), arcane_spire(), healing_shrine()]


# ----------------------------------------------------------------------------- race towers (one race each)

def rt_ballista():
    stand = [box((-2, 16, -2), (4, 8, 4), "tw_wood_dark"), box((-6, 22, -6), (12, 2, 12), "tw_stone")]
    turret = part("turret", pivot=(0, 15, 0), boxes=[
        box((-1.5, -2, -8), (3, 2, 16), "tw_wood"),                   # stock
        box((-11, -3, -8), (22, 2, 2), "tw_wood_dark"),               # bow arms
        box((-11, -2.5, -6.5), (22, 0.5, 0.5), "tw_trim"),            # string
        box((-0.5, -3, -10), (1, 1, 14), "tw_metal"),                 # the bolt
        box((-2, -1, 6), (4, 3, 3), "tw_trim")])                      # winch
    return {"id": "rt_ballista", "race": "human", "tex": [128, 64], "parts": [part("body", boxes=stand, children=[turret])]}


def rt_thornwood_sentinel():
    trunk = [box((-3, 0, -3), (6, 24, 6), "tw_wood"), box((-5, 20, -5), (10, 4, 10), "tw_wood_dark")]
    thorns = [box((x, y, z), (w, 1, d), "tw_wood_dark") for (x, y, z, w, d) in
              ((-8, 14, -0.5, 5, 1), (3, 10, -0.5, 6, 1), (-0.5, 6, -8, 1, 5), (-0.5, 12, 3, 1, 6))]
    canopy = [box((-7, -6, -7), (14, 6, 14), "tw_cloth"), box((-4, -9, -4), (8, 3, 8), "tw_cloth")]
    eyes = [box((-2, 8, -3.2), (1, 1, 0.2), "tw_rune2"), box((1, 8, -3.2), (1, 1, 0.2), "tw_rune2")]
    return {"id": "rt_thornwood_sentinel", "race": "elf", "tex": [128, 64], "parts": [part("body", boxes=trunk + thorns + canopy + eyes)]}


def rt_rune_cannon():
    stand = [box((-5, 18, -5), (10, 6, 10), "tw_stone_dark"), box((-6, 17, -6), (12, 1, 12), "tw_trim")]
    barrel = part("barrel", pivot=(0, 14, 0), rot=(-0.35, 0, 0), boxes=[
        box((-3, -3, -10), (6, 6, 16), "tw_metal"), box((-3.5, -3.5, -9), (7, 7, 1), "tw_trim"),
        box((-3.5, -3.5, 2), (7, 7, 1), "tw_trim"), box((-3.2, -1, -4), (0.2, 2, 4), "tw_rune2"),
        box((3, -1, -4), (0.2, 2, 4), "tw_rune2")])
    return {"id": "rt_rune_cannon", "race": "dwarf", "tex": [128, 64], "parts": [part("body", boxes=stand, children=[barrel])]}


def rt_war_drum_totem():
    drum = [box((-6, 16, -6), (12, 8, 12), "tw_wood"), box((-6.5, 15, -6.5), (13, 1, 13), "tw_cloth"),
            box((-6.5, 19, -6.5), (13, 1, 13), "tw_trim")]
    pole = [box((-2, -6, 4), (4, 22, 4), "tw_wood_dark"), box((-3, -2, 3.5), (6, 4, 1), "tw_cloth"),
            box((-6, -8, 5), (3, 2, 2), "tw_trim"), box((3, -8, 5), (3, 2, 2), "tw_trim"),
            box((-1.5, 2, 3.8), (1, 1, 0.2), "tw_rune2"), box((0.5, 2, 3.8), (1, 1, 0.2), "tw_rune2")]
    return {"id": "rt_war_drum_totem", "race": "orc", "tex": [128, 64], "parts": [part("body", boxes=drum + pole)]}


def rt_soul_pyre():
    legs = [box((x, 16, z), (2, 8, 2), "tw_metal") for x in (-6, 4) for z in (-6, 4)]
    bowl = [box((-7, 12, -7), (14, 4, 14), "tw_stone_dark"), box((-7.5, 11, -7.5), (15, 1, 15), "tw_trim")]
    flame = part("flame", pivot=(0, 12, 0), boxes=[box((-5, -8, -5), (10, 8, 10), "tw_flame"),
                                                   box((-3, -13, -3), (6, 5, 6), "tw_flame")])
    return {"id": "rt_soul_pyre", "race": "demon", "tex": [128, 64], "parts": [part("body", boxes=legs + bowl, children=[flame])]}


def rt_sun_lance():
    obelisk = [box((-4, 4, -4), (8, 20, 8), "tw_stone"), box((-3, -8, -3), (6, 12, 6), "tw_stone"),
               box((-4.5, 3, -4.5), (9, 1, 9), "tw_trim"), box((-3.5, -9, -3.5), (7, 1, 7), "tw_trim")]
    lens = part("lens", pivot=(0, -12, 0), rot=(0, 0.785, 0), boxes=[box((-2, -2, -2), (4, 4, 4), "tw_crystal")])
    return {"id": "rt_sun_lance", "race": "angel", "tex": [128, 64], "parts": [part("body", boxes=obelisk, children=[lens])]}


def rt_lurker_pit():
    mound = [box((-7, 22, -7), (14, 2, 14), "tw_stone_dark")]
    maw = part("maw", pivot=(0, 22, 0), boxes=[box((x, -6, z), (2, 6, 2), "tw_trim") for x in (-6, -1, 4) for z in (-6, 4)]
               + [box((-4, -3, -4), (8, 3, 8), "tw_flame")])
    return {"id": "rt_lurker_pit", "race": "hive", "tex": [128, 64], "parts": [part("body", boxes=mound, children=[maw])]}


def race_models():
    return [rt_ballista(), rt_thornwood_sentinel(), rt_rune_cannon(), rt_war_drum_totem(), rt_soul_pyre(), rt_sun_lance(),
            rt_lurker_pit()]


def tw_material(mat, side, x, y, w, h, pal, rng):
    if not mat.startswith("tw_"):
        return NotImplemented
    p = pal
    if mat == "tw_stone":
        c = hexc(p["stone"])
        if (y % 4 == 3) or ((x + (y // 4) * 3) % 6 == 5):
            c = hexc(p["stone_dark"])                                           # mortar lines
        return shade(c, rng.uniform(0.9, 1.06))
    if mat == "tw_slit":
        if side == "front" and w // 2 - 1 <= x <= w // 2 and 1 <= y <= h - 2:
            return hexc("141414")                                               # the arrow slit
        return tw_material("tw_stone", side, x, y, w, h, pal, rng)
    if mat == "tw_stone_dark":
        return shade(hexc(p["stone_dark"]), rng.uniform(0.9, 1.08))
    if mat == "tw_wood":
        c = mix(hexc(p["wood"]), hexc(p["wood_dark"]), rng.uniform(0, 0.35))
        return shade(c, 1.1) if side == "top" else c
    if mat == "tw_wood_dark":
        return shade(hexc(p["wood_dark"]), rng.uniform(0.9, 1.1))
    if mat == "tw_planks":
        c = hexc(p["wood"]) if (x // 4) % 2 == 0 else hexc(p["wood_dark"])
        return shade(c, rng.uniform(0.92, 1.05))
    if mat == "tw_trim":
        return shade(hexc(p["trim"]), rng.uniform(0.9, 1.1))
    if mat == "tw_cloth":
        return shade(hexc(p["accent"]), rng.uniform(0.9, 1.05))
    if mat == "tw_rune":
        return glow(hexc(p["glow"])) if (y % 3 == 1 and x % 4 in (1, 2)) or (x % 4 == 1 and y % 6 < 2) else None
    if mat == "tw_crystal":
        return glow(hexc(p["glow_core"] if (x + y) % 3 == 0 else p["glow"]))
    if mat == "tw_water":
        return glow(mix(hexc(p["glow"]), hexc(p["glow_core"]), rng.uniform(0, 0.4))) if side == "top" else hexc(p["stone_dark"])
    if mat == "tw_metal":
        c = mix(hexc(p["stone_dark"]), hexc("2a2a2a"), 0.4)
        return shade(c, 1.25) if side == "top" else shade(c, rng.uniform(0.85, 1.1))
    if mat == "tw_rune2":
        return glow(hexc(p["glow"]))
    if mat == "tw_flame":
        t = y / max(1, h - 1)                                                   # 0 at the top, 1 at the base
        if rng.random() < (0.55 if side == "top" else 0.5 * (1 - t)):
            return None                                                         # ragged tongues of flame
        return glow(mix(hexc(p["glow"]), hexc(p["glow_core"]), t * 0.7))
    if mat == "tw_light":
        return glow(hexc(p["glow_core"]))
    return hexc("ff00ff")
