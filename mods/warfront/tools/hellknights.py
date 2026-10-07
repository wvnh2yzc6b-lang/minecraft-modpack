"""Demon hellknights: the full-sized demon soldiers, worn over the soldier's player rig. Imported by units.py.

Shared look: faceless black-iron plate, horns breaking through the helm, legs ending in hooves with a backward hock
spur (a digitigrade hint on the straight player leg), and ember-orange glowing in the visor slit and the plate seams.
  Hellguard (shieldbearer): the heaviest plate, doubled pauldrons, a black shield.
  Fiend (swordsman): plate and blade.
  Archfiend (captain): a horned crest and a cape.
  Hellknight (champion): the most ornate: great horns, spiked pauldrons, an ember sigil on the chest, a cape.
  Blood Witch (healer): blood-red robes over light pauldrons, a horned hood, no full plate.
The Burning Horde wears the same geometry in its own colors.
"""
# part, box, overlay, attach, mirror and the pixel helpers are injected by units.py (see bind()).

ROLES = ["shieldbearer", "swordsman", "captain", "champion", "healer"]
SKINS = ["demon", "burning_horde"]

PALETTES = {
    "demon": dict(plate="1d1b20", plate_hi="3a3640", plate_dark="0c0b0e", ember="ff7a1a", ember_hi="ffd27a",
                  horn="1e1410", horn_tip="4a3a30", hoof="1a1210", robe="7a0f12", robe_dark="3a0608", robe_hi="a8242a",
                  cape="4a0a0e", cape_dark="220406", trim="c9772a", leather="3a2418", leather_dark="1c100a"),
    "burning_horde": dict(plate="141012", plate_hi="2e2428", plate_dark="070506", ember="ff4a00", ember_hi="ffb040",
                          horn="1a1210", horn_tip="ff7a1a", hoof="0e0908", robe="4a0806", robe_dark="1e0302",
                          robe_hi="8a1a10", cape="2a0604", cape_dark="120202", trim="ff6a1a", leather="2a1810",
                          leather_dark="140a06"),
}


def bind(ns):
    """Receives the model and painting helpers from units.py."""
    globals().update(ns)


def rig(model_id):
    """Empty vanilla player rig to hang gear on; pivots match PlayerModel."""
    return {"id": model_id, "tex": [128, 128], "parts": [
        part("head"), part("hat"), part("body"), part("right_arm", pivot=(-5, 2, 0)),
        part("left_arm", pivot=(5, 2, 0)), part("right_leg", pivot=(-1.9, 12, 0)), part("left_leg", pivot=(1.9, 12, 0)),
    ]}


# ----------------------------------------------------------------------------- shared pieces

def horns(scale=1.0, mat="hk_horn"):
    """Horns breaking out through the helm, sweeping out, up and back."""
    s = scale
    horn_r = part("horn_r", pivot=(-3.6, -6.5, -0.5), rot=(0.35, 0.1, -0.75),
                  boxes=[box((-1.2, -3.0 * s, -1.2), (2.4, 3.0 * s, 2.4), mat)],
                  children=[part("horn_r_mid", pivot=(0, -3.0 * s, 0), rot=(0.25, 0, 0.55),
                                 boxes=[box((-0.9, -2.6 * s, -0.9), (1.8, 2.6 * s, 1.8), mat)],
                                 children=[part("horn_r_tip", pivot=(0, -2.6 * s, 0), rot=(0.3, 0, 0.45),
                                                boxes=[box((-0.5, -2.2 * s, -0.5), (1, 2.2 * s, 1), "hk_horn_tip")])])])
    return [horn_r, mirror(horn_r)]


def helm(scale_horns=1.0, crest=False):
    parts = [overlay("hk_helm", (-4, -8, -4), (8, 8, 8), "hk_helm", 0.7)]
    parts += horns(scale_horns)
    if crest:
        parts.append(part("crest", pivot=(0, -8.6, 0), boxes=[box((-0.6, -2.5, -4.5), (1.2, 2.5, 9), "hk_crest")]))
    return parts


def cuirass(m, grow=0.5):
    attach(m, "body", overlay("hk_cuirass", (-4, 0, -2), (8, 12, 4), "hk_plate", grow),
           overlay("hk_fauld", (-4, 9, -2), (8, 3, 4), "hk_plate_dark", grow + 0.25))


