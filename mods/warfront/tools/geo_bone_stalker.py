"""The Bone Stalker, the demons' war beast, as a high-detail GeckoLib model (docs/art-reference/demon-beast.png).

Run: python3 mods/warfront/tools/geo_bone_stalker.py
Writes assets/warfront/geo/demon_beast.geo.json, animations/demon_beast.animation.json and the textures (with
glowmasks) in textures/entity/geo/<skin>/. Model space: Y up, feet at 0, front -z, pixels; see geo.py.
"""
import math
from pathlib import Path

from geo import (Bone, bone, cube, mirror, scale_tree, export, export_animations, depth, hexc, mix, shade, with_alpha)

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "warfront"
MODEL = "demon_beast"

PALETTES = {
    "demon": dict(bone="ece8df", bone_mid="cdc6b9", bone_dark="8f877b", shadow="2a1a18", red="8e1612", red_dark="420606",
                  red_hi="c4352b", eye="ee1408", membrane="ebe6de", vein="9a2a22", mouth="2a0505"),
    "burning_horde": dict(bone="b9b0a4", bone_mid="958b7e", bone_dark="5e554b", shadow="1a100c", red="b2401a",
                          red_dark="4a1206", red_hi="ff8a3a", eye="ff7a18", membrane="aca396", vein="b04a1a", mouth="1a0604"),
}

LEAN = 22            # the spine rises steeply from the hips to the shoulders (degrees)


# ----------------------------------------------------------------------------- parts

def taper(name, pivot, rot, length, width, mats=("barb_root", "barb", "barb"), curve=0.0, down=False):
    """A spike that narrows to a point in three pieces, along +y (or -y when `down`), bending `curve` degrees at
    each joint. Used for the back spikes, the crest, claws and fangs."""
    parts = [(0.40, 1.0), (0.33, 0.62), (0.27, 0.32)]
    sign = -1 if down else 1
    child = None
    for k in reversed(range(3)):
        f, wf = parts[k]
        L, w = length * f, width * wf
        cubes = [cube((-w / 2, -L if down else 0, -w / 2), (w, L, w), mats[k])]
        piv = pivot if k == 0 else (0, sign * length * parts[k - 1][0], 0)
        child = bone(f"{name}_{k}", piv, rot if k == 0 else (curve, 0, 0), cubes, [child] if child else [])
    return child


def barb(name, pivot, rot, length=3.0, width=0.9):
    return taper(name, pivot, rot, length, width, curve=12)


def fang(name, pivot, rot, length, width, up=False, curve=10):
    return taper(name, pivot, rot, length, width, ("fang_root", "fang", "fang"), curve=(-curve if up else curve), down=not up)


def finger(name, pivot, rot, lengths, width, curl, claw):
    """A long finger hanging down (-y) from the hand, curling forward joint by joint, ending in a red claw."""
    tip = taper(f"{name}_claw", (0, -lengths[-1], 0), (curl + 6, 0, 0), claw, width * 0.85,
                ("talon", "talon", "talon"), curve=14, down=True)
    child = tip
    for k in reversed(range(len(lengths))):
        L, w = lengths[k], width * (1 - 0.12 * k)
        child = bone(f"{name}_{k}", pivot if k == 0 else (0, -lengths[k - 1], 0), rot if k == 0 else (curl, 0, 0),
                     [cube((-w / 2, -L, -w / 2), (w, L, w), "skin"),
                      cube((-w * 0.62, -0.7, -w * 0.62), (w * 1.24, 0.8, w * 1.24), "bone_mid")], [child])
    return child


def toe(name, pivot, yaw, lengths, width, talon):
    """A toe reaching forward (-z), its talon curling down to the ground."""
    tip = taper(f"{name}_talon", (0, 0, -lengths[-1]), (-100, 0, 0), talon, width * 0.85, ("talon", "talon", "talon"), curve=-16)
    child = tip
    for k in reversed(range(len(lengths))):
        L, w = lengths[k], width * (1 - 0.12 * k)
        child = bone(f"{name}_{k}", pivot if k == 0 else (0, 0, -lengths[k - 1]), (6 if k else 0, yaw if k == 0 else 0, 0),
                     [cube((-w / 2, -w / 2, -L), (w, w, L), "skin"),
                      cube((-w * 0.6, -w * 0.6, -0.6), (w * 1.2, w * 1.2, 0.7), "bone_mid")], [child])
    return child


def horn(name, pivot, rot, length, width, bend):
    """A long horn: three pieces and a pointed tip, bending `bend` (x, y, z degrees) at each joint, so it can sweep
    sideways as well as back."""
    seg = length / 4
    tip = taper(f"{name}_tip", (0, seg, 0), bend, seg * 1.5, width * 0.5, ("crest", "crest_tip", "crest_tip"), curve=0)
    child = tip
    for k in reversed(range(3)):
        w = width * (1 - 0.18 * k)
        child = bone(f"{name}_{k}", pivot if k == 0 else (0, seg, 0), rot if k == 0 else bend,
                     [cube((-w / 2, 0, -w / 2), (w, seg + 0.25, w), "crest")], [child])
    return child


