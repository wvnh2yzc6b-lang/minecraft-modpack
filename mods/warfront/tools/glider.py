"""The Mana Glider worn on a player's back: a swept wing of blue sailcloth on a wooden frame, a mana crystal at the
nose, matching the item icon. Imported by units.py. The game folds the two halves flat down the back on the ground
and spreads them wide while gliding (GliderLayer)."""
# part, box, mirror and the pixel helpers are injected by units.py (see bind()).

PAL = dict(sail="2f5fa8", sail_hi="4f86d0", sail_dark="1f3f78", wood="6a4424", wood_hi="8a5a30", trim="d4a017",
           crystal="6ee8ff", crystal_hi="d8fbff")


def bind(ns):
    globals().update(ns)


def model():
    wing_r = part("wing_r", pivot=(-0.6, 1.5, 3.4), rot=(0, 0, 0),
                  boxes=[box((-14, 0, 0), (14, 7, 0.4), "gl_sail")],
                  children=[part("spar_r", pivot=(0, 0, 0), boxes=[box((-14, -0.6, -0.4), (14, 1, 1), "gl_wood")]),
                            part("rib_r", pivot=(-7, 0, 0), rot=(0, 0, 0.45), boxes=[box((-0.4, 0, -0.3), (0.8, 7, 0.8), "gl_wood")])])
    spine = part("spine", pivot=(0, 0, 3.0), boxes=[box((-0.6, 0, -0.2), (1.2, 10, 1.2), "gl_wood")],
                 children=[part("crystal", pivot=(0, -0.2, 0.4), rot=(0, 0.785, 0), boxes=[box((-1, -2.4, -1), (2, 2.4, 2), "gl_crystal")]),
                           part("strap", pivot=(0, 2, -2.0), boxes=[box((-4.2, 0, -3.4), (8.4, 1, 3.6), "gl_strap")])])
    body = part("body", children=[spine, wing_r, mirror(wing_r)])
    return {"id": "mana_glider", "tex": [64, 32], "parts": [body]}


def gl_material(mat, side, x, y, w, h, pal, rng):
    if not mat.startswith("gl_"):
        return NotImplemented
    p = PAL
    if mat == "gl_sail":
        c = hexc(p["sail_hi"]) if (x // 3) % 2 == 0 else hexc(p["sail"])      # sailcloth panels
        if y == h - 1:
            c = hexc(p["sail_dark"])
        if x == w // 2 and side in ("front", "back"):
            c = hexc(p["trim"])                                                 # gold seam, as on the icon
        return shade(c, rng.uniform(0.95, 1.05))
    if mat == "gl_wood":
        c = mix(hexc(p["wood"]), hexc(p["wood_hi"]), rng.uniform(0, 0.4))
        return shade(c, 1.15) if side == "top" else c
    if mat == "gl_crystal":
        return glow(hexc(p["crystal_hi"] if (x + y) % 3 == 0 else p["crystal"]))
    if mat == "gl_strap":
        return mix(hexc("4a2e18"), hexc("24160a"), rng.uniform(0, 0.4))
    return hexc("ff00ff")
