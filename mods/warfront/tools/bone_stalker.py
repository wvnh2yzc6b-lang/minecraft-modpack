"""The demons' war beast: the Bone Stalker, after docs/art-reference/demon-beast.png. Imported by units.py.

A gaunt, bone-white skeletal predator that prowls on all fours: long thin arms reach down to the ground as front
legs, digitigrade hind legs, ribs and spine showing. Blood-red barbs run down its back and stud the elbows, knees
and heels; it has a skull face with a gaping fanged jaw and small red eyes under a cage of swept-back horn-spikes,
tall insect wings with red struts and torn pale membrane, and a long spined tail that curls up over its back to a
big curved red stinger. Parts follow the humanoid rig so the game can animate them (feet at y=24, front is -z).
"""
import math

# part, box, mirror and the pixel helpers are injected by units.py (see bind()).

PALETTES = {
    # Player demons: bone white and pale grey, dark blood-red barbs, talons, wing struts and stinger.
    "demon": dict(bone="ece8df", bone_mid="cdc6b9", bone_dark="8f877b", shadow="4e463e", red="8e1612", red_dark="420606",
                  red_hi="c4352b", eye="ff3a22", membrane="e9e6e0", membrane_dark="bdb7ad", mouth="2a0505"),
    # The Burning Horde: soot-stained bone and smoldering ember barbs.
    "burning_horde": dict(bone="b9b0a4", bone_mid="958b7e", bone_dark="5e554b", shadow="2c2622", red="b2401a",
                          red_dark="4a1206", red_hi="ff8a3a", eye="ffb040", membrane="a8a096", membrane_dark="6e665c",
                          mouth="1a0604"),
}

LEAN = 1.15          # the torso pitches forward this far (radians); parts on it undo it to point along the world


def bind(ns):
    globals().update(ns)


def _spike(name, pivot, rot, length=3.0, width=1.0, mat="bs_barb"):
    """A barb pointing along its part's -y."""
    return part(name, pivot=pivot, rot=rot, boxes=[box((-width / 2, -length, -width / 2), (width, length, width), mat)])


def _talons(prefix, count, spread, length, down):
    """Long curved talons reaching forward (-z) from a hand or foot, bending down at the tip."""
    out = []
    for i in range(count):
        x = (i - (count - 1) / 2) * spread
        out.append(part(f"{prefix}{i}", pivot=(x, 0.4, -1.4), rot=(down, (i - (count - 1) / 2) * -0.25, 0),
                        boxes=[box((-0.4, -0.4, -length), (0.8, 0.8, length), "bs_bone")], children=[
                            part(f"{prefix}{i}_tip", pivot=(0, 0, -length), rot=(0.7, 0, 0),
                                 boxes=[box((-0.3, -0.3, -2), (0.6, 0.6, 2), "bs_talon")])]))
    return out


def _arm():
    """A front leg: a long thin arm reaching down and forward, a bony elbow barb, a clawed hand on the ground."""
    hand = part("hand_r", pivot=(0, 9, 0), rot=(-0.1, 0, 0.08), boxes=[box((-1.5, -0.2, -1.6), (3, 1.2, 2.6), "bs_bone_mid")],
                children=_talons("finger_r", 3, 1.1, 2.8, 0.15))
    fore = part("forearm_r", pivot=(0, 9, 0), rot=(0.5, 0, -0.5), boxes=[box((-0.8, 0, -0.8), (1.6, 9, 1.6), "bs_bone")], children=[
        hand,
        _spike("elbow_barb_r", (0, 0.5, 0.7), (-2.3, 0, 0), 3.5, 0.9),
        _spike("forearm_barb_r", (-0.7, 4.5, 0.2), (-2.0, 0, -0.5), 2.2, 0.7),
    ])
    return part("right_arm", pivot=(-4, 5.5, -6), rot=(-0.4, 0, 0.42), boxes=[
        box((-1.6, -1.6, -1.6), (3.2, 3.2, 3.2), "bs_bone_mid"),              # shoulder knob
        box((-1, 0, -1), (2, 9, 2), "bs_bone"),
    ], children=[fore, _spike("shoulder_barb_r", (-0.8, -1.2, 0.6), (-0.6, 0, -0.8), 3.0, 1.0)])