def skull():
    """Head after the reference and its two head studies. A skull face: a heavy brow in a V over deep sockets with
    small red eyes, a bridge between them, a nose hole like an upturned heart, jutting cheekbones and a narrow upper
    jaw. Over it a tall ridged dome swept up and back, framed by two long horns rising from the temples and curving
    in; a mane of spikes runs off the back of the dome. The long lower jaw hangs wide open, narrowing to a pointed
    chin with a beard of spikes; both jaws carry rows of tapering fangs, longest at the canines, red at the gums."""
    h = bone("head", (0, 0.2, -8.4), (-6, 0, 0), [
        cube((-2.6, -1.0, -4.8), (5.2, 3.6, 5.0), "skull"),                    # back of the skull
        cube((-2.4, -0.5, -5.1), (4.8, 2.9, 0.5), "skull"),                    # face the sockets sink into
        cube((-0.42, 0.45, -5.55), (0.84, 1.85, 0.7), "bone"),                 # bridge between the sockets
        cube((-0.75, -1.3, -6.1), (1.5, 1.1, 1.5), "skull"),                   # upper jaw, lifted in a snarl
        cube((-0.75, -1.5, -6.05), (1.5, 0.22, 1.35), "gum"),
        cube((-1.8, -1.45, -5.6), (3.6, 0.12, 4.4), "mouth"),                  # roof of the mouth
    ])
    for x in (-1, 1):
        s = "r" if x > 0 else "l"

        def X(x0, w):                                                          # mirror a box across the middle
            return x0 if x > 0 else -(x0 + w)
        h.add(cube((X(0.36, 2.05), 0.5, -5.2), (2.05, 1.55, 0.15), "socket", rot=(0, 0, x * 16)),
              cube((X(0.9, 0.52), 0.95, -5.25), (0.52, 0.34, 0.06), "eye", rot=(0, 0, x * 18)),
              cube((X(0.0, 0.55), -0.32, -5.2), (0.55, 0.75, 0.12), "socket", rot=(0, 0, -x * 14)),  # nose hole
              cube((X(0.05, 2.95), 1.85, -5.85), (2.95, 0.95, 1.75), "brow", rot=(0, 0, x * 21)),    # the brow, in a V
              cube((X(0.45, 2.5), -0.4, -5.6), (2.5, 0.85, 2.2), "bone", rot=(0, -x * 12, -x * 8)),  # cheekbone
              cube((X(2.45, 0.6), -0.3, -4.4), (0.6, 0.75, 3.6), "bone", rot=(0, -x * 6, 0)),       # arch back to the ear
              cube((X(2.25, 0.55), 0.35, -5.5), (0.55, 1.95, 1.2), "bone", rot=(0, -x * 18, -x * 8)),  # outer rim
              cube((X(0.6, 0.85), -1.75, -6.0), (0.85, 1.5, 1.6), "skull"),                         # low over the canines
              cube((X(0.6, 0.85), -1.95, -5.95), (0.85, 0.22, 1.45), "gum"),
              cube((X(0.9, 1.3), -1.7, -5.65), (1.3, 1.4, 3.6), "skull", rot=(0, x * 10, 0)),      # sides of the upper jaw
              cube((X(0.95, 1.2), -1.92, -5.6), (1.2, 0.22, 3.4), "gum", rot=(0, x * 10, 0)))
        for k, (xx, y, z, L, w) in enumerate(((0.3, -1.45, -5.85, 1.5, 0.42), (0.82, -1.9, -5.75, 2.3, 0.52),
                                              (1.32, -1.9, -5.45, 4.2, 0.82), (1.68, -1.9, -4.8, 2.5, 0.56),
                                              (1.88, -1.9, -4.0, 1.9, 0.48), (2.0, -1.9, -3.2, 1.4, 0.42))):
            h.add(fang(f"fang_up_{s}{k}", (x * xx, y, z), (-4, 0, -x * 5), L, w, curve=4))
        h.add(horn(f"horn_{s}", (x * 2.7, 0.9, -3.0), (20, 0, -x * 20), 10.0, 1.25, (6, 0, x * 12)))
        for k in range(3):                                                    # spikes fanning out from the cheeks
            h.add(taper(f"cheek_spike_{s}{k}", (x * 2.9, 0.3 - k * 0.5, -3.8 + k * 1.1), (30 + k * 10, 0, -x * (58 + k * 34)),
                        5.2 - k * 0.8, 0.95, ("crest", "crest", "crest_tip"), curve=-7))

    dome = bone("dome", (0, 2.3, -4.6), (26, 0, 0), [
        cube((-2.6, 0, 0), (5.2, 1.8, 5.4), "dome"),
        cube((-2.35, 1.6, 0.25), (4.7, 1.8, 5.0), "dome"),
        cube((-2.0, 3.2, 0.6), (4.0, 1.7, 4.4), "dome"),
        cube((-1.5, 4.7, 1.0), (3.0, 1.4, 3.6), "dome"),
        cube((-0.9, 5.9, 1.5), (1.8, 1.0, 2.6), "dome"),
        cube((-0.4, 0.3, -0.25), (0.8, 6.4, 0.9), "bone_mid", rot=(14, 0, 0), pivot=(0, 0.3, 0)),  # ridge up the middle
    ])
    for x in (-1, 1):
        s = "r" if x > 0 else "l"
        dome.add(cube(((1.3 if x > 0 else -1.85), 0.4, -0.1), (0.55, 4.8, 0.6), "bone_mid", rot=(12, 0, -x * 12),
                      pivot=(x * 1.55, 0.4, 0)))
        for k in range(4):                                                    # the mane off the back of the dome
            dome.add(taper(f"mane_{s}{k}", (x * (1.0 + 0.3 * k), 5.4 - 1.1 * k, 2.2 + 0.8 * k),
                           (42 + k * 9, 0, -x * (22 + k * 11)), 5.4 - 0.4 * k, 1.05, ("crest", "crest", "crest_tip"), curve=9))
        for k in range(3):
            dome.add(taper(f"mane_low_{s}{k}", (x * 2.3, 2.2 - 0.7 * k, 3.0 + 1.0 * k), (60 + k * 10, 0, -x * 55),
                           4.4 - 0.5 * k, 0.95, ("crest", "crest", "crest_tip"), curve=10))
    for k in range(3):
        dome.add(taper(f"mane_mid_{k}", (0, 6.6, 2.0 + 1.0 * k), (30 + k * 14, 0, 0), (5.6, 6.4, 5.8)[k], 1.2,
                       ("crest", "crest", "crest_tip"), curve=9))
    h.add(dome)

    jaw = bone("jaw", (0, -1.3, -1.4), (-46, 0, 0), [
        cube((-1.6, -0.95, -5.8), (3.2, 0.5, 5.4), "mouth"),                  # the dark inside of the mouth
        cube((-1.5, -1.5, -6.95), (3.0, 1.45, 1.4), "bone_mid"),              # front of the jaw
        cube((-1.5, -0.1, -6.85), (3.0, 0.22, 1.2), "gum"),
        cube((-0.75, -1.9, -7.95), (1.5, 1.5, 1.4), "bone_mid", rot=(-20, 0, 0)),  # pointed chin
    ], [taper("chin_spike_m", (0, -2.4, -8.0), (-152, 0, 0), 3.4, 0.85, ("crest", "crest", "crest_tip"), curve=-8),
        taper("chin_spike_l", (-0.5, -2.0, -7.6), (-150, 0, -22), 2.4, 0.65, ("crest", "crest", "crest_tip"), curve=-8),
        taper("chin_spike_r", (0.5, -2.0, -7.6), (-150, 0, 22), 2.4, 0.65, ("crest", "crest", "crest_tip"), curve=-8)])
    for x in (-1, 1):
        s = "r" if x > 0 else "l"
        jaw.add(cube(((1.1 if x > 0 else -2.1), -1.4, -6.2), (1.0, 1.4, 6.6), "bone_mid", rot=(0, x * 2, 0)),  # jawbone
                cube(((1.15 if x > 0 else -2.05), -0.1, -6.0), (0.9, 0.22, 5.0), "gum", rot=(0, x * 2, 0)))
        for k, (xx, z, L, w) in enumerate(((0.4, -6.55, 1.4, 0.42), (1.05, -6.3, 3.1, 0.66), (1.5, -5.4, 2.1, 0.5),
                                           (1.6, -4.5, 1.7, 0.45), (1.65, -3.6, 1.3, 0.4))):
            jaw.add(fang(f"fang_dn_{s}{k}", (x * xx, 0.05, z), (30, 0, x * 5), L, w, up=True, curve=-4))
        for a in (12, 34, 56):                                               # flesh closing the mouth corners
            jaw.add(cube(((1.85 if x > 0 else -2.1), -0.7, -4.0), (0.25, 1.4, 4.0), "sinew", rot=(a, 0, 0), pivot=(0, 0, 0)))
        for k in range(2):
            jaw.add(taper(f"jaw_spike_{s}{k}", (x * 2.0, -0.8, -2.4 - k * 1.8), (20 + k * 10, 0, -x * (115 + k * 15)),
                          3.4 - k * 0.6, 0.75, ("crest", "crest", "crest_tip"), curve=-8))
    h.add(jaw)
    scale_tree(h, 1.12)
    h.pivot = [0, 0.2, -8.4]
    return h


