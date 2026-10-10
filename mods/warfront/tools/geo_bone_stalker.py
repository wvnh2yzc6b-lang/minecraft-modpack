"""The Bone Stalker, the demons' war beast, as a high-detail GeckoLib model (docs/art-reference/demon-beast.png).

Run: python3 mods/warfront/tools/geo_bone_stalker.py
Writes assets/warfront/geo/demon_beast.geo.json, animations/demon_beast.animation.json and the textures (with
glowmasks) in textures/entity/geo/<skin>/. Model space: Y up, feet at 0, front -z, pixels; see geo.py.
"""
import math
from pathlib import Path

from geo import (Bone, bone, cube, mirror, export, export_animations, depth, hexc, mix, shade, with_alpha)

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "warfront"
MODEL = "demon_beast"

PALETTES = {
    "demon": dict(bone="ece8df", bone_mid="cdc6b9", bone_dark="8f877b", shadow="2a1a18", red="8e1612", red_dark="420606",
                  red_hi="c4352b", eye="ff3a22", membrane="ebe6de", vein="9a2a22", mouth="2a0505"),
    "burning_horde": dict(bone="b9b0a4", bone_mid="958b7e", bone_dark="5e554b", shadow="1a100c", red="b2401a",
                          red_dark="4a1206", red_hi="ff8a3a", eye="ffb040", membrane="aca396", vein="b04a1a", mouth="1a0604"),
}

LEAN = 13            # the spine rises toward the shoulders (degrees)


# ----------------------------------------------------------------------------- parts

def barb(name, pivot, rot, length=3.0, width=0.9):
    """A curved spike: a bone-coloured root and a hooked red point, pointing along +y."""
    return bone(name, pivot, rot, [cube((-width / 2, 0, -width / 2), (width, length * 0.55, width), "barb_root")], [
        bone(name + "_tip", (0, length * 0.55, 0), (16, 0, 0),
             [cube((-width * 0.35, 0, -width * 0.35), (width * 0.7, length * 0.5, width * 0.7), "barb")])])


def digit(name, pivot, yaw, lengths=(1.6, 1.3), curl=(8, 18), talon=2.0, w=0.7):
    """A finger or toe reaching forward (-z), curling down, ending in a long red talon."""
    seg = bone(f"{name}_c", (0, 0, -lengths[-1]), (curl[-1] + 14, 0, 0),
               [cube((-w * 0.32, -w * 0.32, -talon), (w * 0.64, w * 0.64, talon), "talon")])
    for i in reversed(range(len(lengths))):
        L, ww = lengths[i], w * (1 - i * 0.12)
        seg = bone(f"{name}_{i}", (0, 0, -lengths[i - 1]) if i else pivot, (curl[i], yaw if i == 0 else 0, 0),
                   [cube((-ww / 2, -ww / 2, -L), (ww, ww, L), "bone"),
                    cube((-ww * 0.6, -ww * 0.6, -0.5), (ww * 1.2, ww * 1.2, 0.6), "bone_mid")], [seg])
    return seg


def rib(i, z, size):
    """One rib: out from the spine, down the flank with a slight bow, then curving in under the chest."""
    out, down = 2.4 * size, 4.2 * size
    return bone(f"rib_r{i}", (0.6, 1.0, z), (0, 0, 0), [
        cube((0, -0.4, -0.35), (out, 0.8, 0.7), "rib", rot=(0, 0, -16), pivot=(0, 0, 0)),
        cube((out - 0.35, -down, -0.35), (0.75, down, 0.7), "rib", rot=(0, 0, 6), pivot=(out, -0.6, 0)),
        cube((0.2, -down - 0.6, -0.35), (out - 0.2, 0.7, 0.7), "rib", rot=(0, 0, 12), pivot=(out, -down, 0)),
    ])


