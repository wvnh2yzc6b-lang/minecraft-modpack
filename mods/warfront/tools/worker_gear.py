"""Gear for the posted roles (farmer, builder, guard): one model per role and humanoid race, worn
over the soldier's body. Imported by units.py.

Humanoid gear models use the vanilla player rig (head, body, arms, legs) so the game can copy the
soldier's pose straight onto them. Workers are kept plain and humanoid: simple hats, smocks, aprons
and helms, told apart by the race's colors rather than by horns or crests. NPC factions reuse their
race's geometry with their own colors.
"""
# part, box, overlay, attach and the pixel helpers are injected by units.py (see bind()).

# Factions borrow their race's geometry.
RACE_OF = {
    "human": "human", "elf": "elf", "dwarf": "dwarf", "orc": "orc", "angel": "angel", "hive": "hive",
    "marauders": "orc", "black_legion": "human", "silverwood_reavers": "elf", "ironbeard_clan": "dwarf",
    "fallen_host": "angel", "the_swarm": "hive", "demon": "demon", "burning_horde": "demon",
}
HUMANOID_RACES = ["human", "elf", "dwarf", "orc", "angel", "hive", "demon"]
ROLES = ["farmer", "builder", "guard"]
# Orc farmers and builders are goblins: smaller, with long ears and a hooked nose, painted in orc colors.
GOBLIN_ROLES = ["farmer", "builder"]
GOBLIN_SKIN = "9aae44"


def _base(lead):
    """Common palette keys so every material has what it needs."""
    p = dict(leather="7a5230", leather_dark="4a3018", iron="9aa0a8", iron_dark="565c64", rust="8a4a22",
             cloth="7a6a54", cloth_dark="3a3028", bone="d8cdb0", bone_dark="8c7c5c", accent="2f5fa8",
             accent_dark="1c3a6a", trim="c9a44a", straw="d8b45a", straw_dark="9a7a32", wood="9a6a3a",
             wood_dark="5a3c20", apron="b8a888", leaf="5a8a3a", leaf_dark="2e5020", linen="e8e0cc",
             chitin="5a3a6a", chitin_dark="2a1a34", glow_c="ffb040")
    p.update(lead)
    return p


GEAR_PALETTES = {
    "human": _base({}),
    "elf": _base(dict(leather="5f6a3a", leather_dark="333d1c", iron="c8d2d6", iron_dark="7a8a90", cloth="4f7a4a",
                      cloth_dark="263d24", accent="3f8f3f", accent_dark="1f4f22", trim="d8d0a0", wood="b08c58",
                      wood_dark="6a5030", apron="9ab070", glow_c="c8ff9a")),
    "dwarf": _base(dict(leather="6a4226", leather_dark="3a2414", iron="7c7f84", iron_dark="45474c", cloth="8a3a2a",
                        cloth_dark="4a1a12", accent="a83a2a", accent_dark="5a1a12", trim="d4a017", apron="8a6a4a",
                        glow_c="ffd060")),
    "orc": _base(dict(leather="4a3a22", leather_dark="241a0e", iron="6a6460", iron_dark="363230", rust="9a4a1a",
                      cloth="6a5a3a", cloth_dark="2e2618", accent="8a2a1a", accent_dark="4a120a", trim="d8cdb0",
                      apron="6a5a40", glow_c="ff8a3a")),
    "angel": _base(dict(leather="d8c8a8", leather_dark="9a8a6a", iron="e6dcb8", iron_dark="a89a6a", rust="c8a44a",
                        cloth="f0ece0", cloth_dark="b8b0a0", accent="d4a017", accent_dark="8a6a10", trim="fff0a8",
                        apron="e8e0cc", glow_c="fff4c0")),
    "hive": _base(dict(leather="1a3a40", leather_dark="0a1c20", iron="2e5a62", iron_dark="14303a", rust="29dfeb",
                       cloth="143038", cloth_dark="081a1f", accent="29dfeb", accent_dark="0e6a74", trim="49ffc8",
                       apron="1c3c44", straw="4a6a5a", straw_dark="22382e", glow_c="29dfeb")),
    "marauders": _base(dict(leather="5a3a1e", leather_dark="2a1a0c", iron="6a6460", iron_dark="363230", rust="9a4a1a",
                            cloth="7a4a2a", cloth_dark="3a2212", accent="9a5a1a", accent_dark="4a2a0a", trim="d8cdb0",
                            apron="7a5a3a", glow_c="ff8a3a")),
    "black_legion": _base(dict(leather="2e2a2a", leather_dark="141212", iron="4a4c54", iron_dark="1e2026", cloth="2a2a30",
                               cloth_dark="121216", accent="1a1a1e", accent_dark="0a0a0c", trim="a83232", apron="3a3a40",
                               glow_c="ff4a3a")),
    "silverwood_reavers": _base(dict(leather="3a4a5a", leather_dark="1a2430", iron="b8c4d0", iron_dark="6a7a8a",
                                     cloth="3a4a5a", cloth_dark="1a222c", accent="8a9aa8", accent_dark="4a5a6a",
                                     trim="dde6ee", apron="5a6a7a", glow_c="b8e0ff")),
    "ironbeard_clan": _base(dict(leather="5a3a22", leather_dark="2e1c10", iron="9aa0a8", iron_dark="565c64",
                                 cloth="3a5a8a", cloth_dark="1a2a4a", accent="3a5a8a", accent_dark="1a2a4a",
                                 trim="d4a017", apron="6a5a4a", glow_c="ffd060")),
    "fallen_host": _base(dict(leather="3a3448", leather_dark="1a1624", iron="4a4458", iron_dark="221e2c", rust="6a5a8a",
                              cloth="2a2632", cloth_dark="121018", accent="4a3a6a", accent_dark="221a34", trim="9a8ac8",
                              apron="3a3448", glow_c="b0a0ff")),
    "the_swarm": _base(dict(leather="1c2622", leather_dark="0b1210", iron="34403a", iron_dark="18201c", rust="8aff3a",
                            cloth="141d1a", cloth_dark="070b0a", accent="8aff3a", accent_dark="3e7a14", trim="b8ff4a",
                            apron="1c2622", straw="4a5a3a", straw_dark="22301a", glow_c="b8ff4a")),
}