def arm():
    """A mantis-like arm: the upper arm rises up and back to a high elbow that bristles with long red spikes; a long
    forearm comes down in front to a big hand with four long, spread fingers and red claws."""
    hand = bone("hand_r", (0, -21.0, 0), (18, 0, 0), [
        cube((-2.1, -3.8, -1.3), (4.2, 3.8, 2.6), "skin"),
        cube((-2.2, -4.2, -1.1), (4.4, 0.9, 2.2), "bone_mid"),                # knuckles
    ], [finger(f"finger_r{k}", (x, -4.0, 0), (4, 0, -x * 11), (3.6, 2.8, 2.0), 0.85, 12, 3.6)
        for k, x in enumerate((-1.6, -0.55, 0.55, 1.6))]
       + [finger("thumb_r", (-2.2, -1.8, -0.4), (10, 0, 40), (2.2, 1.6), 0.8, 12, 2.8)])
    fore = bone("forearm_r", (0, 12.5, 0), (-26, 0, 22), [
        cube((-1.5, -1.5, -1.5), (3.0, 3.0, 3.0), "skin"),                    # elbow
        cube((-1.35, -11.0, -1.3), (2.7, 11.0, 2.6), "skin"),
        cube((-1.0, -21.0, -1.0), (2.0, 10.4, 2.0), "skin"),
        cube((-0.3, -19.0, 0.6), (0.6, 17.0, 0.5), "bone_mid"),              # ulna ridge down the back
    ], [hand] + [taper(f"forearm_spike_r{k}", (0, -2.5 - k * 2.6, 0.9), (118 + k * 6, 0, 0), 3.8 - k * 0.7, 0.8, curve=10)
                 for k in range(2)])
    elbow_spikes = [taper(f"elbow_spike_r{k}", (0, 12.1, 0.6), (rx, 0, rz), L, 1.15, curve=8)
                    for k, (rx, rz, L) in enumerate(((18, -18, 8.0), (34, 8, 8.6), (52, -8, 7.6), (70, 16, 6.6), (90, -2, 5.4)))]
    return bone("arm_r", (4.6, -0.6, -16.4), (38, 0, -28), [
        cube((-2.0, -1.8, -2.0), (4.0, 3.6, 4.0), "skin"),                    # shoulder
        cube((-1.2, 0, -1.2), (2.4, 12.5, 2.4), "skin"),                      # upper arm
        cube((-1.5, 2.0, -1.5), (3.0, 4.6, 2.3), "skin"),                     # sinew bulge
    ], [fore] + elbow_spikes)


