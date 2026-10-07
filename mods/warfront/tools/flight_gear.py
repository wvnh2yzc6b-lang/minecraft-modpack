"""Race flight gear worn on a player's back: the orc rocket pack (two scrap tanks, a boiler, twin nozzles) and angel
wings (white feathers, gold-edged). Imported by units.py. FlightGearLayer folds the wings down the back on the ground
and spreads them in the air, and lights the pack's exhaust while it climbs. Placeholder looks until the owner sends
references."""
# part, box, mirror and the pixel helpers are injected by units.py (see bind()).

PAL = dict(metal="5a5f63", metal_hi="7d8388", rust="8a4a22", paint="4f6b2a", paint_hi="6a8a3a", brass="b08a3a",
           brass_hi="d8b25a", soot="2a2a2a", flame="ffb040", flame_hi="fff0a0", strap="4a2e18",
           feather="f4f1e8", feather_hi="ffffff", feather_shade="d6d0c0", gold="d4a017", gold_hi="ffe27a")


def bind(ns):
    globals().update(ns)


def rocket_pack():
    flame_r = part("flame_r", pivot=(-2.2, 11, 4.8), boxes=[box((-0.8, 0, -0.8), (1.6, 3, 1.6), "fg_flame")])
    pack = part("pack", pivot=(0, 0, 0), boxes=[
        box((-4, 1, 2), (8, 9, 1), "fg_plate"),
        box((-4, 0, 3), (3.5, 9, 3.5), "fg_tank"),
        box((0.5, 0, 3), (3.5, 9, 3.5), "fg_tank"),
        box((-1.5, 2.5, 3.6), (3, 6.5, 2.6), "fg_engine"),
        box((-3.4, 9, 3.6), (2.4, 2, 2.4), "fg_nozzle"),
        box((1.0, 9, 3.6), (2.4, 2, 2.4), "fg_nozzle"),
        box((-0.5, -2, 4.2), (1, 2.5, 1), "fg_nozzle"),                       # exhaust stack
        box((-3.5, -0.3, -2.3), (1.5, 1, 4.6), "fg_strap"),
        box((2.0, -0.3, -2.3), (1.5, 1, 4.6), "fg_strap"),
    ], children=[flame_r, mirror(flame_r)])
    body = part("body", children=[pack])
    return {"id": "rocket_pack", "tex": [64, 32], "parts": [body]}


def angel_wings():
    wing_r = part("wing_r", pivot=(-1.5, 1.0, 2.6), boxes=[
        box((-17, 0, 0), (17, 1.5, 1.5), "fg_bone"),
        box((-16, 1.5, 0.3), (16, 3, 0.8), "fg_covert"),
        box((-23, 2, 0.5), (14, 14, 0.4), "fg_primary"),
        box((-10, 3, 0.6), (10, 10, 0.4), "fg_secondary"),
    ])
    body = part("body", children=[wing_r, mirror(wing_r)])
    return {"id": "angel_wings", "tex": [64, 64], "parts": [body]}


def models():
    return [rocket_pack(), angel_wings()]


def _feathers(x, y, w, h, side, rng, long_end):
    """Long feathers with jagged tips; each feather two pixels wide, longer toward {long_end} (0 or w - 1)."""
    p = PAL
    if side not in ("front", "back"):
        return shade(hexc(p["feather_shade"]), rng.uniform(0.95, 1.05))
    far = abs(x - long_end) / max(1, w - 1)
    length = h - 1 - int(far * h * 0.45) - ((x // 2) % 2)
    if y > length:
        return None
    c = hexc(p["feather_hi"] if (x // 2) % 2 == 0 else p["feather"])
    if x % 2 == 1:
        c = hexc(p["feather_shade"])                                           # quill line between feathers
    if y >= length - 1:
        c = mix(c, hexc(p["feather_shade"]), 0.5)
    return shade(c, rng.uniform(0.96, 1.03))


def fg_material(mat, side, x, y, w, h, pal, rng):
    if not mat.startswith("fg_"):
        return NotImplemented
    p = PAL
    if mat == "fg_plate":
        return mix(hexc(p["metal"]), hexc(p["rust"]), rng.uniform(0, 0.5)) if rng.random() < 0.3 else \
            shade(hexc(p["metal"]), rng.uniform(0.85, 1.05))
    if mat == "fg_tank":
        if y in (1, h - 2):
            return hexc(p["brass_hi"] if side == "top" else p["brass"])          # brass bands
        c = hexc(p["paint_hi"] if x == 1 else p["paint"])
        if rng.random() < 0.12:
            c = hexc(p["rust"])                                                 # chipped paint
        if side == "top":
            c = hexc(p["metal_hi"])
        return shade(c, rng.uniform(0.9, 1.05))
    if mat == "fg_engine":
        if edge(x, y, w, h):
            return hexc(p["brass"])
        if side == "back" and y == h // 2 and 0 < x < w - 1:
            return glow(hexc(p["flame"]))                                       # firebox grate
        return shade(hexc(p["metal"]), rng.uniform(0.75, 0.95))
    if mat == "fg_nozzle":
        if side == "bottom":
            return glow(hexc(p["flame"])) if not edge(x, y, w, h) else hexc(p["soot"])
        return mix(hexc(p["soot"]), hexc(p["metal"]), rng.uniform(0, 0.4))
    if mat == "fg_flame":
        t = y / max(1, h - 1)
        if rng.random() < t * 0.6:
            return None
        return glow(mix(hexc(p["flame_hi"]), hexc(p["flame"]), t))
    if mat == "fg_strap":
        return mix(hexc(p["strap"]), hexc("24160a"), rng.uniform(0, 0.4))
    if mat == "fg_bone":
        if side == "top" or y == 0:
            return hexc(p["gold_hi"] if x % 3 == 0 else p["gold"])                # gold leading edge
        return shade(hexc(p["feather_hi"]), rng.uniform(0.95, 1.02))
    if mat == "fg_covert":
        if side in ("front", "back") and y == h - 1 and x % 2 == 1:
            return None                                                         # scalloped edge
        c = hexc(p["feather_hi"] if (x + y) % 2 == 0 else p["feather"])
        return shade(c, rng.uniform(0.95, 1.03))
    if mat == "fg_primary":
        return _feathers(x, y, w, h, side, rng, 0 if side == "front" else w - 1)
    if mat == "fg_secondary":
        return _feathers(x, y, w, h, side, rng, w - 1 if side == "front" else 0)
    return hexc("ff00ff")