# Extra keys the imp palettes need for the gear materials they share with worker gear.
IMP_WORKER_KEYS = {
    "demon": dict(accent="7a1a14", accent_dark="3e0a08", trim="c9772a", straw="b8944a", straw_dark="7a5a2a",
                  wood="7a5030", wood_dark="4a2c18", apron="6a4a3a", leaf="5a6a2a", leaf_dark="2e3a14", linen="c8b8a0",
                  chitin="3a2020", chitin_dark="1a0c0c", glow_c="ff9a2e"),
    "burning_horde": dict(accent="3a0a08", accent_dark="1a0504", trim="ff6a1a", straw="8a6a3a", straw_dark="4a3418",
                          wood="4a3020", wood_dark="24160c", apron="3a2a20", leaf="4a4a1a", leaf_dark="24240a",
                          linen="8a7a68", chitin="2a1010", chitin_dark="140606", glow_c="ff6a1a"),
}

GEAR_PALETTES["demon"] = _base(dict(leather="5a2a1e", leather_dark="2a120c", iron="4a4448", iron_dark="242026",
                                    rust="a04a18", cloth="4a1c18", cloth_dark="22100c", bone="cbb89a",
                                    bone_dark="7a6450", **IMP_WORKER_KEYS["demon"]))
GEAR_PALETTES["burning_horde"] = _base(dict(leather="3e2616", leather_dark="1e120a", iron="3a3438", iron_dark="1a1618",
                                            rust="c05a18", cloth="2e1210", cloth_dark="140806", bone="a8987a",
                                            bone_dark="5a4a38", **IMP_WORKER_KEYS["burning_horde"]))


# ----------------------------------------------------------------------------- shared pieces

def rig(model_id):
    """Empty vanilla player rig to hang gear on; pivots match PlayerModel."""
    return {"id": model_id, "tex": [64, 64], "parts": [
        part("head"), part("hat"), part("body"), part("right_arm", pivot=(-5, 2, 0)),
        part("left_arm", pivot=(5, 2, 0)), part("right_leg", pivot=(-1.9, 12, 0)), part("left_leg", pivot=(1.9, 12, 0)),
    ]}


def both_arms(m, make):
    attach(m, "right_arm", *make("r", -1))
    attach(m, "left_arm", *make("l", 1))


def both_legs(m, make):
    attach(m, "right_leg", *make("r", -1))
    attach(m, "left_leg", *make("l", 1))


def boots(m, mat="leather"):
    both_legs(m, lambda s, sx: [overlay(f"boot_{s}", (-2, 9, -2), (4, 3, 4), mat, 0.3)])


# ----------------------------------------------------------------------------- farmer