def leg():
    """A long hind leg, digitigrade: a muscled haunch, thigh forward, shin back, a high hock with a big red spur, a long
    ankle down to a foot of long taloned toes."""
    foot = bone("foot_r", (0, -7.6, 0), (-30, 0, 0), [cube((-1.7, -1.0, -2.2), (3.4, 1.4, 3.2), "skin")],
                [toe(f"toe_r{k}", (x, -0.4, -2.0), -x * 16, (4.4, 3.4), 0.9, 4.4) for k, x in enumerate((-1.1, 0, 1.1))]
                + [toe("dewclaw_r", (0, -0.2, 0.8), 180, (1.4,), 0.8, 2.6)])
    ankle = bone("ankle_r", (0, -8.0, 0), (84, 0, -6), [
        cube((-1.4, -1.2, -1.4), (2.8, 2.6, 2.8), "bone_mid"),                # hock
        cube((-0.9, -7.6, -0.9), (1.8, 7.6, 1.8), "skin"),
        cube((-0.25, -7.0, 0.7), (0.5, 6.4, 0.5), "bone_mid"),               # tendon down the back
    ], [foot, taper("hock_spur_r", (0, 0.4, 1.0), (70, 0, 0), 7.0, 1.6, curve=16)])
    shin = bone("shin_r", (0, -8.6, 0), (-92, 0, 0), [
        cube((-1.6, -1.4, -1.6), (3.2, 3.0, 3.2), "skin"),                    # knee
        cube((-1.15, -8.0, -1.15), (2.3, 8.0, 2.3), "skin"),
    ], [ankle] + [taper(f"knee_spike_r{k}", (0, -0.6 - k * 1.8, 1.2), (96 + k * 12, 0, 0), 3.8 - k * 0.6, 1.0, curve=10)
                  for k in range(3)])
    return bone("leg_r", (3.6, -1.6, 0.4), (13, 0, 14), [
        cube((-2.4, -5.0, -2.8), (4.8, 6.6, 5.4), "skin"),                    # haunch
        cube((-1.7, -8.6, -1.8), (3.4, 8.6, 3.4), "skin"),                    # thigh, thick with muscle
    ], [shin])


TAIL_CURL = [-2, -1, -1, -1, -1, -2, -6, -16, -26, -30, -28, -22, -14, -8, -5, -3, -2, -2, -2, -2, -2, -3, -4, -5, -6]


def tail():
    """A long thick tail with big red spikes along the top and smaller ones beneath. It runs back, rises and arches
    over the body, turning red toward the end, and ends in a hooked red stinger above the head."""
    n = len(TAIL_CURL)
    tip = bone("stinger", (0, 0, 2.3), (-20, 0, 0), [
        cube((-1.5, -1.5, 0), (3.0, 3.0, 3.2), "venom"),
    ], [taper("stinger_blade", (0, 0, 3.2), (-80, 0, 0), 9.0, 1.6, ("tail_red", "barb", "barb"), curve=-24)]
       + [taper(f"stinger_hook_{k}", (0, 1.4, 1.0 + k * 1.2), (-20 - k * 10, 0, 0), 2.0, 0.7, curve=-12) for k in range(2)])
    child = tip
    for i in reversed(range(n)):
        w = 4.0 - i * 0.11
        L = 3.6 - i * 0.055
        red = i >= n - 9
        mat = "tail_red" if red else "skin"
        seg = bone(f"tail_{i}", (0, 0.6, 2.6) if i == 0 else (0, 0, 3.6 - (i - 1) * 0.055),
                   (TAIL_CURL[i] - (LEAN if i == 0 else 0), 0, 0), [
                       cube((-w / 2, -w * 0.45, 0), (w, w * 0.9, L * 0.82), mat),
                       cube((-w * 0.58, -w * 0.52, L * 0.74), (w * 1.16, w * 1.04, 0.55), "bone_mid" if not red else "tail_red"),
                   ], [child,
                       taper(f"tail_spike_{i}", (0, w * 0.4, L * 0.35), (50, 0, 0), max(1.8, 5.6 - i * 0.16), w * 0.55, curve=10),
                       taper(f"tail_under_{i}", (0, -w * 0.4, L * 0.4), (128, 0, 0), max(1.0, 3.2 - i * 0.12), w * 0.36, curve=-8)])
        child = seg
    return child


