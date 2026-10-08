"""The angels' war beast: an armored Moa, a tall flightless bird in gilded barding. Warfront's own model, so it works
without the Aether. Imported by units.py. Parts follow the humanoid rig (head, body, arms as wings, legs); feet at y=24.
Placeholder look until the owner sends references."""
# part, box, mirror and the pixel helpers are injected by units.py (see bind()).

PALETTES = {
    "angel": dict(feather="8fb4e0", feather_dark="5f80b0", scale="c9a24a", scale_dark="8a6a28", beak="e0b040",
                  armor="f2efe6", armor_trim="d4a017", eye="7fd8ff"),
    "fallen_host": dict(feather="9a9aa2", feather_dark="5e5e66", scale="7a6a4a", scale_dark="4a3e28", beak="8a7a50",
                        armor="b8b4aa", armor_trim="8a7330", eye="c8c8ff"),
}


def bind(ns):
    globals().update(ns)


def moa():
    leg = part("right_leg", pivot=(-3, 10, 2), boxes=[
        box((-1.5, 0, -1.5), (3, 6, 3), "mo_feather"),
        box((-0.75, 6, -0.75), (1.5, 7, 1.5), "mo_scale"),
        box((-2, 13, -3.5), (4, 1, 5), "mo_scale"),
    ])
    wing = part("right_arm", pivot=(-5, 2, -1), rot=(0.1, 0, 0.1), boxes=[box((-1, 0, -2), (1, 7, 10), "mo_feather")])
    head = part("head", pivot=(0, 1, -6), boxes=[
        box((-1.5, -12, -1.5), (3, 12, 3), "mo_feather"),               # neck
        box((-2.5, -16, -4), (5, 5, 6), "mo_feather"),                  # head
        box((-1.5, -14.5, -8), (3, 2.5, 4), "mo_beak"),                 # beak
        box((-2.75, -16.5, -4.5), (5.5, 2, 5.5), "mo_armor"),           # helm
        box((-2.6, -14, -3.5), (0.2, 1, 1), "mo_eye"), box((2.4, -14, -3.5), (0.2, 1, 1), "mo_eye"),
        box((-0.5, -19, -3), (1, 3, 4), "mo_trim"),                     # helm crest
    ])
    body = part("body", pivot=(0, 0, 0), boxes=[
        box((-5, 0, -6), (10, 10, 14), "mo_feather"),
        box((-5.5, -0.5, -5), (11, 6, 11), "mo_armor"),                 # barding
        box((-5.6, 5, -5), (11.2, 1, 11), "mo_trim"),
        box((-4.5, 10, -4), (9, 1, 10), "mo_feather_dark"),
    ], children=[part("tail", pivot=(0, 2, 8), rot=(0.5, 0, 0), boxes=[box((-3.5, -1, 0), (7, 3, 7), "mo_feather_dark")])])
    return {"id": "angel_moa", "tex": [128, 64], "parts": [head, part("hat"), body, wing, mirror(wing), leg, mirror(leg)]}


def models():
    return [moa()]


def mo_material(mat, side, x, y, w, h, pal, rng):
    if not mat.startswith("mo_"):
        return NotImplemented
    p = pal
    if mat == "mo_feather":
        c = hexc(p["feather"]) if (x + (y // 2) * 3) % 7 else hexc(p["feather_dark"])
        return shade(c, rng.uniform(0.93, 1.05))
    if mat == "mo_feather_dark":
        return shade(hexc(p["feather_dark"]), rng.uniform(0.9, 1.08))
    if mat == "mo_scale":
        return hexc(p["scale"]) if (x + y) % 3 else hexc(p["scale_dark"])
    if mat == "mo_beak":
        return shade(hexc(p["beak"]), 1.1 if side == "top" else 0.95)
    if mat == "mo_armor":
        c = hexc(p["armor"])
        if edge(x, y, w, h):
            c = hexc(p["armor_trim"])
        return shade(c, rng.uniform(0.95, 1.04))
    if mat == "mo_trim":
        return hexc(p["armor_trim"])
    if mat == "mo_eye":
        return glow(hexc(p["eye"]))
    return hexc("ff00ff")