def skull():
    h = bone("head", (0, 0.4, -5.2), (-28, 0, 0), [
        cube((-2.4, -0.6, -5.2), (4.8, 4.2, 5.4), "skull"),                    # cranium
        cube((-2.0, -0.2, -0.3), (4.0, 3.5, 1.0), "bone_mid"),                 # back of the skull
        cube((-2.7, 2.5, -5.7), (5.4, 1.2, 1.7), "bone_mid", rot=(12, 0, 0)),  # brow ridge
        cube((-2.1, 1.2, -5.35), (1.6, 1.1, 0.45), "socket"),                  # eye sockets
        cube((0.5, 1.2, -5.35), (1.6, 1.1, 0.45), "socket"),
        cube((-1.85, 1.4, -5.42), (0.8, 0.6, 0.3), "eye"),
        cube((1.05, 1.4, -5.42), (0.8, 0.6, 0.3), "eye"),
        cube((-1.8, -0.7, -8.4), (3.6, 1.9, 3.5), "skull"),                    # long upper jaw
        cube((-1.3, -0.5, -9.8), (2.6, 1.5, 1.6), "skull"),                    # snout tip
        cube((-0.55, 0.9, -9.4), (1.1, 0.7, 4.6), "bone_mid", rot=(-8, 0, 0)),  # nasal ridge
        cube((-0.9, 0.2, -9.85), (1.8, 0.5, 0.12), "socket"),                  # nostrils
        cube((-1.4, -0.75, -8.2), (2.8, 0.15, 7.6), "mouth"),                  # palate seen through the open jaw
    ])
    for x in (-1, 1):
        s = "r" if x > 0 else "l"
        h.add(cube((x * 2.2 - (1.1 if x > 0 else 0), -0.2, -5.0), (1.1, 1.5, 3.4), "bone", rot=(0, -x * 18, 0)))   # cheekbones
        for k, z in enumerate((-9.3, -8.4, -7.5, -6.6, -5.7)):
            L = 2.4 if k == 0 else 1.7 - k * 0.1
            h.add(cube((x * 1.45 - 0.2, -0.7 - L, z), (0.4, L, 0.4), "fang", rot=(0, 0, -x * 4)))
        # The horn cage: a long horn sweeping back from each temple, a shorter one from each cheek.
        h.add(bone(f"horn_{s}", (x * 2.2, 2.6, -3.6), (55, 0, -x * 32),
                   [cube((-0.6, 0, -0.6), (1.2, 3.2, 1.2), "bone")], [
                       bone(f"horn_{s}_b", (0, 3.2, 0), (22, 0, x * 16), [cube((-0.45, 0, -0.45), (0.9, 2.8, 0.9), "bone")], [
                           bone(f"horn_{s}_c", (0, 2.8, 0), (24, 0, x * 12), [cube((-0.3, 0, -0.3), (0.6, 2.4, 0.6), "barb")])])]))
        h.add(bone(f"cheekhorn_{s}", (x * 2.4, 0.6, -2.4), (72, 0, -x * 62),
                   [cube((-0.4, 0, -0.4), (0.8, 2.4, 0.8), "bone")], [
                       bone(f"cheekhorn_{s}_b", (0, 2.4, 0), (24, 0, x * 10), [cube((-0.28, 0, -0.28), (0.56, 2.0, 0.56), "barb")])]))
    for k, x in enumerate((-1.6, -0.8, 0, 0.8, 1.6)):
        h.add(barb(f"crown_{k}", (x, 3.4, -1.6), (62 + abs(x) * 4, 0, -x * 14), 4.4 - abs(x) * 0.6, 0.85))
    jaw = bone("jaw", (0, -0.9, -3.6), (30, 0, 0), [
        cube((-1.6, -1.1, -6.0), (3.2, 1.0, 6.0), "bone_mid"),
        cube((-1.15, -1.0, -6.8), (2.3, 0.9, 0.9), "bone_mid"),
        cube((-1.3, -0.15, -5.7), (2.6, 0.15, 5.2), "mouth"),
    ])
    for x in (-1, 1):
        for k, z in enumerate((-6.4, -5.4, -4.4, -3.4)):
            L = 1.8 if k == 0 else 1.2
            jaw.add(cube((x * 1.25 - 0.2, -0.1, z), (0.4, L, 0.4), "fang"))
        jaw.add(cube((x * 1.6 - (0.5 if x > 0 else 0), -1.0, -3.0), (0.5, 0.9, 3.0), "bone"))   # jaw hinge
    h.add(jaw)
    return h