def farmer_hat(race):
    """Plain work hats: a straw hat for most, a knit cap for dwarves, a cloth hood for elves and hive."""
    if race == "dwarf":
        return [overlay("cap_knit", (-4, -9, -4), (8, 3, 8), "wool", 0.4),
                overlay("cap_fold", (-4, -6.6, -4), (8, 1.5, 8), "wool", 0.65)]
    if race in ("elf", "hive"):
        return [overlay("hood", (-4, -8, -4), (8, 6, 8), "smock", 0.5)]
    if race in ("orc", "goblin"):
        return [overlay("bandana", (-4, -8, -4), (8, 3, 8), "accent_cloth", 0.35)]
    return [part("hat_brim", pivot=(0, -6.3, 0), rot=(0.06, 0, 0), boxes=[box((-6, 0, -6), (12, 0.8, 12), "straw")]),
            overlay("hat_crown", (-4, -10, -4), (8, 4, 8), "straw", 0.3),
            overlay("hat_band", (-4, -7.4, -4), (8, 1, 8), "accent_cloth", 0.5)]


def goblin_face():
    """Long ears swept out and back, and a hooked nose. Hung on the gear so the body stays the plain rig."""
    ear_r = part("goblin_ear_r", pivot=(-3.8, -4.5, 0.5), rot=(0.1, 0.45, -0.3),
                 boxes=[box((-4, -1.5, -0.5), (4, 3, 1), "goblin_skin")],
                 children=[part("goblin_ear_r_tip", pivot=(-4, 0, 0), rot=(0, 0, -0.2),
                                boxes=[box((-3, -1, -0.5), (3, 1.5, 1), "goblin_skin")])])
    nose = part("goblin_nose", pivot=(0, -3.2, -4), rot=(0.35, 0, 0),
                boxes=[box((-1, -1, -2.5), (2, 2, 2.5), "goblin_skin")],
                children=[part("goblin_nose_tip", pivot=(0, 0.6, -2.5), rot=(0.6, 0, 0),
                               boxes=[box((-0.5, 0, -1.2), (1, 1.4, 1.2), "goblin_skin")])])
    return [ear_r, mirror(ear_r), nose]


def gear_farmer(race):
    m = rig(f"gear_farmer_{race}")
    attach(m, "head", *farmer_hat(race))
    if race == "goblin":
        attach(m, "head", *goblin_face())
    attach(m, "body",
           overlay("smock", (-4, 0, -2), (8, 12, 4), "smock", 0.3),
           part("apron", pivot=(0, 4, -2.55), rot=(-0.04, 0, 0), boxes=[box((-3, 0, 0), (6, 8, 0), "apron")]),
           overlay("belt", (-4, 9, -2), (8, 1, 4), "leather_belt", 0.5),
           part("seed_pouch", pivot=(3.4, 9.6, -1), rot=(0, 0, -0.12), boxes=[box((-1, 0, -1), (2, 3, 2), "pouch")]),
           part("basket", pivot=(0, 2, 2.4), rot=(0.08, 0, 0), boxes=[box((-3, 0, 0), (6, 6, 3.5), "wicker")]),
           part("basket_strap_r", pivot=(-2.5, 0, -2.4), boxes=[box((-0.5, 0, 0), (1, 3, 0), "leather")]),
           part("basket_strap_l", pivot=(2.5, 0, -2.4), boxes=[box((-0.5, 0, 0), (1, 3, 0), "leather")]))
    both_arms(m, lambda s, sx: [overlay(f"sleeve_{s}", (-3 if s == "r" else -1, -2, -2), (4, 4, 4), "smock", 0.35)])
    boots(m)
    return m


# ----------------------------------------------------------------------------- builder

def builder_hat(race):
    """A plain leather work cap with a short visor; dwarves wear an iron mining helm instead."""
    if race == "dwarf":
        return [overlay("mining_helm", (-4, -8.8, -4), (8, 3.5, 8), "plate", 0.55),
                part("helm_rim", pivot=(0, -5.6, 0), boxes=[box((-5, 0, -5), (10, 0.6, 10), "plate")])]
    return [overlay("cap", (-4, -8.5, -4), (8, 3, 8), "leather", 0.4),
            part("cap_visor", pivot=(0, -6.1, -4.3), rot=(0.25, 0, 0), boxes=[box((-4, 0, -2.5), (8, 0.6, 2.5), "leather")])]