def _leg():
    """A hind leg: digitigrade like a hound's, thigh forward, shin back, a long ankle, clawed foot; barbed knee and heel."""
    foot = part("foot_r", pivot=(0, 4.2, 0), rot=(0.3, 0, 0), boxes=[box((-1.4, -0.3, -1.8), (2.8, 1.2, 2.8), "bs_bone_mid")],
                children=_talons("toe_r", 3, 0.9, 2.2, 0.1))
    ankle = part("ankle_r", pivot=(0, 6, 0), rot=(-1.1, 0, 0), boxes=[box((-0.75, 0, -0.75), (1.5, 4.2, 1.5), "bs_bone")],
                 children=[foot, _spike("heel_barb_r", (0, 0.4, 0.6), (-2.4, 0, 0), 3.0, 0.9)])
    shin = part("shin_r", pivot=(0, 7, 0), rot=(1.4, 0, 0), boxes=[box((-1, 0, -1), (2, 6, 2), "bs_bone")],
                children=[ankle, _spike("knee_barb_r", (0, 0.3, -0.8), (0.4, 0, 0), 3.0, 1.0)])
    return part("right_leg", pivot=(-3, 10, 3), rot=(-0.6, 0, 0.05), boxes=[
        box((-1.6, -0.5, -1.6), (3.2, 7.5, 3.2), "bs_bone_mid"),
    ], children=[shin])


def _wing(name, length, width, flare, sweep, y):
    """An insect wing: a tall thin plane of pale membrane with red struts, rising up and back from the shoulders.
    The root undoes the torso's lean, so sweep (back) and flare (out to the side) are in the world's frame."""
    return part(name.replace("_r", "_root_r"), pivot=(-1.5, y, 2.2), rot=(-LEAN, 0, 0), children=[
        part(name, rot=(sweep, 0, -flare), boxes=[box((0, -length, 0), (0, length, width), "bs_wing")])])


def _tail():
    """A long segmented tail lined with barbs, curling up over the back to a big curved stinger."""
    curl = [0.05, 0.12, 0.3, 0.45, 0.55, 0.55]
    sizes = [(2.4, 6), (2.2, 6), (2.0, 5.5), (1.8, 5), (1.6, 5), (1.4, 4.5)]
    stinger = part("stinger", pivot=(0, 0, sizes[-1][1]), rot=(0.5, 0, 0), boxes=[
        box((-1.3, -1.3, 0), (2.6, 2.6, 3), "bs_red"),                    # venom bulb
    ], children=[part("stinger_hook", pivot=(0, 0, 3), rot=(0.9, 0, 0), boxes=[box((-0.6, -0.6, 0), (1.2, 1.2, 4), "bs_barb")],
                      children=[part("stinger_tip", pivot=(0, 0, 4), rot=(0.7, 0, 0),
                                     boxes=[box((-0.35, -0.35, 0), (0.7, 0.7, 3), "bs_barb")])])])
    child = stinger
    for i in reversed(range(len(sizes))):
        w, l = sizes[i]
        barbs = [_spike(f"tail_barb_{i}_{j}", (0, -w / 2, 1 + j * (l - 2)), (-0.5, 0, 0), 2.6 - i * 0.25, 0.7)
                 for j in range(2)]
        pivot = (0, 0, sizes[i - 1][1]) if i > 0 else (0, 0.5, 1.5)
        rot = (curl[i] - (LEAN if i == 0 else 0), 0, 0)
        child = part(f"tail_{i}", pivot=pivot, rot=rot, boxes=[box((-w / 2, -w / 2, 0), (w, w, l), "bs_vertebra")],
                     children=[child, *barbs])
    return child


