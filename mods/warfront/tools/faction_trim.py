"""Enemy faction trim: a cosmetic overlay worn by raiders of each NPC faction, over the race body and any armor, so
you can tell who is attacking at a glance. Imported by units.py. Player-owned units never wear it.

  Marauder Horde (orc): war-paint stripes, bone trophies on the belt and shoulders.
  Black Legion (human): a black tabard with a white skull, a spiked pauldron.
  Burning Horde (demon): smoldering chains across the chest, a glowing brand.
  The Swarm (Hive): glowing acid sacs on the back, jagged black chitin spurs.
  Silverwood Reavers (elf): thorn-vine wraps, a pale antler-bone mask.
  Ironbeard Clan (dwarf): a rune-etched iron face guard, braided beard rings.
  Fallen Host (angel): torn ash-gray wings, a cracked and dimmed halo.
"""
# part, box, overlay, attach, mirror and the pixel helpers are injected by units.py (see bind()).

FACTIONS = ["marauders", "black_legion", "burning_horde", "the_swarm", "silverwood_reavers", "ironbeard_clan",
            "fallen_host"]

PAL = dict(paint="c8281e", paint_dark="7a120c", bone="ddd2b4", bone_dark="8c7c5c", tabard="141416", tabard_dark="050506",
           skull="ece8dc", iron="5a5c62", iron_dark="2a2c30", spike="8a8c92", chain="3a3438", chain_hi="6a6068",
           ember="ff5a10", soot="1a1210", acid="8aff3a", acid_dark="2e7a10", chitin="0e1210", chitin_hi="2a3430",
           vine="2e4a22", vine_dark="16260e", thorn="c8d8b8", rune="5ab0ff", ring="d4a017", ring_dark="7a5a0a",
           ash="6e6a6a", ash_dark="3a3636", halo="b8a870", halo_dark="5a5238")


def bind(ns):
    globals().update(ns)


def rig(model_id):
    return {"id": model_id, "tex": [64, 64], "parts": [
        part("head"), part("hat"), part("body"), part("right_arm", pivot=(-5, 2, 0)),
        part("left_arm", pivot=(5, 2, 0)), part("right_leg", pivot=(-1.9, 12, 0)), part("left_leg", pivot=(1.9, 12, 0)),
    ]}


def marauders():
    m = rig("trim_marauders")
    attach(m, "head", overlay("war_paint", (-4, -8, -4), (8, 8, 8), "tr_paint", 0.05))
    attach(m, "body", overlay("trophy_belt", (-4, 9, -2), (8, 1, 4), "tr_belt", 0.75),
           part("trophy_skull", pivot=(-2.5, 10, -2.9), boxes=[box((-1, 0, -1), (2, 2, 1.5), "tr_bone")]),
           part("trophy_tooth_a", pivot=(1.5, 10.5, -2.8), rot=(0, 0, 0.3), boxes=[box((-0.4, 0, -0.4), (0.8, 2.2, 0.8), "tr_bone")]),
           part("trophy_tooth_b", pivot=(3, 10.5, -2.6), rot=(0, 0, -0.2), boxes=[box((-0.4, 0, -0.4), (0.8, 1.8, 0.8), "tr_bone")]))
    for s, arm, x in (("r", "right_arm", -2), ("l", "left_arm", 2)):
        attach(m, arm, part(f"shoulder_bone_{s}", pivot=(x, -2.6, 0), rot=(0, 0, 0.5 if s == "r" else -0.5),
                            boxes=[box((-0.6, -3, -0.6), (1.2, 3, 1.2), "tr_bone")]))
    return m


def black_legion():
    m = rig("trim_black_legion")
    attach(m, "body", part("tabard_front", pivot=(0, 0.5, -2.8), boxes=[box((-3.5, 0, 0), (7, 12, 0), "tr_tabard_skull")]),
           part("tabard_back", pivot=(0, 0.5, 2.8), boxes=[box((-3.5, 0, 0), (7, 12, 0), "tr_tabard")]))
    attach(m, "right_arm", overlay("pauldron", (-3, -2, -2), (4, 3, 4), "tr_iron", 1.0),
           part("pauldron_spike", pivot=(-3.2, -3.4, 0), rot=(0, 0, 0.55), boxes=[box((-0.6, -3.2, -0.6), (1.2, 3.2, 1.2), "tr_spike")]),
           part("pauldron_spike2", pivot=(-2.2, -3.6, 1.4), rot=(0.3, 0, 0.3), boxes=[box((-0.5, -2.4, -0.5), (1, 2.4, 1), "tr_spike")]))
    return m