def arm():
    """A front leg: shoulder knob, humerus, a bony elbow with a barb, twin forearm bones, a clawed hand."""
    hand = bone("hand_r", (0, -7.6, 0), (-20, 0, 0), [
        cube((-1.4, -0.9, -1.8), (2.8, 1.0, 2.6), "bone_mid"),
        cube((-1.0, -0.5, -0.4), (2.0, 1.2, 1.2), "bone"),
    ], [digit(f"finger_r{i}", (x, -0.4, -1.6), x * -10, (1.6, 1.3), (6, 14)) for i, x in enumerate((-0.9, 0, 0.9))]
       + [digit("thumb_r", (1.2, -0.3, -0.2), -60, (1.0,), (10,), 1.5, 0.6)])
    fore = bone("forearm_r", (0, -6.6, 0), (-42, 0, -16), [
        cube((-1.1, -1.2, -1.1), (2.2, 2.2, 2.2), "bone_mid"),                 # elbow
        cube((-0.9, -7.6, -0.45), (0.8, 7.0, 0.8), "bone"),                    # radius
        cube((0.15, -7.4, -0.05), (0.75, 6.8, 0.75), "bone"),                  # ulna
        cube((-1.0, -7.8, -0.8), (2.0, 1.0, 1.6), "bone_mid"),                 # wrist
    ], [hand, barb("elbow_barb_r", (0, -0.4, 0.8), (105, 0, 0), 3.6, 1.0),
        barb("forearm_barb_r", (0.6, -3.6, 0.3), (110, 0, -40), 2.2, 0.7)])
    return bone("arm_r", (3.4, 0.6, -11.6), (-LEAN + 6, 0, 18), [
        cube((-1.6, -1.4, -1.6), (3.2, 2.8, 3.2), "bone_mid"),                 # shoulder knob
        cube((-1.0, -6.6, -1.0), (2.0, 6.0, 2.0), "bone"),                     # humerus
        cube((-1.25, -3.6, -1.15), (2.5, 2.4, 2.3), "bone_mid"),               # sinew knot
    ], [fore, barb("shoulder_barb_r", (0.9, 0.6, 0.6), (40, 0, -30), 3.2, 1.0)])


def leg():
    """A hind leg, digitigrade: thigh forward, shin back, a long ankle forward, clawed toes; barbed knee and heel."""
    foot = bone("foot_r", (0, -5.0, 0), (34, 0, 0), [cube((-1.3, -0.8, -1.6), (2.6, 1.0, 2.6), "bone_mid")],
                [digit(f"toe_r{i}", (x, -0.4, -1.4), x * -12, (1.4, 1.1), (4, 10), 1.8, 0.65) for i, x in enumerate((-0.8, 0, 0.8))])
    ankle = bone("ankle_r", (0, -6.2, 0), (-78, 0, 0), [
        cube((-0.75, -5.2, -0.75), (1.5, 5.2, 1.5), "bone"),
        cube((-1.0, -0.9, -1.0), (2.0, 1.8, 2.0), "bone_mid"),
    ], [foot, barb("heel_barb_r", (0, -0.4, 0.7), (115, 0, 0), 3.0, 0.9)])
    shin = bone("shin_r", (0, -6.4, 0), (86, 0, 0), [
        cube((-1.2, -1.2, -1.2), (2.4, 2.4, 2.4), "bone_mid"),                 # knee
        cube((-0.8, -6.4, -0.8), (1.6, 6.0, 1.6), "bone"),
        cube((0.3, -5.8, 0.0), (0.6, 5.0, 0.6), "bone_dark"),                  # fibula
    ], [ankle, barb("knee_barb_r", (0, -0.2, -1.0), (-40, 0, 0), 3.0, 1.0)])
    return bone("leg_r", (2.8, -0.6, 0.6), (-LEAN - 40, 0, 6), [
        cube((-1.5, -6.6, -1.5), (3.0, 6.6, 3.0), "bone_mid"),
        cube((-1.7, -2.6, -1.6), (3.4, 2.6, 3.2), "bone"),                     # haunch
    ], [shin])