def bone_stalker():
    # Skull after the head studies: long and narrow, a heavy brow over small red eyes, a gaping jaw full of long
    # fangs, horn-spikes framing the head like a cage and a crown of spikes swept back.
    head = part("head", pivot=(0, 4.5, -7.5), boxes=[
        box((-2.25, -3.5, -5), (4.5, 4, 5.5), "bs_skull"),                # cranium
        box((-1.75, -1.5, -8), (3.5, 2.5, 3.2), "bs_skull"),              # long upper jaw
        box((-2.5, -3.6, -5.4), (5, 1.1, 1.6), "bs_bone_mid"),            # brow ridge
        box((-1.9, -2.5, -5.45), (1.1, 0.8, 0.3), "bs_eye"),
        box((0.8, -2.5, -5.45), (1.1, 0.8, 0.3), "bs_eye"),
        box((-1.5, 0.9, -7.6), (3, 0.6, 2.6), "bs_mouth"),                 # throat seen through the open jaw
    ], children=[
        part("jaw", pivot=(0, 0.5, -1.5), rot=(0.55, 0, 0), boxes=[box((-1.6, 0, -6.2), (3.2, 1.3, 6.2), "bs_bone_mid")], children=[
            *[part(f"jaw_fang_{i}", pivot=(x, 0.1, z), rot=(0, 0, 0), boxes=[box((-0.25, -1.8, -0.25), (0.5, 1.8, 0.5), "bs_fang")])
              for i, (x, z) in enumerate(((-1.2, -5.6), (1.2, -5.6), (-1.3, -3.8), (1.3, -3.8), (0, -5.9)))]]),
        *[part(f"fang_{i}", pivot=(x, 1, z), rot=(0.15, 0, 0), boxes=[box((-0.25, 0, -0.25), (0.5, l, 0.5), "bs_fang")])
          for i, (x, z, l) in enumerate(((-1.3, -7.5, 2.4), (1.3, -7.5, 2.4), (-1.4, -6, 1.8), (1.4, -6, 1.8), (-0.5, -7.8, 1.6), (0.5, -7.8, 1.6)))],
        # The cage: two long curved horn-spikes sweeping back and up from each temple.
        part("horn_r", pivot=(-2.2, -2.6, -3.5), rot=(-0.9, -0.35, -0.35), boxes=[box((-0.6, -5, -0.6), (1.2, 5, 1.2), "bs_bone")],
             children=[part("horn_r_tip", pivot=(0, -5, 0), rot=(-0.5, 0, 0.3), boxes=[box((-0.4, -3.5, -0.4), (0.8, 3.5, 0.8), "bs_barb")])]),
        part("cheek_spike_r", pivot=(-2.2, -0.6, -2.6), rot=(-1.4, -0.6, -0.2), boxes=[box((-0.45, -3.5, -0.45), (0.9, 3.5, 0.9), "bs_barb")]),
        *[_spike(f"crown_{i}", (x, -3.4, -2.5 + i * 0.4), (-1.2 + i * 0.05, 0, x * 0.22), 3.5 + (1.2 if x == 0 else 0), 0.9)
          for i, x in enumerate((-1.4, 0, 1.4))],
        *[_spike(f"crown_back_{i}", (x, -2.5, 0), (-1.5, 0, x * 0.3), 2.8, 0.8) for i, x in enumerate((-1.6, 0, 1.6))],
    ])
    head["children"] += [mirror(c) for c in list(head["children"]) if c["name"].endswith("_r")]

    # The torso pitches forward from the hips (the pivot) so the beast prowls low: ribs, a thin waist, a ridge of
    # barbs down the spine, wings at the shoulders and the tail from the pelvis.
    body = part("body", pivot=(0, 9, 3), rot=(LEAN, 0, 0), boxes=[
        box((-3.5, -11, -2.5), (7, 6.5, 5), "bs_ribs"),                    # ribcage
        box((-2, -4.5, -1.6), (4, 3, 3.2), "bs_bone_dark"),               # thin waist
        box((-3, -1.5, -2), (6, 3, 4), "bs_bone_mid"),                    # pelvis
        box((-0.8, -11.5, 2), (1.6, 13, 1.2), "bs_vertebra"),             # spine down the back
        box((-4, -12, -2.2), (8, 2, 4.4), "bs_bone_mid"),                 # shoulder girdle
    ], children=[
        part("neck", pivot=(0, -11.5, 0), rot=(-0.3, 0, 0), boxes=[box((-1, -3, -1.2), (2, 3.5, 2.4), "bs_vertebra")]),
        *[_spike(f"spine_barb_{i}", (0, -10.5 + i * 2.1, 2.8), (-LEAN + 0.35 - i * 0.04, 0, 0), 4.2 - abs(i - 1.5) * 0.45, 1.0)
          for i in range(6)],
        _wing("wing_r", 18, 8, 0.5, 0.25, -10.5), _wing("hindwing_r", 14, 7, 0.95, 0.6, -9),
        _tail(),
    ])
    body["children"] += [mirror(c) for c in list(body["children"]) if c["name"].endswith("_r")]

    arm, leg = _arm(), _leg()
    return {"id": "demon_beast", "tex": [128, 128],
            "parts": [head, part("hat"), body, arm, mirror(arm), leg, mirror(leg)]}