def gear_builder(race):
    m = rig(f"gear_builder_{race}")
    attach(m, "head", *builder_hat(race))
    if race == "goblin":
        attach(m, "head", *goblin_face())
    attach(m, "body",
           overlay("work_shirt", (-4, 0, -2), (8, 12, 4), "smock", 0.25),
           part("heavy_apron", pivot=(0, 1, -2.5), boxes=[box((-3.5, 0, 0), (7, 10, 0), "heavy_apron")]),
           overlay("tool_belt", (-4, 9, -2), (8, 1.5, 4), "tool_belt", 0.55),
           part("chisel", pivot=(-3.6, 10.3, -1.2), rot=(0.1, 0, 0.15), boxes=[box((-0.5, 0, -0.5), (1, 3, 1), "plate")]),
           part("trowel", pivot=(3.7, 10.3, 0.4), rot=(0, 0, -0.2), boxes=[box((-0.5, 0, -1), (1, 1.5, 2), "wood"),
                                                                        box((-0.5, 1.5, -1.5), (1, 2, 3), "plate")]),
           part("plank_bundle", pivot=(0, 0.5, 2.3), rot=(0, 0, 0.22), boxes=[
               box((-3.5, -2, 0), (2, 11, 1), "planks"), box((-1, -3, 0), (2, 12, 1), "planks"),
               box((1.5, -1.5, 0), (2, 10, 1), "planks")],
               children=[part("bundle_strap", pivot=(0, 4, 0), boxes=[box((-4, 0, -0.1), (8, 1, 1.2), "leather")])]))
    both_arms(m, lambda s, sx: [overlay(f"glove_{s}", (-3 if s == "r" else -1, 6, -2), (4, 4, 4), "leather", 0.35)])
    both_legs(m, lambda s, sx: [overlay(f"knee_pad_{s}", (-2, 4, -2), (4, 2, 4), "leather", 0.35)])
    boots(m)
    return m


# ----------------------------------------------------------------------------- guard

def guard_helm(race):
    """A plain open-faced kettle helm, the same shape for every race; only its colors differ."""
    return [overlay("kettle_helm", (-4, -8.5, -4), (8, 4, 8), "plate", 0.55),
            part("kettle_brim", pivot=(0, -5, 0), rot=(0.05, 0, 0), boxes=[box((-5.5, 0, -5.5), (11, 0.7, 11), "plate")])]


def gear_guard(race):
    m = rig(f"gear_guard_{race}")
    attach(m, "head", *guard_helm(race))
    plate = "plate"
    body = [overlay("mail", (-4, 0, -2), (8, 12, 4), "mail", 0.35),
            overlay("guard_belt", (-4, 9, -2), (8, 1, 4), "leather_belt", 0.6),
            part("lantern", pivot=(4.7, 9.5, -0.5), boxes=[box((-1, 0, -1), (2, 3, 2), "lantern")],
                 children=[part("lantern_hook", pivot=(0, 0, 0), boxes=[box((-0.5, -1, -0.5), (1, 1, 1), "plate")])])]
    body += [part("tabard_front", pivot=(0, 1, -2.7), boxes=[box((-3, 0, 0), (6, 11, 0), "tabard")]),
             part("tabard_back", pivot=(0, 1, 2.7), boxes=[box((-3, 0, 0), (6, 11, 0), "tabard")])]
    attach(m, "body", *body)
    both_arms(m, lambda s, sx: [
        overlay(f"vambrace_{s}", (-3 if s == "r" else -1, 5, -2), (4, 5, 4), plate, 0.35)])
    both_legs(m, lambda s, sx: [overlay(f"greave_{s}", (-2, 5, -2), (4, 7, 4), plate, 0.35)])
    return m


def all_gear():
    makers = {"farmer": gear_farmer, "builder": gear_builder, "guard": gear_guard}
    return ([makers[role](race) for role in ROLES for race in HUMANOID_RACES]
            + [makers[role]("goblin") for role in GOBLIN_ROLES])


def painted_keys(race):
    """The skin keys whose texture folders get this gear geometry painted in their colors."""
    return ["orc"] if race == "goblin" else [k for k, g in RACE_OF.items() if g == race]


# ----------------------------------------------------------------------------- materials

def bind(ns):
    """Receives the model and painting helpers from units.py."""
    globals().update(ns)