def pauldrons(m, layers=1, spikes=False, mat="hk_plate"):
    def make(s, sx):
        x0 = -3.6 if s == "r" else -1.4
        out = [overlay(f"pauldron_{s}", (-3 if s == "r" else -1, -2, -2), (4, 4, 4), mat, 0.9)]
        if layers > 1:
            out.append(overlay(f"pauldron2_{s}", (-3 if s == "r" else -1, -2.6, -2), (4, 2, 4), mat, 1.4))
        if spikes:
            for i, z in enumerate((-1.2, 1.2)):
                out.append(part(f"pauldron_spike_{s}{i}", pivot=(x0 + (0 if s == "r" else 5), -3.2, z),
                                rot=(0, 0, -0.5 * sx), boxes=[box((-0.5, -2.5, -0.5), (1, 2.5, 1), "hk_horn")]))
        out.append(overlay(f"gauntlet_{s}", (-3 if s == "r" else -1, 6, -2), (4, 6, 4), mat, 0.5))
        return out
    attach(m, "right_arm", *make("r", -1))
    attach(m, "left_arm", *make("l", 1))


def hoofed_legs(m, mat="hk_plate"):
    def make(s, sx):
        return [overlay(f"greave_{s}", (-2, 0, -2), (4, 10, 4), mat, 0.45),
                # The hock: a backward spur halfway down, hinting at a beast's bent leg.
                part(f"hock_{s}", pivot=(0, 6.5, 2.2), rot=(0.75, 0, 0),
                     boxes=[box((-1.5, 0, -0.6), (3, 3.5, 1.6), "hk_plate_dark")]),
                part(f"hoof_{s}", pivot=(0, 10, 0), boxes=[box((-2.3, 0, -2.9), (4.6, 2.2, 5.0), "hk_hoof")])]
    attach(m, "right_leg", *make("r", -1))
    attach(m, "left_leg", *make("l", 1))


def cape(m, length=14):
    attach(m, "body", part("cape", pivot=(0, 0.2, 2.6), rot=(0.12, 0, 0),
                           boxes=[box((-4.5, 0, 0), (9, length, 0.5), "hk_cape")]))


# ----------------------------------------------------------------------------- the five

def hellguard():
    m = rig("gear_shieldbearer_demon")
    attach(m, "head", *helm(0.85))
    attach(m, "head", overlay("hk_gorget", (-4, -1.2, -4), (8, 2, 8), "hk_plate_dark", 1.1))
    cuirass(m, 0.65)
    attach(m, "body", overlay("hk_breastplate", (-4, 0.5, -2), (8, 7, 4), "hk_plate", 0.95))
    pauldrons(m, layers=2)
    hoofed_legs(m)
    return m


def fiend():
    m = rig("gear_swordsman_demon")
    attach(m, "head", *helm(1.0))
    cuirass(m)
    pauldrons(m)
    hoofed_legs(m)
    return m


def archfiend():
    m = rig("gear_captain_demon")
    attach(m, "head", *helm(1.1, crest=True))
    cuirass(m)
    pauldrons(m, layers=2)
    cape(m)
    hoofed_legs(m)
    return m


def hellknight():
    m = rig("gear_champion_demon")
    attach(m, "head", *helm(1.45, crest=True))
    cuirass(m, 0.6)
    attach(m, "body", part("sigil", pivot=(0, 3, -2.75), boxes=[box((-2, 0, -0.3), (4, 4, 0.3), "hk_sigil")]))
    pauldrons(m, layers=2, spikes=True)
    cape(m, 16)
    hoofed_legs(m)
    return m


def blood_witch():
    m = rig("gear_healer_demon")
    attach(m, "head", overlay("hood", (-4, -8, -4), (8, 8, 8), "hk_hood", 0.75))
    attach(m, "head", *horns(0.75))
    attach(m, "body", overlay("robe", (-4, 0, -2), (8, 12, 4), "hk_robe", 0.4),
           part("robe_skirt", pivot=(0, 10.5, 0), boxes=[box((-4.6, 0, -2.6), (9.2, 11, 5.2), "hk_robe")]),
           overlay("robe_sash", (-4, 8.5, -2), (8, 1.5, 4), "hk_sash", 0.55))
    pauldrons(m, mat="hk_plate_light")
    attach(m, "right_arm", overlay("sleeve_r", (-3, 2, -2), (4, 5, 4), "hk_robe", 0.6))
    attach(m, "left_arm", overlay("sleeve_l", (-1, 2, -2), (4, 5, 4), "hk_robe", 0.6))
    for s, leg in (("r", "right_leg"), ("l", "left_leg")):
        attach(m, leg, part(f"hoof_{s}", pivot=(0, 10, 0), boxes=[box((-2.3, 0, -2.9), (4.6, 2.2, 5.0), "hk_hoof")]))
    return m