def wing(name, z, y, length, width, flare, sweep):
    """A long, narrow dragonfly wing: see-through veined membrane with a raised red leading strut and two veins."""
    veins = [cube((-0.14, 0, 0.6), (0.28, length * f, 0.28), "strut", rot=(a, 0, 0), pivot=(0, 0, 0))
             for f, a in ((0.86, 3.0), (0.74, 6.0))]
    return bone(name.replace("_r", "_root_r"), (1.8, y, z), (-LEAN, 0, 0), [], [
        bone(name, (0, 0, 0), (sweep, 0, -flare), [
            cube((0, 0, 0), (0, length, width), "membrane"),
            cube((-0.28, 0, 0.1), (0.55, length * 0.9, 0.55), "strut"),
            cube((-0.6, -0.4, -0.6), (1.2, 2.4, 1.2), "barb_root"),
        ] + veins)])


def body():
    b = bone("body", (0, 21.0, 6.0), (LEAN, 0, 0), [
        cube((-3.4, -2.2, -2.0), (6.8, 4.4, 4.8), "skin"),                    # pelvis
        cube((-2.5, -2.4, -8.6), (5.0, 4.2, 6.8), "belly"),                   # gaunt waist
        cube((-4.6, -3.4, -19.4), (9.2, 7.4, 11.2), "chest"),                 # ribcage under skin, narrowing below
        cube((-3.5, -6.2, -18.8), (7.0, 3.0, 9.6), "chest"),
        cube((-1.2, -6.6, -19.0), (2.4, 1.2, 8.5), "bone_mid", rot=(-8, 0, 0)),  # breastbone keel
        cube((-5.0, 2.6, -19.6), (10.0, 2.2, 6.4), "skin"),                   # shoulders
        cube((-3.0, 3.6, -20.0), (6.0, 2.4, 4.0), "skin"),                    # base of the neck
    ])
    for x in (-1, 1):
        b.add(cube((x * 3.0 - (1.3 if x > 0 else 0), -1.4, -1.8), (1.3, 3.0, 4.0), "bone_mid", rot=(0, 0, -x * 12)))  # hip bones
        for i in range(6):                                                    # ribs standing out of the hide
            L = 7.0 - i * 0.45
            b.add(cube((x * 4.75 - 0.3, 1.8 - i * 1.25, -15.6 + i * 0.5), (0.6, 0.55, L), "rib", rot=(-28, 0, 0)))
    for k in range(11):                                                       # knobs of the spine
        b.add(cube((-0.7, 2.0, -0.6 - k * 1.8), (1.4, 1.2, 1.2), "vertebra"))
    # The mane of red spikes down the back: longest over the shoulders, shorter toward the hips, two side rows.
    for k in range(14):
        z = -19.6 + k * 1.35
        L = 10.0 - k * 0.48
        y = 4.6 if z < -13 else 3.0
        b.add(taper(f"back_spike_{k}", (0, y, z), (10 + k * 2.5, 0, 0), L, 1.35, curve=9))
    for x in (-1, 1):
        for k in range(6):
            b.add(taper(f"shoulder_spike_{'r' if x > 0 else 'l'}{k}", (x * (2.4 + k * 0.1), 4.2, -19.4 + k * 1.5),
                        (14 + k * 4, 0, -x * (22 + k * 3)), 8.4 - k * 0.7, 1.15, curve=10))
    neck = bone("neck", (0, 2.4, -20.2), (-30, 0, 0), [
        cube((-2.2, -2.4, -4.5), (4.4, 4.6, 5.0), "skin"),
        cube((-1.8, -2.0, -8.6), (3.6, 3.9, 4.6), "skin"),
        cube((-1.0, -2.6, -8.0), (2.0, 1.0, 7.6), "skin_dark"),               # sinew under the throat
    ], [skull()] + [taper(f"neck_spike_{k}", (0, 1.8, -1.6 - k * 2.4), (30, 0, 0), 3.6 - k * 0.5, 0.9, curve=10) for k in range(3)])
    b.add(neck)
    a, l = arm(), leg()
    b.add(a, mirror(a), l, mirror(l), tail())
    for wb in (wing("wing_r", -15.0, 4.2, 36, 6.0, 24, 30), wing("hindwing_r", -12.5, 3.8, 31, 5.2, 40, 60)):
        b.add(wb, mirror(wb))
    return b


def model():
    return bone("root", (0, 0, 0), (0, 0, 0), [], [body()])


# ----------------------------------------------------------------------------- painting

def p_bone(base_key):
    def paint(ctx):
        c = mix(ctx.col(base_key), ctx.col("bone_mid"), ctx.rng.uniform(0, 0.3))
        if ctx.rng.random() < 0.05:
            c = mix(c, ctx.col("bone_dark"), 0.55)                            # pores
        if ctx.wall and ctx.h > 6 and (ctx.y + ctx.x * 3) % 11 == 0 and ctx.rng.random() < 0.5:
            c = mix(c, ctx.col("bone_dark"), 0.35)                            # hairline cracks
        return depth(ctx, c)
    return paint