def worker_material(mat, side, x, y, w, h, pal, rng):
    """Materials for worker and guard gear. Returns NotImplemented for anything else."""
    top = side == "top"
    if mat == "straw":
        c = mix(hexc(pal["straw"]), hexc(pal["straw_dark"]), 0.15 + 0.35 * (((x + y * 2) % 4) / 3))   # diagonal plait
        if rng.random() < 0.1:
            c = shade(c, 1.15)                                            # sun-bleached strands
        if edge(x, y, w, h) and not top:
            c = shade(c, 0.82)
        return shade(c, 1.1) if top else c
    if mat == "wicker":
        band = (y // 2) % 2
        c = hexc(pal["straw"] if band else pal["straw_dark"])
        if x % 3 == 0:
            c = mix(c, hexc(pal["wood_dark"]), 0.45)                     # upright stakes
        if y == 0 and side in ("front", "back", "left", "right"):
            c = shade(hexc(pal["straw"]), 1.12)                           # rolled rim
        return shade(c, rng.uniform(0.92, 1.06))
    if mat in ("smock", "linen"):
        return cloth_px(pal, rng, x, y, w, h, side, base="linen" if mat == "linen" else "cloth")
    if mat == "accent_cloth":
        c = cloth_px(pal, rng, x, y, w, h, side, base="accent")
        return c
    if mat == "apron":
        c = cloth_px(pal, rng, x, y, w, h, side, base="apron", fray=1)
        if c is not None and side == "front" and y == h // 2 and 1 <= x <= w - 2:
            c = shade(c, 0.8)                                             # pocket seam
        return c
    if mat == "wool":
        c = hexc(pal["accent"])
        c = shade(c, 1.1 if x % 2 == 0 else 0.88)                         # knit ribs
        if y % 2 == 1:
            c = shade(c, 0.95)
        return shade(c, 1.1) if top else c
    if mat == "pouch":
        c = leather_px(pal, rng, x, y, w, h, side)
        if y == 0 and side != "top":
            c = shade(c, 0.75)                                            # flap
        return c
    if mat == "heavy_apron":
        c = leather_px(pal, rng, x, y, w, h, side, stitch_row=h - 1)
        if side == "front" and (y in (5, 6)) and 1 <= x <= w - 2:
            c = shade(c, 0.82 if y == 5 else 1.05)                        # tool pocket
        if side == "front" and x == w // 2 and y >= 5:
            c = shade(c, 0.85)
        return c
    if mat == "tool_belt":
        c = leather_px(pal, rng, x, y, w, h, side)
        if side in ("front", "back", "left", "right") and x % 3 == 0:
            c = shade(c, 0.7)                                             # tool loops
        if side == "front" and x == w // 2:
            c = hexc("c4c8ce")                                            # buckle
        return c
    if mat == "planks":
        c = mix(hexc(pal["wood"]), hexc(pal["wood_dark"]), 0.15 + 0.25 * ((y * 3 + x) % 5 == 0))
        if side in ("front", "back", "left", "right") and x % 2 == 0:
            c = shade(c, 0.92)                                            # grain
        if rng.random() < 0.04:
            c = mix(c, hexc(pal["wood_dark"]), 0.7)                       # knot
        if edge(x, y, w, h):
            c = shade(c, 0.8)
        return shade(c, 1.15) if top else c
    if mat == "wood":
        c = mix(hexc(pal["wood"]), hexc(pal["wood_dark"]), rng.uniform(0.1, 0.4))
        return shade(c, 0.9) if x % 2 else c
    if mat == "plate":
        c = iron_px(pal, rng, x, y, w, h, side, rivets=False)
        if side in ("front", "back", "left", "right") and y == 1:
            c = mix(c, hexc("eef2f6"), 0.4)                               # polished highlight
        return c
    if mat == "mail":
        ring = (x + (y % 2)) % 2 == 0
        c = mix(hexc(pal["iron"]), hexc(pal["iron_dark"]), 0.25 if ring else 0.75)
        if rng.random() < 0.05:
            c = mix(c, hexc(pal["rust"]), 0.5)
        return shade(c, 1.1) if top else c
    if mat == "tabard":
        c = cloth_px(pal, rng, x, y, w, h, side, base="accent", fray=1)
        if c is None:
            return None
        if x in (0, w - 1) or y == 0:
            return mix(hexc(pal["trim"]), c, 0.2)                         # trimmed border
        if side in ("front", "back") and x in (w // 2 - 1, w // 2) and 2 <= y <= 7:
            return mix(hexc(pal["trim"]), c, 0.3)                         # faction stripe
        return c
    if mat == "goblin_skin":
        c = shade(hexc(GOBLIN_SKIN), rng.uniform(0.9, 1.06))
        if side in ("bottom", "back"):
            c = shade(c, 0.8)
        if edge(x, y, w, h) and side != "top":
            c = shade(c, 0.88)
        return shade(c, 1.1) if top else c
    if mat == "lantern":
        if edge(x, y, w, h) or side in ("top", "bottom"):
            return shade(hexc(pal["iron_dark"]), 0.9)
        return glow(hexc(pal["glow_c"]))
    return NotImplemented