def burning_horde():
    m = rig("trim_burning_horde")
    attach(m, "body", part("chain_a", pivot=(-4, 0.8, -3.2), rot=(0, 0, 0.9), boxes=[box((0, -0.7, -0.5), (13, 1.4, 1), "tr_chain")]),
           part("chain_b", pivot=(-4, 0.8, 3.2), rot=(0, 0, 0.9), boxes=[box((0, -0.7, -0.5), (13, 1.4, 1), "tr_chain")]),
           part("brand", pivot=(0, 3, -2.95), boxes=[box((-1.5, 0, -0.2), (3, 3, 0.2), "tr_brand")]))
    return m


def the_swarm():
    m = rig("trim_the_swarm")
    attach(m, "body", part("sac_l", pivot=(-1.8, 2.5, 2.4), boxes=[box((-1.5, 0, 0), (3, 4, 2.2), "tr_acid")]),
           part("sac_r", pivot=(1.9, 4, 2.4), boxes=[box((-1.3, 0, 0), (2.6, 3.4, 1.8), "tr_acid")]),
           part("sac_low", pivot=(0, 7.6, 2.4), boxes=[box((-1.2, 0, 0), (2.4, 2.6, 1.6), "tr_acid")]))
    for s, arm, x, sx in (("r", "right_arm", -3, -1), ("l", "left_arm", 3, 1)):
        attach(m, arm, part(f"spur_shoulder_{s}", pivot=(x * 0.4, -2.4, 0.5), rot=(-0.4, 0, -0.7 * sx),
                            boxes=[box((-0.6, -3.4, -0.6), (1.2, 3.4, 1.2), "tr_chitin")]),
               part(f"spur_elbow_{s}", pivot=(x * 0.4, 5.5, 2.1), rot=(0.9, 0, 0), boxes=[box((-0.5, -0.5, 0), (1, 1, 2.8), "tr_chitin")]))
    return m


def silverwood_reavers():
    m = rig("trim_silverwood_reavers")
    attach(m, "head", part("mask", pivot=(0, -4.5, -4.35), boxes=[box((-3.5, -3, -0.4), (7, 5, 0.4), "tr_mask")]),
           part("antler_r", pivot=(-3, -8, -2), rot=(0.2, 0, -0.5), boxes=[box((-0.5, -4, -0.5), (1, 4, 1), "tr_antler")],
                children=[part("antler_r_tine", pivot=(0, -2.5, 0), rot=(0, 0, -0.8), boxes=[box((-0.4, -2.2, -0.4), (0.8, 2.2, 0.8), "tr_antler")])]))
    attach(m, "head", mirror(find(m, "antler_r")))
    for limb, s in (("right_arm", "r"), ("left_arm", "l"), ("right_leg", "rl"), ("left_leg", "ll")):
        x = -3 if limb == "right_arm" else -1 if limb == "left_arm" else -2
        attach(m, limb, overlay(f"vines_{s}", (x, 0 if "leg" in limb else -2, -2), (4, 12, 4), "tr_vine", 0.3))
    return m


def ironbeard_clan():
    m = rig("trim_ironbeard_clan")
    attach(m, "head", part("face_guard", pivot=(0, -3.6, -4.45), boxes=[box((-3.6, 0, -0.5), (7.2, 3.6, 0.5), "tr_guard")]),
           part("beard_ring_a", pivot=(-1.5, 0.4, -4.2), boxes=[box((-0.6, 0, -0.6), (1.2, 1, 1.2), "tr_ring")]),
           part("beard_ring_b", pivot=(1.5, 0.4, -4.2), boxes=[box((-0.6, 0, -0.6), (1.2, 1, 1.2), "tr_ring")]),
           part("beard_ring_c", pivot=(0, 2.2, -4.0), boxes=[box((-0.6, 0, -0.6), (1.2, 1, 1.2), "tr_ring")]))
    return m


def fallen_host():
    m = rig("trim_fallen_host")
    attach(m, "body", part("wing_r", pivot=(-1.5, 2, 2.2), rot=(0.35, -0.55, 0.2), boxes=[box((-12, -2, 0), (12, 14, 0), "tr_wing")]),
           part("wing_l", pivot=(1.5, 2, 2.2), rot=(0.35, 0.55, -0.2), boxes=[box((0, -2, 0), (12, 14, 0), "tr_wing", mirror=True)]))
    halo = [part(f"halo_{i}", pivot=(0, -11, 0), rot=(0, i * 1.5708, 0), boxes=[box((-3.5, 0, -3.5), (7, 0.6, 0.6), "tr_halo")])
            for i in range(4)]
    attach(m, "head", part("halo", pivot=(0, 0, 0), rot=(0.15, 0, 0.12), children=halo))
    return m


def models():
    return [marauders(), black_legion(), burning_horde(), the_swarm(), silverwood_reavers(), ironbeard_clan(), fallen_host()]