def models():
    return [hellguard(), fiend(), archfiend(), hellknight(), blood_witch()]


# ----------------------------------------------------------------------------- painting

def _plate(pal, rng, x, y, w, h, side, base="plate"):
    c = mix(hexc(pal[base]), hexc(pal["plate_dark"]), rng.uniform(0, 0.4))
    if rng.random() < 0.06:
        c = mix(c, hexc(pal["plate_hi"]), 0.6)                        # scratches catching the light
    if side == "top":
        c = mix(c, hexc(pal["plate_hi"]), 0.35)
    elif side == "bottom":
        c = shade(c, 0.7)
    # Ember seams: a thin line of heat along the bottom edge of each plate, broken here and there.
    if side in ("front", "back", "left", "right") and y == h - 1 and h > 2 and x % 3 != 2 and rng.random() < 0.6:
        return glow(mix(hexc(pal["ember"]), hexc("5a1404"), rng.uniform(0.2, 0.5)))
    if edge(x, y, w, h) and side != "bottom":
        c = mix(c, hexc(pal["plate_hi"]), 0.25)                       # a rim of light on the plate edges
    return c


def hk_material(mat, side, x, y, w, h, pal, rng):
    """Materials for the hellknights. Returns NotImplemented for anything else."""
    if not mat.startswith("hk_"):
        return NotImplemented
    if mat in ("hk_plate", "hk_plate_dark", "hk_plate_light"):
        base = {"hk_plate": "plate", "hk_plate_dark": "plate_dark", "hk_plate_light": "plate_hi"}[mat]
        return _plate(pal, rng, x, y, w, h, side, base)
    if mat == "hk_helm":
        # Faceless: a single glowing visor slit across the front.
        if side == "front" and y == h // 2 - 1 and 1 <= x <= w - 2:
            return glow(mix(hexc(pal["ember"]), hexc(pal["ember_hi"]), 0.3 if 2 <= x <= w - 3 else 0.0))
        return _plate(pal, rng, x, y, w, h, side)
    if mat == "hk_crest":
        return _plate(pal, rng, x, y, w, h, side)
    if mat == "hk_horn":
        return mix(hexc(pal["horn"]), hexc(pal["horn_tip"]), min(1.0, (h - 1 - y) / max(1, h) * 0.5 + rng.uniform(0, 0.15)))
    if mat == "hk_horn_tip":
        return mix(hexc(pal["horn_tip"]), hexc(pal["horn"]), y / max(1, h) * 0.6)
    if mat == "hk_hoof":
        c = shade(hexc(pal["hoof"]), rng.uniform(0.85, 1.15))
        return shade(c, 1.3) if side == "top" else c
    if mat == "hk_cape":
        c = mix(hexc(pal["cape"]), hexc(pal["cape_dark"]), 0.25 + 0.35 * ((x // 2) % 2))   # folds
        if y >= h - 1 and rng.random() < 0.4:
            return None                                                # ragged hem
        if y == 0:
            c = hexc(pal["trim"])
        return c
    if mat in ("hk_robe", "hk_hood"):
        c = mix(hexc(pal["robe"]), hexc(pal["robe_dark"]), 0.15 + 0.45 * ((x + (y // 3)) % 3 == 0))
        if rng.random() < 0.05:
            c = mix(c, hexc(pal["robe_hi"]), 0.6)
        if mat == "hk_hood" and side == "front" and 2 <= x <= w - 3 and 2 <= y <= h - 2:
            if y == h // 2 and x in (2, w - 3):
                return glow(hexc(pal["ember"]))                       # eyes in the shadow of the hood
            return hexc("080404")
        return c
    if mat == "hk_sash":
        return hexc(pal["trim"]) if (x + y) % 3 else shade(hexc(pal["trim"]), 0.7)
    if mat == "hk_sigil":
        cx, cy = (w - 1) / 2, (h - 1) / 2
        if abs(x - cx) + abs(y - cy) <= 1.0 or (x == round(cx) and y < h - 1):
            return glow(hexc(pal["ember"]))
        return _plate(pal, rng, x, y, w, h, side)
    return hexc("ff00ff")