TAIL_CURL = [-6, -2, -3, -5, -9, -13, -17, -21, -23, -23, -21, -18]


def tail():
    n = len(TAIL_CURL)
    tip = bone("stinger", (0, 0, 2.2), (-30, 0, 0), [
        cube((-1.35, -1.35, 0), (2.7, 2.6, 3.0), "venom"),
    ], [bone("stinger_hook", (0, 0, 3.0), (-38, 0, 0), [cube((-0.6, -0.6, 0), (1.2, 1.2, 3.4), "barb")], [
        bone("stinger_point", (0, 0, 3.4), (-40, 0, 0), [cube((-0.35, -0.35, 0), (0.7, 0.7, 3.0), "barb")])])])
    child = tip
    for i in reversed(range(n)):
        w = 2.3 - i * 0.12
        L = 2.6 - i * 0.04
        seg = bone(f"tail_{i}", (0, 0.4, 2.0) if i == 0 else (0, 0, 2.6 - (i - 1) * 0.04),
                   (TAIL_CURL[i] - (LEAN if i == 0 else 0), 0, 0), [
                       cube((-w / 2, -w * 0.45, 0), (w, w * 0.9, L * 0.8), "vertebra"),
                       cube((-w * 0.6, -w * 0.55, L * 0.75), (w * 1.2, w * 1.1, 0.45), "bone_mid"),
                   ], [child, barb(f"tail_barb_{i}", (0, w * 0.4, L * 0.4), (34, 0, 0), 2.8 - i * 0.12, 0.8 - i * 0.02)])
        if i % 2 == 0:
            for x in (-1, 1):
                seg.add(barb(f"tail_side_{i}_{'r' if x > 0 else 'l'}", (x * w * 0.5, 0, L * 0.4), (20, 0, -x * 80), 1.6 - i * 0.05, 0.55))
        child = seg
    return child


def wing(name, y, z, length, width, flare, sweep):
    """Insect wing: a see-through membrane with a raised red leading strut and veins fanning across it."""
    veins = [cube((-0.15, 0, -0.15), (0.3, length * f, 0.3), "strut", rot=(a, 0, 0), pivot=(0, 0, 0))
             for f, a in ((0.85, 9), (0.7, 18), (0.5, 27))]
    cross = [cube((-0.12, length * f, 0), (0.24, 0.24, width * (1 - f) * 0.9), "strut") for f in (0.3, 0.5, 0.7)]
    return bone(name.replace("_r", "_root_r"), (1.4, 2.0, z), (LEAN * -1, 0, 0), [], [
        bone(name, (0, 0, 0), (sweep, 0, -flare), [
            cube((0, 0, 0), (0, length, width), "membrane"),
            cube((-0.3, 0, -0.3), (0.6, length, 0.6), "strut"),
            cube((-0.45, 0, -0.45), (0.9, 2.2, 0.9), "barb_root"),
        ] + veins + cross)])