def p_skin(ctx):
    # Pale hide stretched over bone: mottled, with grey sinew streaks running along the limb and deep shading.
    c = mix(ctx.col("bone"), ctx.col("bone_mid"), ctx.rng.uniform(0.15, 0.7))
    if ctx.wall and (ctx.x * 5 + (ctx.y // 5) * 3) % 9 == 0:
        c = mix(c, ctx.col("bone_dark"), 0.45)
    shadowed = ctx.v if ctx.wall else (0.9 if ctx.side == "down" else 0.1)
    if ctx.rng.random() < 0.08 + 0.22 * shadowed:                           # ink stipple, thicker in the shadows
        c = mix(c, ctx.col("bone_dark"), ctx.rng.uniform(0.45, 0.8))
    return depth(ctx, c, bevel=0.22, falloff=0.4)


def p_skin_dark(ctx):
    return depth(ctx, mix(ctx.col("bone_mid"), ctx.col("bone_dark"), 0.3 + ctx.rng.uniform(0, 0.3)), falloff=0.2)


def p_chest(ctx):
    # The ribcage under the hide: slanting grooves between the ribs on the flanks, a keel down the front.
    c = mix(ctx.col("bone"), ctx.col("bone_mid"), ctx.rng.uniform(0.05, 0.35))
    if ctx.side in ("east", "west") and (ctx.y + ctx.x // 2) % 5 == 0:
        c = mix(c, ctx.col("bone_dark"), 0.5)
    elif ctx.side == "down" and (ctx.y // 3) % 2 == 0:
        c = mix(c, ctx.col("bone_dark"), 0.25)
    return depth(ctx, c, bevel=0.2, falloff=0.28)


def p_belly(ctx):
    c = mix(ctx.col("bone_mid"), ctx.col("bone"), ctx.rng.uniform(0.2, 0.6))
    if ctx.y % 3 == 0:
        c = mix(c, ctx.col("bone_dark"), 0.35)                                # sunken, segmented belly
    return depth(ctx, c, falloff=0.3)


def p_crest(ctx):
    # The pale spikes of the mane: bone at the root, greying toward the point.
    t = ctx.v if ctx.wall else 0.5
    return depth(ctx, mix(ctx.col("bone_dark"), ctx.col("bone"), 0.15 + 0.6 * t), bevel=0.14, falloff=0.0)


def p_crest_tip(ctx):
    t = ctx.v if ctx.wall else 0.5
    return depth(ctx, mix(ctx.col("shadow"), ctx.col("bone_dark"), 0.2 + 0.6 * t), bevel=0.1, falloff=0.0)


def p_tail_red(ctx):
    c = mix(ctx.col("red"), ctx.col("red_dark"), ctx.rng.uniform(0, 0.45))
    if ctx.wall and ctx.y == 0:
        c = mix(c, ctx.col("red_hi"), 0.4)
    return depth(ctx, c, bevel=0.18, falloff=0.2)


def p_skull(ctx):
    # Skull bone in ink: pale, stippled, darker into the hollows at the bottom of each face.
    c = mix(ctx.col("bone"), ctx.col("bone_mid"), ctx.rng.uniform(0, 0.3) + ctx.v * 0.25)
    if ctx.side == "up" and abs(ctx.u - 0.5) < 0.08:
        c = mix(c, ctx.col("bone_dark"), 0.35)                                # suture down the crown
    if ctx.rng.random() < 0.07 + 0.2 * (ctx.v if ctx.wall else 0.5 if ctx.side == "down" else 0):
        c = mix(c, ctx.col("bone_dark"), ctx.rng.uniform(0.4, 0.75))
    return depth(ctx, c, bevel=0.22, falloff=0.3)


def p_dome(ctx):
    # The tall dome over the skull: ridges and grooves running up it, ink stipple, darker low on the sides.
    c = mix(ctx.col("bone"), ctx.col("bone_mid"), ctx.rng.uniform(0.05, 0.4))
    wobble = int(round(1.2 * math.sin(ctx.y * 0.55 + ctx.x)))
    if ctx.wall and (ctx.x + wobble) % 4 == 0:
        c = mix(c, ctx.col("bone_dark"), 0.6)                                  # groove
    elif ctx.wall and (ctx.x + wobble) % 4 == 1:
        c = mix(c, ctx.col("bone"), 0.6)                                       # lit ridge beside it
    elif ctx.side == "up" and (ctx.x + wobble) % 4 == 0:
        c = mix(c, ctx.col("bone_dark"), 0.45)
    if ctx.rng.random() < 0.08 + 0.2 * (ctx.v if ctx.wall else 0):
        c = mix(c, ctx.col("bone_dark"), ctx.rng.uniform(0.4, 0.7))
    return depth(ctx, c, bevel=0.2, falloff=0.32)


def p_brow(ctx):
    # The heavy brow: pale bone on top and in front, a deep shadow underneath falling into the sockets.
    if ctx.side == "down":
        return shade(ctx.col("shadow"), ctx.rng.uniform(1.1, 1.5))
    c = mix(ctx.col("bone"), ctx.col("bone_mid"), ctx.rng.uniform(0, 0.3))
    if ctx.wall and ctx.y >= ctx.h - 2:
        c = mix(c, ctx.col("bone_dark"), 0.55)
    return depth(ctx, c, bevel=0.25, falloff=0.2)


def p_gum(ctx):
    # Gums: dark, wet red, nearly the black of the mouth, so the teeth stand out instead of a lip line.
    return shade(mix(ctx.col("mouth"), ctx.col("red_dark"), ctx.rng.uniform(0.35, 0.85)), ctx.rng.uniform(0.85, 1.1))


def p_sinew(ctx):
    # The flesh at the corners of the mouth: dark and wet, stretched into pale strands across the gape.
    c = mix(ctx.col("mouth"), ctx.col("red_dark"), ctx.rng.uniform(0.3, 0.8))
    if ctx.wall and (ctx.x + ctx.y // 3) % 3 == 0:
        c = mix(c, ctx.col("bone_mid"), 0.3 + ctx.rng.uniform(0, 0.15))
    return shade(c, ctx.rng.uniform(0.85, 1.05))


def p_fang_root(ctx):
    # Teeth are bloodied at the gum: the root end of the first piece runs from dark red into ivory.
    down = ctx.cube.origin[1] < 0
    root = (1 - ctx.v) if down else ctx.v
    c = mix(ctx.col("bone"), ctx.col("bone_mid"), 0.25 + ctx.rng.uniform(0, 0.15))
    c = mix(c, ctx.col("red_dark"), max(0.0, root - 0.45) / 0.55 * 0.85) if ctx.wall else ctx.col("red_dark")
    return depth(ctx, c, bevel=0.1, falloff=0.0)


def p_vertebra(ctx):
    c = ctx.col("bone")
    if ctx.wall and (ctx.y in (0, ctx.h - 1) or ctx.x in (0, ctx.w - 1)):
        c = mix(c, ctx.col("bone_dark"), 0.4)
    return depth(ctx, mix(c, ctx.col("bone_mid"), ctx.rng.uniform(0, 0.25)))


def p_rib(ctx):
    c = mix(ctx.col("bone"), ctx.col("bone_mid"), 0.15 + ctx.rng.uniform(0, 0.2))
    return depth(ctx, c, bevel=0.22)


def p_barb(ctx):
    # Spikes point along +y: the top of each wall (v = 0) is the point, dark blood red; the root shades to bone.
    t = ctx.v if ctx.wall else 0.5
    c = mix(ctx.col("red_dark"), ctx.col("red"), 0.35 + 0.65 * t)
    if ctx.wall and ctx.x == 0:
        c = mix(c, ctx.col("red_hi"), 0.45)                                   # a lit edge
    return depth(ctx, c, bevel=0.12, falloff=0.0)


def p_barb_root(ctx):
    c = mix(ctx.col("bone_mid"), ctx.col("red"), 0.15 + 0.55 * (1 - ctx.v if ctx.wall else 0.5))
    return depth(ctx, c)


def p_talon(ctx):
    return depth(ctx, mix(ctx.col("red"), ctx.col("red_dark"), ctx.u if ctx.wall else 0.5), falloff=0.0)


def p_fang(ctx):
    c = mix(ctx.col("bone"), ctx.col("bone_mid"), 0.15 + ctx.rng.uniform(0, 0.25))
    if ctx.wall and ctx.x == ctx.w - 1:
        c = mix(c, ctx.col("bone_dark"), 0.45)                                # shaded edge so each tooth reads
    return depth(ctx, c, bevel=0.1, falloff=0.0)


def p_venom(ctx):
    c = ctx.col("red")
    if ctx.wall and ctx.rng.random() < 0.12:
        c = mix(c, ctx.col("red_hi"), 0.6)                                    # wet sheen
    if ctx.wall and ctx.y == 1 and 0 < ctx.x < 3:
        c = mix(c, hexc("ffffff"), 0.35)                                      # a highlight
    return depth(ctx, c)


def p_strut(ctx):
    return depth(ctx, mix(ctx.col("red"), ctx.col("red_dark"), ctx.rng.uniform(0, 0.4)), bevel=0.1, falloff=0.05)


def p_membrane(ctx):
    # See-through dragonfly membrane: a network of fine veins (long veins down the wing, cross veins between them
    # making cells), clearer toward the tip, a little torn at the trailing edge.
    if ctx.side not in ("east", "west"):
        return None
    t = 1 - ctx.v                                                             # 0 at the root, 1 at the tip
    half = 0.5 * (0.4 + 0.6 * min(1.0, t / 0.3))
    if t > 0.72:
        half *= math.sqrt(max(0.0, 1 - ((t - 0.72) / 0.28) ** 2))
    if abs(ctx.u - 0.5) > half or ctx.rng.random() < 0.006:
        return None
    if abs(ctx.u - 0.5) > half - 0.6 / ctx.w:
        return with_alpha(mix(ctx.col("vein"), ctx.col("bone_dark"), 0.3), 200)   # the rim
    band = int((ctx.u - 0.5 + half) / (2 * half) * 4)
    long_vein = any(abs(ctx.u - (0.5 - half + 2 * half * f)) < 0.5 / ctx.w for f in (0.25, 0.5, 0.75))
    cross = (ctx.y * 5 + band * 13) % 17 == 0
    if long_vein or cross:
        return with_alpha(mix(ctx.col("vein"), ctx.col("bone_dark"), 0.4), 170)
    c = mix(ctx.col("membrane"), ctx.col("vein"), 0.02 + ctx.rng.uniform(0, 0.04))
    return with_alpha(shade(c, 1.0 + 0.05 * t), round(45 + 60 * ctx.v))


def p_socket(ctx):
    return shade(ctx.col("shadow"), 0.6 if 0 < ctx.x < ctx.w - 1 and 0 < ctx.y < ctx.h - 1 else 0.9)


def p_eye(ctx):
    c = ctx.col("eye")
    return ("glow", c)


def p_mouth(ctx):
    return shade(ctx.col("mouth"), ctx.rng.uniform(0.85, 1.1))


def p_innards(ctx):
    # The dark hollow seen between the ribs: near black, with faint rib shadows across it.
    f = 0.55 if ctx.wall and ctx.x % 4 == 0 else 0.8
    return shade(ctx.col("shadow"), f * ctx.rng.uniform(0.85, 1.1))


PAINTERS = {"skin": p_skin, "skin_dark": p_skin_dark, "chest": p_chest, "belly": p_belly, "crest": p_crest,
            "crest_tip": p_crest_tip, "tail_red": p_tail_red, "bone": p_bone("bone"), "bone_mid": p_bone("bone_mid"), "bone_dark": p_bone("bone_dark"), "skull": p_skull, "dome": p_dome, "brow": p_brow, "gum": p_gum, "sinew": p_sinew, "fang_root": p_fang_root,
            "vertebra": p_vertebra, "rib": p_rib, "barb": p_barb, "barb_root": p_barb_root, "talon": p_talon,
            "fang": p_fang, "venom": p_venom, "strut": p_strut, "membrane": p_membrane, "socket": p_socket,
            "eye": p_eye, "mouth": p_mouth, "innards": p_innards}


# ----------------------------------------------------------------------------- animations

def _w(t, period, phase=0.0):
    return math.sin(2 * math.pi * (t / period + phase))


def idle(t):
    p = 3.2
    out = {"body": {"pos": (0, 0.3 * _w(t, p), 0)},
           "neck": {"rot": (3 * _w(t, p, 0.1), 5 * _w(t, p * 2), 0)},
           "head": {"rot": (2 * _w(t, p, 0.3), 0, 0)},
           "jaw": {"rot": (-3 - 3 * _w(t, p / 2), 0, 0)},
           "arm_r": {"rot": (2 * _w(t, p), 0, 0)}, "arm_l": {"rot": (2 * _w(t, p, 0.5), 0, 0)},
           "wing_r": {"rot": (0, 0, -3 * _w(t, p))}, "wing_l": {"rot": (0, 0, 3 * _w(t, p))},
           "hindwing_r": {"rot": (0, 0, -2 * _w(t, p, 0.2))}, "hindwing_l": {"rot": (0, 0, 2 * _w(t, p, 0.2))}}
    for i in range(len(TAIL_CURL)):
        out[f"tail_{i}"] = {"rot": (1.0 * _w(t, p, i * 0.05), 2.5 * _w(t, p, i * 0.06), 0)}
    return out


def walk(t):
    p = 1.1
    s, o = _w(t, p), _w(t, p, 0.5)
    lift_a, lift_b = max(0.0, _w(t, p, 0.25)), max(0.0, _w(t, p, 0.75))
    out = {"body": {"pos": (0, 0.6 * abs(_w(t, p / 2)), 0)},
           # Diagonal pairs: front right with hind left. The arms reach forward from the shoulder and lift the hand.
           "arm_r": {"rot": (-14 * s, 0, 0)}, "forearm_r": {"rot": (-16 * lift_a, 0, 0)},
           "arm_l": {"rot": (-14 * o, 0, 0)}, "forearm_l": {"rot": (-16 * lift_b, 0, 0)},
           "leg_l": {"rot": (-22 * s, 0, 0)}, "shin_l": {"rot": (18 * lift_a, 0, 0)},
           "leg_r": {"rot": (-22 * o, 0, 0)}, "shin_r": {"rot": (18 * lift_b, 0, 0)},
           "neck": {"rot": (4 * _w(t, p / 2, 0.25), 0, 0)},
           "jaw": {"rot": (-4, 0, 0)}}
    for i in range(len(TAIL_CURL)):
        out[f"tail_{i}"] = {"rot": (0, 3.5 * _w(t, p, i * 0.06), 0)}
    return out


def attack(t):
    L = 0.8
    k = math.sin(math.pi * min(1.0, t / L))                                  # 0 -> 1 -> 0
    strike = math.sin(math.pi * min(1.0, max(0.0, (t - 0.15) / 0.5)))
    out = {"body": {"pos": (0, -0.3 * k, -2.0 * strike), "rot": (-4 * strike, 0, 0)},
           "neck": {"rot": (-8 * k, 0, 0)}, "jaw": {"rot": (-14 * k, 0, 0)},
           "wing_r": {"rot": (0, 0, -14 * k + 5 * _w(t, 0.2))}, "wing_l": {"rot": (0, 0, 14 * k - 5 * _w(t, 0.2))},
           "hindwing_r": {"rot": (0, 0, -10 * k + 5 * _w(t, 0.2, 0.5))}, "hindwing_l": {"rot": (0, 0, 10 * k - 5 * _w(t, 0.2, 0.5))},
           # Both arms rake forward while the tail drives the stinger down over the head.
           "arm_r": {"rot": (-24 * k, 0, 0)}, "arm_l": {"rot": (-24 * k, 0, 0)},
           "forearm_r": {"rot": (-30 * strike, 0, 0)}, "forearm_l": {"rot": (-30 * strike, 0, 0)},
           "stinger": {"rot": (-30 * strike, 0, 0)}}
    for i in range(len(TAIL_CURL)):
        out[f"tail_{i}"] = {"rot": ((-6 if i > 6 else 2) * strike, 0, 0)}
    return out


ANIMS = {"idle": dict(length=3.2, loop=True, fn=idle), "walk": dict(length=1.0, loop=True, fn=walk),
         "attack": dict(length=0.8, loop=False, fn=attack)}


if __name__ == "__main__":
    export(MODEL, model(), ASSETS / "geo" / f"{MODEL}.geo.json", PAINTERS, PALETTES, ASSETS / "textures" / "entity" / "geo")
    export_animations(MODEL, ANIMS, ASSETS / "animations" / f"{MODEL}.animation.json")
    print("wrote", MODEL)