def tr_material(mat, side, x, y, w, h, pal, rng):
    """Materials for faction trim. Returns NotImplemented for anything else."""
    if not mat.startswith("tr_"):
        return NotImplemented
    p = PAL
    if mat == "tr_paint":
        # Only the face carries paint: three slanted red stripes; everything else stays bare.
        if side == "front" and ((x in (1, 2) and 3 <= y <= 6) or (x in (5, 6) and 3 <= y <= 6) or (y == 7 and 2 <= x <= 5)):
            return hexc(p["paint"]) if rng.random() < 0.85 else hexc(p["paint_dark"])
        return None
    if mat in ("tr_bone", "tr_antler"):
        c = mix(hexc(p["bone"]), hexc(p["bone_dark"]), rng.uniform(0, 0.35))
        return shade(c, 1.1) if side == "top" else c
    if mat == "tr_belt":
        return mix(hexc("4a2e18"), hexc("24160a"), rng.uniform(0, 0.4))
    if mat in ("tr_tabard", "tr_tabard_skull"):
        c = mix(hexc(p["tabard"]), hexc(p["tabard_dark"]), rng.uniform(0, 0.4))
        if y == h - 1 and x % 2 == 0:
            return None                                              # ragged hem
        if mat == "tr_tabard_skull" and side == "front":
            skull = {(2, 2), (3, 2), (4, 2), (1, 3), (2, 3), (3, 3), (4, 3), (5, 3), (1, 4), (3, 4), (5, 4),
                     (2, 5), (3, 5), (4, 5), (2, 6), (4, 6)}
            if (x, y) in skull:
                return hexc(p["skull"])
        return c
    if mat in ("tr_iron", "tr_guard"):
        c = mix(hexc(p["iron"]), hexc(p["iron_dark"]), rng.uniform(0, 0.4))
        if mat == "tr_guard" and side == "front" and y == 1 and x % 2 == 1:
            return glow(hexc(p["rune"]))                             # etched runes, faintly lit
        if mat == "tr_guard" and side == "front" and y == 2 and 1 <= x <= w - 2 and x % 3 == 0:
            return hexc("0a0a0a")                                    # breathing slits
        return c
    if mat == "tr_spike":
        return mix(hexc(p["spike"]), hexc(p["iron_dark"]), y / max(1, h) * 0.6)
    if mat == "tr_chain":
        if x % 3 == 2:
            return glow(mix(hexc(p["ember"]), hexc("3a0a04"), 0.4))      # links still glowing from the forge
        return hexc(p["chain_hi"]) if (x + y) % 2 == 0 else hexc(p["chain"])
    if mat == "tr_brand":
        if (x == 1 or y == 1) and side == "front":
            return glow(hexc(p["ember"]))
        return hexc(p["soot"])
    if mat == "tr_acid":
        if side in ("back", "top", "left", "right") and rng.random() < 0.7:
            return glow(mix(hexc(p["acid"]), hexc(p["acid_dark"]), rng.uniform(0, 0.5)))
        return hexc(p["acid_dark"])
    if mat == "tr_chitin":
        c = mix(hexc(p["chitin"]), hexc(p["chitin_hi"]), rng.uniform(0, 0.4))
        return c
    if mat == "tr_mask":
        if side == "front" and y == 2 and x in (1, 2, w - 3, w - 2):
            return hexc("0a0a0a")                                    # eye holes
        return mix(hexc(p["bone"]), hexc("f4efe2"), rng.uniform(0, 0.4))
    if mat == "tr_vine":
        # A spiral of vine around the limb, with pale thorns; the rest is bare.
        if (y + x * 2) % 9 == 0:
            return hexc(p["thorn"]) if rng.random() < 0.15 else mix(hexc(p["vine"]), hexc(p["vine_dark"]), rng.uniform(0, 0.5))
        return None
    if mat == "tr_ring":
        return hexc(p["ring"]) if (x + y) % 2 == 0 else hexc(p["ring_dark"])
    if mat == "tr_wing":
        # Torn feathers: gaps along the lower edge and holes through the vane.
        if y > h * 0.6 and (x * 7 + y * 3) % 5 == 0:
            return None
        if rng.random() < 0.06:
            return None
        c = mix(hexc(p["ash"]), hexc(p["ash_dark"]), 0.2 + 0.3 * ((x // 2) % 2))
        return c
    if mat == "tr_halo":
        if x == 3 and side in ("front", "back", "top"):
            return None                                              # the crack
        return glow(mix(hexc(p["halo"]), hexc(p["halo_dark"]), 0.5)) if x % 3 else hexc(p["halo_dark"])
    return hexc("ff00ff")