def models():
    return [bone_stalker()]


# ----------------------------------------------------------------------------- materials

def _shade_side(c, side):
    return shade(c, {"top": 1.12, "bottom": 0.7, "left": 0.9, "right": 0.9}.get(side, 1.0))


def bs_material(mat, side, x, y, w, h, pal, rng):
    if not mat.startswith("bs_"):
        return NotImplemented
    p = {k: hexc(v) for k, v in pal.items()}
    t = y / max(1, h - 1)
    if mat in ("bs_bone", "bs_bone_mid", "bs_bone_dark", "bs_skull"):
        base = {"bs_bone": p["bone"], "bs_skull": p["bone"], "bs_bone_mid": p["bone_mid"], "bs_bone_dark": p["bone_dark"]}[mat]
        c = mix(base, p["bone_mid"], rng.uniform(0, 0.35))
        if rng.random() < 0.08:
            c = mix(c, p["bone_dark"], 0.5)                                  # pits and cracks
        if mat == "bs_skull" and side == "front" and h > 3 and (x + y) % 5 == 0:
            c = mix(c, p["shadow"], 0.4)                                      # sunken sockets
        return _shade_side(c, side)
    if mat == "bs_ribs":
        # Ribs curving round the chest, with dark gaps between, and a breastbone down the front.
        if side in ("front", "back", "left", "right"):
            if side == "front" and x in (w // 2 - 1, w // 2):
                return _shade_side(p["bone_mid"], side)
            c = p["bone"] if y % 2 == 0 else mix(p["shadow"], p["bone_dark"], 0.3)
            return _shade_side(shade(c, rng.uniform(0.92, 1.05)), side)
        return _shade_side(p["bone_mid"], side)
    if mat == "bs_vertebra":
        c = p["bone"] if (y + x) % 3 else p["bone_dark"]
        if side in ("left", "right") and y % 3 == 2:
            c = p["shadow"]                                                   # gaps between vertebrae
        return _shade_side(shade(c, rng.uniform(0.93, 1.05)), side)
    if mat == "bs_barb":
        # Barbs and spines: bone at the root fading to dark blood red at the point (y=0 is the tip).
        c = mix(p["red_dark"], p["red"], 0.3 + 0.5 * t)
        if t > 0.8:
            c = mix(c, p["bone_mid"], 0.45)
        if side in ("left", "front") and rng.random() < 0.2:
            c = mix(c, p["red_hi"], 0.4)
        return shade(c, rng.uniform(0.92, 1.06))
    if mat == "bs_talon":
        return shade(mix(p["red"], p["red_dark"], t), rng.uniform(0.9, 1.05))
    if mat == "bs_red":
        c = mix(p["red"], p["red_hi"], 0.4) if (x + y) % 3 == 0 else p["red"]
        return _shade_side(c, side)
    if mat == "bs_fang":
        return mix(p["bone"], p["bone_mid"], t)
    if mat == "bs_mouth":
        return p["mouth"]
    if mat == "bs_eye":
        return glow(p["eye"])
    if mat == "bs_wing":
        # Thin pale membrane split by red struts (one along the leading edge, veins fanning out), torn near the tips.
        if side not in ("left", "right"):
            return p["red_dark"]
        fx = x / max(1, w - 1)
        if x == 0:
            return shade(p["red"], 0.9)                                       # leading-edge strut
        # One long vein fanning from the root toward the trailing edge, and a few short cross-veins.
        vein = abs(x - (0.5 + (1 - t) * (w - 2) * 0.75)) < 0.5 or (y % 6 == 2 and x <= 1 + (1 - t) * (w - 2) * 0.75)
        if vein:
            return mix(p["red"], p["red_dark"], 0.3)
        # Tatters: the membrane is torn away at the outer edge and the tip.
        if (x == w - 1 and (y * 7) % 5 < 2) or (t < 0.12 and x > w * (0.4 + t * 3)) or rng.random() < 0.03:
            return None
        return shade(mix(p["membrane"], p["membrane_dark"], 0.2 + 0.4 * fx), rng.uniform(0.95, 1.04))
    return hexc("ff00ff")