def body():
    b = bone("body", (0, 14.0, 5.0), (LEAN, 0, 0), [
        cube((-3.0, -1.8, -1.6), (6.0, 3.2, 4.0), "bone_mid"),                 # pelvis
        cube((-1.0, -0.6, -6.0), (2.0, 2.0, 5.0), "bone_dark"),               # bare spine at the waist
        cube((-1.4, -3.0, -12.4), (2.8, 3.6, 5.8), "innards"),                 # dark hollow behind the ribs
        cube((-0.6, -4.9, -13.4), (1.2, 1.0, 6.8), "bone"),                    # breastbone
        cube((-3.6, 0.2, -13.4), (7.2, 1.8, 3.4), "bone_mid"),                 # shoulder girdle
    ])
    for k in range(9):                                                        # vertebrae along the back
        b.add(cube((-0.8, 1.0, -1.0 - k * 1.55), (1.6, 1.3, 1.1), "vertebra"))
    for x in (-1, 1):
        b.add(cube((x * 2.2 - (1.6 if x > 0 else 0), -0.6, -1.4), (1.6, 2.8, 3.4), "bone", rot=(0, 0, -x * 14)))   # hip blades
        b.add(cube((x * 2.0 - (2.0 if x > 0 else 0), 0.6, -13.0), (2.0, 3.0, 3.6), "bone", rot=(-10, 0, -x * 22)))  # shoulder blades
    for i, z in enumerate((-6.6, -8.2, -9.8, -11.4, -12.8)):
        r = rib(i, z, 0.8 + 0.06 * i)
        b.add(r, mirror(r))
    for k in range(8):
        b.add(barb(f"spine_barb_{k}", (0, 2.2, -0.8 - k * 1.7), (-LEAN + 34 - k * 2, 0, 0), 4.4 - abs(k - 4) * 0.4, 1.0))
    neck = bone("neck", (0, 1.4, -13.6), (16, 0, 0), [
        cube((-0.9, -0.9, -1.8), (1.8, 1.8, 1.8), "vertebra"),
        cube((-0.85, -0.8, -3.6), (1.7, 1.7, 1.8), "vertebra", rot=(-6, 0, 0)),
        cube((-0.8, -0.7, -5.3), (1.6, 1.6, 1.8), "vertebra", rot=(-10, 0, 0)),
    ], [skull()] + [barb(f"neck_barb_{k}", (0, 0.8, -0.8 - k * 1.7), (40, 0, 0), 2.4, 0.8) for k in range(3)])
    b.add(neck)
    a, l = arm(), leg()
    b.add(a, mirror(a), l, mirror(l), tail())
    for wb in (wing("wing_r", 2.0, -11.0, 21, 8, 30, 22), wing("hindwing_r", 2.0, -9.4, 16, 7, 42, 52)):
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


def p_skull(ctx):
    c = mix(ctx.col("bone"), ctx.col("bone_mid"), ctx.rng.uniform(0, 0.25) + ctx.v * 0.2)
    if ctx.side == "up" and abs(ctx.u - 0.5) < 0.08:
        c = mix(c, ctx.col("bone_dark"), 0.35)                                # suture down the crown
    return depth(ctx, c, bevel=0.2)


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
    return depth(ctx, mix(ctx.col("bone"), ctx.col("bone_mid"), ctx.v * 0.8), bevel=0.1)


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
    # See-through membrane: pale, faintly veined, cloudier near the root, torn at the outer edge and the tip.
    if ctx.side not in ("east", "west"):
        return None
    if (ctx.u > 0.82 and (ctx.y * 7 + ctx.x * 3) % 9 < 3) or (ctx.v < 0.1 and ctx.u > 0.35 + ctx.v * 4) or ctx.rng.random() < 0.015:
        return None
    c = mix(ctx.col("membrane"), ctx.col("vein"), 0.06 + ctx.rng.uniform(0, 0.06))
    if (ctx.x + ctx.y) % 13 == 0:
        c = mix(c, ctx.col("vein"), 0.35)
    a = round(150 + 70 * ctx.v)
    return with_alpha(shade(c, 1.0 + 0.08 * (1 - ctx.v)), a)


def p_socket(ctx):
    return ctx.col("shadow")


def p_eye(ctx):
    c = ctx.col("eye")
    return ("glow", mix(c, hexc("ffffff"), 0.35) if (ctx.x + ctx.y) % 3 == 0 else c)


def p_mouth(ctx):
    return shade(ctx.col("mouth"), ctx.rng.uniform(0.85, 1.1))


def p_innards(ctx):
    return shade(ctx.col("shadow"), ctx.rng.uniform(0.8, 1.15))


PAINTERS = {"bone": p_bone("bone"), "bone_mid": p_bone("bone_mid"), "bone_dark": p_bone("bone_dark"), "skull": p_skull,
            "vertebra": p_vertebra, "rib": p_rib, "barb": p_barb, "barb_root": p_barb_root, "talon": p_talon,
            "fang": p_fang, "venom": p_venom, "strut": p_strut, "membrane": p_membrane, "socket": p_socket,
            "eye": p_eye, "mouth": p_mouth, "innards": p_innards}


# ----------------------------------------------------------------------------- animations

def _w(t, period, phase=0.0):
    return math.sin(2 * math.pi * (t / period + phase))


def idle(t):
    p = 3.2
    out = {"body": {"pos": (0, 0.25 * _w(t, p), 0)},
           "neck": {"rot": (3 * _w(t, p, 0.1), 4 * _w(t, p * 2), 0)},
           "jaw": {"rot": (5 + 5 * _w(t, p / 2), 0, 0)},
           "wing_r": {"rot": (0, 0, -3 * _w(t, p))}, "wing_l": {"rot": (0, 0, 3 * _w(t, p))},
           "hindwing_r": {"rot": (0, 0, -2 * _w(t, p, 0.2))}, "hindwing_l": {"rot": (0, 0, 2 * _w(t, p, 0.2))}}
    for i in range(len(TAIL_CURL)):
        out[f"tail_{i}"] = {"rot": (1.2 * _w(t, p, i * 0.05), 5 * _w(t, p, i * 0.07), 0)}
    return out


def walk(t):
    p = 1.0
    s, o = _w(t, p), _w(t, p, 0.5)
    lift_a, lift_b = max(0.0, _w(t, p, 0.25)), max(0.0, _w(t, p, 0.75))
    out = {"body": {"pos": (0, 0.5 * abs(_w(t, p / 2)), 0)},
           "arm_r": {"rot": (-26 * s, 0, 0)}, "leg_l": {"rot": (-26 * s, 0, 0)},
           "arm_l": {"rot": (-26 * o, 0, 0)}, "leg_r": {"rot": (-26 * o, 0, 0)},
           "forearm_r": {"rot": (-24 * lift_a, 0, 0)}, "shin_l": {"rot": (20 * lift_a, 0, 0)},
           "forearm_l": {"rot": (-24 * lift_b, 0, 0)}, "shin_r": {"rot": (20 * lift_b, 0, 0)},
           "neck": {"rot": (4 * _w(t, p / 2, 0.25), 0, 0)},
           "jaw": {"rot": (6, 0, 0)}}
    for i in range(len(TAIL_CURL)):
        out[f"tail_{i}"] = {"rot": (0, 7 * _w(t, p, i * 0.06), 0)}
    return out


def attack(t):
    L = 0.8
    k = math.sin(math.pi * min(1.0, t / L))                                  # 0 -> 1 -> 0
    strike = math.sin(math.pi * min(1.0, max(0.0, (t - 0.15) / 0.5)))
    out = {"body": {"pos": (0, -0.3 * k, -2.0 * strike), "rot": (-4 * strike, 0, 0)},
           "neck": {"rot": (-10 * k, 0, 0)}, "jaw": {"rot": (14 * k, 0, 0)},
           "wing_r": {"rot": (0, 0, -16 * k + 6 * _w(t, 0.2))}, "wing_l": {"rot": (0, 0, 16 * k - 6 * _w(t, 0.2))},
           "hindwing_r": {"rot": (0, 0, -12 * k + 6 * _w(t, 0.2, 0.5))}, "hindwing_l": {"rot": (0, 0, 12 * k - 6 * _w(t, 0.2, 0.5))},
           "arm_r": {"rot": (-14 * strike, 0, 0)}, "arm_l": {"rot": (-14 * strike, 0, 0)},
           "stinger": {"rot": (-26 * strike, 0, 0)}}
    for i in range(len(TAIL_CURL)):
        out[f"tail_{i}"] = {"rot": ((-9 if i > 4 else 4) * strike, 0, 0)}
    return out


ANIMS = {"idle": dict(length=3.2, loop=True, fn=idle), "walk": dict(length=1.0, loop=True, fn=walk),
         "attack": dict(length=0.8, loop=False, fn=attack)}


if __name__ == "__main__":
    export(MODEL, model(), ASSETS / "geo" / f"{MODEL}.geo.json", PAINTERS, PALETTES, ASSETS / "textures" / "entity" / "geo")
    export_animations(MODEL, ANIMS, ASSETS / "animations" / f"{MODEL}.animation.json")
    print("wrote", MODEL)
