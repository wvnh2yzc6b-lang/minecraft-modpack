#!/usr/bin/env python3
"""
Custom unit models for Warfront, defined once and exported three ways:

  * Java geometry (src/main/java/com/warfront/client/model/UnitGeometry.java)
  * hand-painted textures (assets/warfront/textures/entity/soldier/<key>/<unit>.png)
  * a JSON copy of the geometry (build/unit-models.json) for the 3D preview page

Coordinates follow Minecraft entity models: pixels, y points down, the model faces -Z.
Each part has a pivot, a rotation (radians, applied Z then Y then X like ModelPart) and boxes.
A box with a zero dimension is a flat plane (used for wing membranes).

Run:  python3 tools/units.py
"""
import json
import math
import random
import re
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src" / "main" / "resources" / "assets" / "warfront" / "textures" / "entity" / "soldier"
JAVA = ROOT / "src" / "main" / "java" / "com" / "warfront" / "client" / "model" / "UnitGeometry.java"
JSON_OUT = ROOT / "build" / "unit-models.json"


# ----------------------------------------------------------------------------- spec helpers

def part(name, pivot=(0, 0, 0), rot=(0, 0, 0), boxes=(), children=()):
    return {"name": name, "pivot": list(pivot), "rot": list(rot), "boxes": list(boxes), "children": list(children)}


def box(origin, size, mat, **extra):
    b = {"origin": list(origin), "size": list(size), "mat": mat}
    b.update(extra)
    return b


def mirror(p):
    """Mirror a part (and its children) across x = 0, renaming _r <-> _l."""
    name = p["name"]
    if re.search(r"_r(?=$|_|\d)", name):
        name = re.sub(r"_r(?=$|_|\d)", "_l", name)
    elif re.search(r"_l(?=$|_|\d)", name):
        name = re.sub(r"_l(?=$|_|\d)", "_r", name)
    elif name == "right_arm":
        name = "left_arm"
    elif name == "right_leg":
        name = "left_leg"
    px, py, pz = p["pivot"]
    rx, ry, rz = p["rot"]
    boxes = []
    for b in p["boxes"]:
        (x, y, z), (w, h, d) = b["origin"], b["size"]
        nb = dict(b)
        nb["origin"] = [-(x + w), y, z]
        nb["mirror"] = not b.get("mirror", False)
        boxes.append(nb)
    return {"name": name, "pivot": [-px, py, pz], "rot": [rx, -ry, -rz], "boxes": boxes,
            "children": [mirror(c) for c in p["children"]]}


# ----------------------------------------------------------------------------- the Imp

def imp_base(model_id):
    """Shared imp body. Small, gaunt, bat-winged, scorpion-tailed, digitigrade."""
    head = part("head", pivot=(0, 1, -1.5), boxes=[
        box((-3.5, -7, -3.5), (7, 7, 7), "imp_head"),
        box((-3.5, -5, -4.5), (7, 1, 1), "skin_dark"),           # heavy brow
    ], children=[
        part("jaw", pivot=(0, 0, -3.5), boxes=[box((-2.5, -1.5, -1), (5, 2, 1), "jaw")]),
        part("goatee", pivot=(0, 0.5, -4.2), rot=(0.25, 0, 0), boxes=[box((-0.5, 0, -0.5), (1, 3, 1), "beard")]),
        part("ear_r", pivot=(-3.5, -4, 0.5), rot=(0, -0.45, -0.3), boxes=[box((-4, -1.5, -0.5), (4, 3, 1), "ear")],
             children=[part("ear_r_tip", pivot=(-4, -1, 0), rot=(0, 0, -0.5),
                            boxes=[box((-2, -0.5, -0.5), (2, 1, 1), "ear")])]),
        part("horn_c", pivot=(0, -7, 0.5), rot=(-0.6, 0, 0), boxes=[box((-0.5, -3, -0.5), (1, 3, 1), "horn")]),
        part("horn_r1", pivot=(-1.8, -7, -0.5), rot=(-0.45, 0, -0.35), boxes=[box((-0.5, -4, -0.5), (1, 4, 1), "horn")]),
        part("horn_r2", pivot=(-3, -6.5, 1.5), rot=(-0.7, 0, -0.65), boxes=[box((-0.5, -3, -0.5), (1, 3, 1), "horn")]),
        part("horn_r3", pivot=(-2, -6, 3), rot=(-1.1, 0, -0.4), boxes=[box((-0.5, -2, -0.5), (1, 2, 1), "horn")]),
    ])
    head["children"] += [mirror(c) for c in head["children"] if re.search(r"_r\d?$", c["name"])]

    # Bat wings: a bony arm strut raised high above the head, a long finger strut, and a ragged
    # membrane hanging from the strut down toward the back. Much taller than the imp itself.
    wing_r = part("wing_r", pivot=(-1.5, 1.5, 1.5), rot=(0.25, 0.6, 0.85), boxes=[
        box((-15, -0.5, -0.5), (15, 1, 1), "wing_bone"),
        box((-15, 0.5, 0), (15, 19, 0), "membrane"),
    ], children=[
        part("wing_r_claw", pivot=(-15, 0, 0), rot=(0, 0, -0.9), boxes=[box((-0.5, -3, -0.5), (1, 3, 1), "claw")]),
        part("wing_r_finger1", pivot=(-6, 0.5, 0), rot=(0, 0, -0.18), boxes=[box((-0.5, 0, -0.5), (1, 18, 1), "wing_bone")]),
        part("wing_r_finger2", pivot=(-11, 0.5, 0), rot=(0, 0, 0.05), boxes=[box((-0.5, 0, -0.5), (1, 16, 1), "wing_bone")]),
    ])

    # Tail: thick bony segments curling up behind the body, ending in a barbed tuft.
    sizes = [(3, 3, 3), (3, 3, 3), (2, 2, 3), (2, 2, 3), (2, 2, 3), (1, 1, 3)]
    rots = [(-0.55, 0, 0), (0.15, 0, 0), (0.35, 0, 0), (0.45, 0, 0), (0.5, 0, 0), (0.5, 0, 0)]
    tail = part("tail_7", pivot=(0, 0, 3), rot=(0.3, 0, 0), boxes=[box((-1, -1, 0), (2, 2, 2), "claw")],
                children=[part("tail_tuft", pivot=(0, -1, 1), rot=(-0.6, 0, 0),
                               boxes=[box((-1.5, -3, 0), (3, 3, 0), "tuft"), box((0, -3, -1), (0, 3, 3), "tuft")])])
    for i in range(6, 0, -1):
        w, h, d = sizes[i - 1]
        pivot = (0, 10.5, 1.5) if i == 1 else (0, 0, sizes[i - 2][2])
        tail = part(f"tail_{i}", pivot=pivot, rot=rots[i - 1], boxes=[box((-w / 2, -h / 2, 0), (w, h, d), "tail")],
                    children=[tail])

    body = part("body", rot=(0.2, 0, 0), boxes=[
        box((-3, 0, -1.5), (6, 9, 3), "torso"),
        box((-2.5, 9, -1.5), (5, 3, 3), "skin"),
    ], children=[wing_r, mirror(wing_r), tail])

    def claws(prefix, y):
        return [part(f"{prefix}_claw{i}", pivot=(x, y, -0.6), rot=(-0.45, 0, (x * 0.25)),
                     boxes=[box((-0.5, 0, -0.5), (1, 4, 1), "claw")]) for i, x in enumerate((-0.8, 0.0, 0.8))]

    arm_r = part("right_arm", pivot=(-4.2, 2.5, 0), rot=(0, 0, 0.12), boxes=[box((-1, -1, -1), (2, 7, 2), "skin")], children=[
        part("forearm_r", pivot=(0, 6, 0), rot=(-0.3, 0, -0.08), boxes=[box((-1, 0, -1), (2, 7, 2), "skin_fade")], children=[
            part("hand_r", pivot=(0, 7, 0), boxes=[box((-1, 0, -1), (2, 1, 2), "skin_dark")], children=claws("hand_r", 1)),
        ]),
    ])

    leg_r = part("right_leg", pivot=(-1.5, 12, 0), rot=(-0.6, 0, 0), boxes=[box((-1, 0, -1), (2, 5, 2), "skin")],
                 children=[
                     part("shin_r", pivot=(0, 5, 0), rot=(1.1, 0, 0), boxes=[box((-0.75, 0, -0.75), (1.5, 6, 1.5), "skin_fade")],
                          children=[
                              part("ankle_r", pivot=(0, 6, 0), rot=(-0.8, 0, 0),
                                   boxes=[box((-0.75, 0, -0.75), (1.5, 3, 1.5), "skin_dark")], children=[
                                      part("foot_r", pivot=(0, 3, 0), rot=(0.3, 0, 0),
                                           boxes=[box((-1, -0.5, -1), (2, 1, 2), "skin_dark")],
                                           children=[part(f"toe_r{i}", pivot=(x, 0, -1), rot=(0.25, 0, 0),
                                                          boxes=[box((-0.5, -0.5, -2.5), (1, 1, 3), "claw")])
                                                     for i, x in enumerate((-0.7, 0.0, 0.7))]),
                                  ]),
                          ]),
                 ])

    return {
        "id": model_id,
        "tex": [128, 128],
        "parts": [head, part("hat"), body, arm_r, mirror(arm_r), leg_r, mirror(leg_r)],
    }




def find(model, name):
    def walk(parts):
        for p in parts:
            if p["name"] == name:
                return p
            r = walk(p["children"])
            if r:
                return r
        return None
    found = walk(model["parts"])
    if found is None:
        raise KeyError(name)
    return found


def attach(model, parent, *parts):
    find(model, parent)["children"].extend(parts)


def overlay(name, origin, size, mat, grow):
    """A shell box drawn over a body part (armor, clothing, tattoos), inflated by ``grow`` px."""
    return part(name, boxes=[box(origin, size, mat, grow=grow)])


def belt_and_loincloth(m, cloth="rag"):
    attach(m, "body",
           overlay("belt", (-2.5, 9, -1.5), (5, 2, 3), "leather_belt", 0.35),
           part("loin_front", pivot=(0, 10.5, -1.9), rot=(-0.08, 0, 0), boxes=[box((-2, 0, 0), (4, 5, 0), cloth)]),
           part("loin_back", pivot=(0, 10.5, 1.9), rot=(0.12, 0, 0), boxes=[box((-2, 0, 0), (4, 5, 0), cloth)]))


def imp():
    """Imp (swordsman): scrappy and light. Stitched belt, ragged loincloth, wrist wraps."""
    m = imp_base("imp")
    belt_and_loincloth(m)
    attach(m, "belt", part("trinket_1", pivot=(1.6, 10.6, -1.9), boxes=[box((-0.5, 0, -0.5), (1, 2, 1), "bone")]),
           part("trinket_2", pivot=(-1.4, 10.6, -1.9), rot=(0, 0, 0.3), boxes=[box((-0.5, 0, -0.5), (1, 1, 1), "bone")]))
    for side in ("r", "l"):
        attach(m, f"forearm_{side}", overlay(f"wrap_{side}", (-1, 3, -1), (2, 4, 2), "wrap", 0.3))
    return m


def imp_bulwark():
    """Imp Bulwark (shieldbearer): scavenged scrap armor. Dented riveted breastplate, skullcap, one spiked pauldron."""
    m = imp_base("imp_bulwark")
    belt_and_loincloth(m, cloth="rag_dark")
    attach(m, "body", overlay("breastplate", (-3, 0, -1.5), (6, 8, 3), "iron_scrap", 0.55),
           overlay("strap_back", (-3, 1, -1.5), (6, 2, 3), "leather", 0.7))
    attach(m, "head", overlay("skullcap", (-3.5, -7, -3.5), (7, 3, 7), "iron_cap", 0.45),
           part("cap_nasal", pivot=(0, -4.2, -4.1), boxes=[box((-0.5, 0, -0.5), (1, 2, 1), "iron_scrap")]))
    attach(m, "left_arm",
           part("pauldron_l", pivot=(0.5, -1.2, 0), rot=(0, 0, -0.25), boxes=[box((-1.5, -1, -2), (4, 2, 4), "iron_scrap")],
                children=[part("pauldron_spike_1", pivot=(1.2, -1, -0.6), rot=(0, 0, -0.5),
                               boxes=[box((-0.5, -3, -0.5), (1, 3, 1), "iron_spike")]),
                          part("pauldron_spike_2", pivot=(1.2, -1, 1.0), rot=(0.2, 0, -0.7),
                               boxes=[box((-0.5, -2, -0.5), (1, 2, 1), "iron_spike")])]))
    attach(m, "right_arm", overlay("shoulder_strap_r", (-1, -1, -1), (2, 2, 2), "leather", 0.4))
    for side in ("r", "l"):
        attach(m, f"forearm_{side}", overlay(f"vambrace_{side}", (-1, 2, -1), (2, 5, 2), "iron_scrap", 0.35))
    return m


def bone_spike(name, pivot, rot, length=3):
    return part(name, pivot=pivot, rot=rot, boxes=[box((-0.5, -length, -0.5), (1, length, 1), "bone_spike")])


def imp_impaler():
    """Imp Impaler (spearman): rugged and sharp. Studded harness, bone spikes, bracers, scars."""
    m = imp_base("imp_impaler")
    belt_and_loincloth(m, cloth="rag_torn")
    attach(m, "body", overlay("harness", (-3, 0, -1.5), (6, 9, 3), "harness", 0.3))
    # Longer, sharper horns: two extra swept spikes.
    attach(m, "head", bone_spike("horn_x_r", (-2.6, -7, 0), (-0.9, 0, -0.55), 5),
           bone_spike("horn_x_l", (2.6, -7, 0), (-0.9, 0, 0.55), 5))
    for side, sx in (("r", -1), ("l", 1)):
        arm = "right_arm" if side == "r" else "left_arm"
        attach(m, arm, overlay(f"shoulder_pad_{side}", (-1, -1, -1), (2, 3, 2), "studded", 0.4),
               bone_spike(f"shoulder_spike_{side}1", (sx * 0.8, -1.8, -0.4), (-0.25, 0, sx * 0.75), 5),
               bone_spike(f"shoulder_spike_{side}2", (sx * 0.5, -1.6, 0.9), (0.45, 0, sx * 0.55), 4))
        attach(m, f"forearm_{side}", overlay(f"bracer_{side}", (-1, 1, -1), (2, 5, 2), "studded", 0.35),
               part(f"forearm_spike_{side}", pivot=(0, 2, 1.2), rot=(0.9, 0, 0),
                    boxes=[box((-0.5, -3, -0.5), (1, 3, 1), "bone_spike")]))
        attach(m, f"shin_{side}", part(f"knee_spike_{side}", pivot=(0, 0.5, -0.8), rot=(-1.2, 0, 0),
                                        boxes=[box((-0.5, -3, -0.5), (1, 3, 1), "bone_spike")]))
    return m


def imp_firecaster():
    """Imp Firecaster (archer): magical. Rune-trimmed tattered mantle, glowing tattoos, charms, ember orb."""
    m = imp_base("imp_firecaster")
    belt_and_loincloth(m, cloth="rag_rune")
    attach(m, "body",
           overlay("runes_body", (-3, 0, -1.5), (6, 9, 3), "rune_tattoo", 0.05),
           overlay("mantle", (-3.5, -0.5, -2), (7, 2, 4), "mantle", 0.55),
           part("mantle_back", pivot=(0, 3.4, 2.6), rot=(0.12, 0, 0), boxes=[box((-3, 0, 0), (6, 6, 0), "mantle_hang")]),
           part("necklace", pivot=(0, 0.3, -2.1), rot=(0.1, 0, 0), boxes=[box((-2, 0, -0.5), (4, 1, 1), "charms")],
                children=[part("charm_skull", pivot=(0, 1, 0), boxes=[box((-0.5, 0, -0.7), (1, 1, 1), "bone")])]))
    for side in ("r", "l"):
        arm = "right_arm" if side == "r" else "left_arm"
        attach(m, arm, overlay(f"runes_arm_{side}", (-1, -1, -1), (2, 7, 2), "rune_tattoo", 0.05))
    attach(m, "hand_l", part("ember_orb", pivot=(0, 3.5, -1.5), boxes=[box((-1, -1, -1), (2, 2, 2), "orb")],
                             children=[part("ember_orb_core", boxes=[box((-0.5, -0.5, -0.5), (1, 1, 1), "orb_core")])]))
    # Glowing horn tips.
    for name in ("horn_c", "horn_r1", "horn_l1", "horn_r2", "horn_l2"):
        for b in find(m, name)["boxes"]:
            b["mat"] = "horn_glow"
    return m


MODELS = [imp(), imp_bulwark(), imp_impaler(), imp_firecaster()]


# ----------------------------------------------------------------------------- UV packing

def footprint(size):
    w, h, d = [math.ceil(v) for v in size]
    return 2 * (d + w), d + h


def all_boxes(parts):
    for p in parts:
        for b in p["boxes"]:
            yield p, b
        yield from all_boxes(p["children"])


def pack(model):
    tw, th = model["tex"]
    boxes = sorted(all_boxes(model["parts"]), key=lambda pb: -footprint(pb[1]["size"])[1])
    x = y = row_h = 0
    for _, b in boxes:
        fw, fh = footprint(b["size"])
        fw, fh = max(fw, 1), max(fh, 1)
        if x + fw > tw:
            x, y, row_h = 0, y + row_h, 0
        if y + fh > th:
            raise SystemExit(f"{model['id']}: texture {tw}x{th} too small")
        b["uv"] = [x, y]
        x += fw
        row_h = max(row_h, fh)


# ----------------------------------------------------------------------------- painting

def hexc(s, a=255):
    s = s.lstrip("#")
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), a)


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (255,)


def faces(b):
    """Face rectangles (x, y, w, h) on the texture, keyed by the side of the box they show."""
    u, v = b["uv"]
    w, h, d = [math.ceil(s) for s in b["size"]]
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
    }


IMP_PALETTES = {
    # player demons: crimson with dark mottling, bone tail
    "demon": dict(skin="9e211b", dark="56100d", hi="c9483a", deep="230807", claw="140c0b", horn="2b1a15",
                  horn_tip="7a5a48", membrane="6e1612", vein="3e0a08", tail="b38c5c", tail_dark="6a4e2e",
                  eye="ffd23a", eye_hi="fff4b8", tuft="2a1410",
                  leather="6b4226", leather_dark="3a2414", iron="7c7f84", iron_dark="45474c", rust="8a4a22",
                  cloth="5a3a2a", cloth_dark="2e1d15", bone="d8cdb0", bone_dark="8c7c5c", rune="ff9a2e",
                  rune_hi="ffe28a", mantle="3a1020", mantle_trim="c9772a", orb="ff6a10", orb_core="fff1a0"),
    # Burning Horde: darker, ember-veined
    "burning_horde": dict(skin="7a1712", dark="3a0a07", hi="e0602a", deep="160504", claw="0c0808", horn="1a1210",
                          horn_tip="ff7a1a", membrane="4a0d0a", vein="ff5a10", tail="8a6a48", tail_dark="3e2a18",
                          eye="ff7a00", eye_hi="ffe0a0", tuft="1a0a06", vein_glow=True,
                          leather="3e2616", leather_dark="1e120a", iron="4a4448", iron_dark="242026", rust="a04a18",
                          cloth="2e1a14", cloth_dark="140a08", bone="b8a888", bone_dark="6a5a40", rune="ff5a10",
                          rune_hi="ffd060", mantle="1a0808", mantle_trim="ff6a1a", orb="ff4a00", orb_core="ffe080"),
}


def paint_box(img, glow_img, b, pal, rng):
    mat = b["mat"]
    for side, (x0, y0, w, h) in faces(b).items():
        for yy in range(h):
            for xx in range(w):
                c = material(mat, side, xx, yy, w, h, pal, rng)
                if c is None:
                    continue
                if isinstance(c, tuple) and c and c[0] == "glow":
                    c = c[1]
                    glow_img.putpixel((x0 + xx, y0 + yy), shade(c, 0.5))
                img.putpixel((x0 + xx, y0 + yy), c)


def mottled(pal, rng, x, y, w, h, side):
    base = hexc(pal["skin"])
    c = shade(base, 1 + rng.uniform(-0.06, 0.06))
    r = rng.random()
    if r < 0.16:
        c = mix(c, hexc(pal["dark"]), 0.55)
    elif r < 0.22:
        c = mix(c, hexc(pal["hi"]), 0.45)
    if side == "top":
        c = shade(c, 1.12)
    elif side == "bottom":
        c = shade(c, 0.75)
    elif side in ("left", "right"):
        c = shade(c, 0.9)
    return c


def glow(c):
    """Marks a pixel as emissive: drawn in the base texture and in the glow layer."""
    return ("glow", c)


def edge(x, y, w, h):
    return x == 0 or y == 0 or x == w - 1 or y == h - 1


def leather_px(pal, rng, x, y, w, h, side, stitch_row=None):
    c = mix(hexc(pal["leather"]), hexc(pal["leather_dark"]), rng.uniform(0, 0.35))
    if rng.random() < 0.12:
        c = mix(c, hexc(pal["leather_dark"]), 0.6)          # grain pits
    if rng.random() < 0.05:
        c = mix(c, hexc("c8a07a"), 0.35)                     # scuffs
    if edge(x, y, w, h) and side in ("front", "back", "left", "right"):
        c = shade(c, 0.78)                                    # worn, darkened edges
    if stitch_row is not None and y == stitch_row and x % 2 == 0 and side in ("front", "back"):
        c = hexc("d8c49a")                                    # stitching
    if side == "top":
        c = shade(c, 1.12)
    return c


def iron_px(pal, rng, x, y, w, h, side, rivets=True):
    t = y / max(1, h - 1)
    c = mix(hexc(pal["iron"]), hexc(pal["iron_dark"]), 0.15 + 0.5 * t)    # light from above
    c = shade(c, rng.uniform(0.92, 1.08))
    if rng.random() < 0.07:
        c = mix(c, hexc(pal["rust"]), 0.65)                  # rust spots
    if rng.random() < 0.05:
        c = shade(c, 0.7)                                     # dents
    if (x + y) % 7 == 0 and rng.random() < 0.4:
        c = mix(c, hexc("d0d4da"), 0.35)                     # scratches
    if rivets and side in ("front", "back") and y in (0, h - 1) and x % 3 == 1:
        c = hexc("c4c8ce")                                    # rivet heads
    if rivets and side in ("front", "back") and y in (1,) and x % 3 == 1:
        c = shade(hexc(pal["iron_dark"]), 0.8)               # rivet shadow
    if side == "top":
        c = shade(c, 1.18)
    if side == "bottom":
        c = shade(c, 0.7)
    return c


def cloth_px(pal, rng, x, y, w, h, side, base="cloth", fray=0, holes=0.0):
    if fray and y >= h - fray and (x * 5 + y * 3) % 3 == 0:
        return None                                           # frayed hem
    if holes and rng.random() < holes:
        return None
    c = hexc(pal[base]) if base in pal else hexc(base)
    if (x + y) % 2 == 0:
        c = shade(c, 1.07)                                    # weave
    if x % 3 == 0:
        c = shade(c, 0.93)
    if rng.random() < 0.08:
        c = mix(c, hexc(pal["cloth_dark"]), 0.5)             # stains
    if y >= h - 1:
        c = shade(c, 0.8)
    return c


def gear_material(mat, side, x, y, w, h, pal, rng):
    """Clothing and armor materials. Returns NotImplemented for anything else."""
    if mat == "leather_belt":
        if side in ("top", "bottom"):
            return leather_px(pal, rng, x, y, w, h, side)
        c = leather_px(pal, rng, x, y, w, h, side, stitch_row=0)
        if side == "front" and x in (w // 2 - 1, w // 2) :
            return hexc("b89a50") if y == 1 else shade(hexc("8a7038"), 0.9)     # buckle
        return c
    if mat == "leather":
        return leather_px(pal, rng, x, y, w, h, side, stitch_row=h - 1)
    if mat == "wrap":
        c = cloth_px(pal, rng, x, y, w, h, side, base="bone_dark")
        if c is None:
            return None
        if (y + x // 2) % 2 == 0:
            c = shade(c, 0.78)                                # wound bands
        return c
    if mat in ("rag", "rag_dark", "rag_torn", "rag_rune"):
        base = "cloth_dark" if mat == "rag_dark" else "cloth"
        c = cloth_px(pal, rng, x, y, w, h, side, base=base, fray=2 if mat != "rag_torn" else 3,
                     holes=0.06 if mat == "rag_torn" else 0.0)
        if c is None:
            return None
        if mat == "rag_torn" and x in (0, w - 1) and y > 1 and rng.random() < 0.5:
            return None                                       # ripped sides
        if mat == "rag_rune" and side in ("front", "back") and y == 1 and x % 2 == 0:
            return glow(hexc(pal["rune"]))                    # ember-stitched trim
        return c
    if mat == "bone":
        c = mix(hexc(pal["bone"]), hexc(pal["bone_dark"]), rng.uniform(0, 0.3))
        return shade(c, 1.1 if side == "top" else 0.95)
    if mat in ("bone_spike", "iron_spike"):
        t = 1 - y / max(1, h - 1)    # spikes grow upward from their pivot: y=0 is the tip
        if mat == "bone_spike":
            c = mix(hexc(pal["bone"]), hexc(pal["bone_dark"]), t * 0.8)
            if rng.random() < 0.12:
                c = shade(c, 0.75)                            # cracks
        else:
            c = mix(hexc(pal["iron"]), hexc("d8dce2"), t * 0.6)
        return shade(c, rng.uniform(0.92, 1.06))
    if mat in ("iron_scrap", "iron_cap"):
        if mat == "iron_cap" and side in ("front", "back", "left", "right") and y >= h - 1 and x % 2 == 0:
            return hexc("c4c8ce")                             # rim rivets
        return iron_px(pal, rng, x, y, w, h, side, rivets=(mat == "iron_scrap"))
    if mat == "harness":
        # Two straps crossing the chest and back, studded; everything else is see-through.
        on = abs((x - y * w / h)) < 1.0 or abs((w - 1 - x) - y * w / h) < 1.0 or y in (h - 3, h - 2)
        if side in ("left", "right"):
            on = y in (1, 2, h - 3, h - 2)
        if side in ("top", "bottom"):
            on = x in (1, w - 2)
        if not on:
            return None
        c = leather_px(pal, rng, x, y, w, h, side)
        if y % 3 == 0 and side in ("front", "back"):
            c = hexc("b8bcc2")                                # studs
        return c
    if mat == "studded":
        c = leather_px(pal, rng, x, y, w, h, side)
        if side in ("front", "back", "left", "right") and x % 2 == 1 and y % 2 == 1:
            c = hexc("a8acb2")
        return c
    if mat == "rune_tattoo":
        if side not in ("front", "back", "left", "right"):
            return None
        if w >= 6 and side == "front":
            # A burning sigil over the heart: a ring with a downward fork.
            sigil = ["..###.",
                     ".#...#",
                     ".#.#.#",
                     ".#...#",
                     "..###.",
                     "...#..",
                     "..#.#.",
                     ".#...#"]
            sy = y - 1
            if 0 <= sy < len(sigil) and x < len(sigil[sy]) and sigil[sy][x] == "#":
                return glow(hexc(pal["rune_hi"]) if sy in (2, 5) else hexc(pal["rune"]))
            return None
        if w >= 6 and side == "back":
            return glow(hexc(pal["rune"])) if x in (2, 3) and y % 3 != 2 and 0 < y < h - 1 else None
        # Limbs: two angular bands of glyph marks.
        if y in (1, 4) and x % 2 == 0:
            return glow(hexc(pal["rune"]))
        if y in (2, 5) and x % 2 == 1:
            return glow(hexc(pal["rune_hi"]))
        return None
    if mat in ("mantle", "mantle_hang"):
        fray = 2 if mat == "mantle_hang" else 1
        if y >= h - fray and (x * 7 + y) % 3 == 0:
            return None
        if mat == "mantle_hang" and side not in ("front", "back"):
            return None
        c = hexc(pal["mantle"])
        if (x + y) % 2 == 0:
            c = shade(c, 1.1)
        if rng.random() < 0.1:
            c = shade(c, 0.75)
        trim_row = h - 1 if mat == "mantle" else 0
        if y == trim_row and side in ("front", "back", "left", "right"):
            return glow(hexc(pal["mantle_trim"])) if x % 2 == 0 else hexc(pal["mantle_trim"])
        if mat == "mantle_hang" and side == "back" and x in (w // 2 - 1, w // 2) and 1 < y < h - 2:
            return glow(hexc(pal["rune"]))                    # sigil down the back
        return c
    if mat == "charms":
        if side in ("top", "bottom"):
            return None
        return hexc("d8cdb0") if x % 2 == 0 else hexc(pal["leather_dark"])
    if mat == "rune_band":
        if side in ("top", "bottom"):
            return None
        c = hexc(pal["mantle"])
        if x % 3 == 1:
            return glow(hexc(pal["rune"]))
        return c
    if mat == "orb":
        return glow(mix(hexc(pal["orb"]), hexc(pal["orb_core"]), rng.uniform(0, 0.4)))
    if mat == "orb_core":
        return glow(hexc(pal["orb_core"]))
    if mat == "horn_glow":
        t = 1 - y / max(1, h - 1)
        if t > 0.6:
            return glow(mix(hexc(pal["rune"]), hexc(pal["rune_hi"]), t))
        return shade(mix(hexc(pal["horn"]), hexc(pal["horn_tip"]), t * 0.8), rng.uniform(0.9, 1.1))
    return NotImplemented


def material(mat, side, x, y, w, h, pal, rng):
    g = gear_material(mat, side, x, y, w, h, pal, rng)
    if g is not NotImplemented:
        return g
    if mat in ("skin", "torso"):
        c = mottled(pal, rng, x, y, w, h, side)
        if mat == "torso" and side == "front":
            # Ribs and chest definition.
            if y in (2, 4, 6) and 0 < x < w - 1 and x != w // 2:
                c = mix(c, hexc(pal["dark"]), 0.45)
            if x == w // 2 and y > 0:
                c = mix(c, hexc(pal["dark"]), 0.3)
            if y <= 1 and x in (1, w - 2):
                c = mix(c, hexc(pal["hi"]), 0.4)
        return c
    if mat == "skin_fade":
        t = y / max(1, h - 1)
        return shade(mix(mottled(pal, rng, x, y, w, h, side), hexc(pal["deep"]), 0.15 + 0.7 * t), 1)
    if mat == "skin_dark":
        return shade(mix(hexc(pal["deep"]), hexc(pal["dark"]), rng.uniform(0, 0.4)), 1.1 if side == "top" else 1)
    if mat == "jaw":
        c = mottled(pal, rng, x, y, w, h, side)
        if side == "front" and y == h - 1:
            c = hexc(pal["deep"])
            if x in (0, w - 1):
                c = hexc("e8dcc0")   # fangs
        return c
    if mat == "beard":
        return shade(hexc(pal["tuft"]), rng.uniform(0.8, 1.2))
    if mat == "ear":
        c = mottled(pal, rng, x, y, w, h, side)
        if side == "front" and 0 < y < h - 1:
            c = mix(c, hexc(pal["dark"]), 0.6)
        return c
    if mat == "horn":
        t = 1 - y / max(1, h - 1)   # boxes grow upward from the pivot: y=0 is the tip
        return shade(mix(hexc(pal["horn"]), hexc(pal["horn_tip"]), t * 0.8), rng.uniform(0.9, 1.1))
    if mat == "claw":
        c = shade(hexc(pal["claw"]), rng.uniform(0.9, 1.3))
        return c
    if mat == "wing_bone":
        return shade(mix(hexc(pal["dark"]), hexc(pal["skin"]), 0.35), rng.uniform(0.85, 1.1))
    if mat == "membrane":
        if side not in ("front", "back"):
            return None
        # Tattered trailing edge: ragged notches along the bottom and outer edge.
        ragged = h - 1 - int(2.5 * (1 + math.sin(x * 1.7) * math.cos(x * 0.9))) - (2 if x < 3 else 0)
        if y > ragged:
            return None
        if rng.random() < 0.025 and y > h // 2:
            return None   # small holes
        c = mix(hexc(pal["membrane"]), hexc(pal["skin"]), 0.15 + 0.25 * (1 - y / h))
        if (x + y // 3) % 5 == 0:
            c = mix(c, hexc(pal["vein"]), 0.5)
            if pal.get("vein_glow") and y % 4 != 3:
                return glow(c)
        if side == "back":
            c = shade(c, 0.8)
        return shade(c, rng.uniform(0.94, 1.06))
    if mat == "tail":
        c = shade(hexc(pal["tail"]), rng.uniform(0.9, 1.08))
        # Dark rings where segments meet, and a pale ridge along the top.
        if side in ("left", "right", "top", "bottom"):
            if x in (0, w - 1) and side in ("left", "right"):
                c = mix(c, hexc(pal["tail_dark"]), 0.7)
        if side == "top":
            c = shade(c, 1.12)
        if side == "bottom":
            c = mix(c, hexc(pal["tail_dark"]), 0.4)
        if rng.random() < 0.08:
            c = mix(c, hexc(pal["tail_dark"]), 0.5)
        return c
    if mat == "tuft":
        if side not in ("front", "back", "left", "right"):
            return None
        if (x * 7 + y * 3) % 4 == 0 and y < h - 1:
            return None
        return shade(hexc(pal["tuft"]), rng.uniform(0.8, 1.3))
    if mat == "imp_head":
        c = mottled(pal, rng, x, y, w, h, side)
        if side == "front":
            if y == 2 and x in (1, 2, 4, 5):
                c = hexc(pal["deep"])                     # brow shadow
            if y == 3 and x in (1, 2, 4, 5):
                return glow(hexc(pal["eye"]) if x in (1, 5) else hexc(pal["eye_hi"]))
            if y == 4 and x == 3:
                c = mix(c, hexc(pal["dark"]), 0.6)        # nose
            if y == 5 and 1 <= x <= 5:
                c = hexc(pal["deep"])                     # snarl
            if y == 5 and x in (2, 4):
                c = hexc("e8dcc0")                        # fangs
            if y == 6 and 2 <= x <= 4:
                c = mix(c, hexc(pal["dark"]), 0.5)
        if side == "top" and rng.random() < 0.2:
            c = mix(c, hexc(pal["dark"]), 0.5)
        return c
    return hexc("ff00ff")


def paint(model, key, pal):
    tw, th = model["tex"]
    img = Image.new("RGBA", (tw, th), (0, 0, 0, 0))
    glow_img = Image.new("RGBA", (tw, th), (0, 0, 0, 0))
    rng = random.Random(f"{model['id']}:{key}")
    for _, b in all_boxes(model["parts"]):
        paint_box(img, glow_img, b, pal, rng)
    out = RES / key / f"{model['id']}.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out)
    glow_img.save(RES / key / f"{model['id']}_glow.png")
    return out


# ----------------------------------------------------------------------------- Java export

def jf(v):
    s = f"{v:.4f}".rstrip("0").rstrip(".")
    if s in ("-0", ""):
        s = "0"
    return s + "F"


def java_part(p, parent_var, lines, counter):
    var = f"p{counter[0]}"
    counter[0] += 1
    cubes = "CubeListBuilder.create()"
    for b in p["boxes"]:
        (x, y, z), (w, h, d) = b["origin"], b["size"]
        cubes += f".texOffs({b['uv'][0]}, {b['uv'][1]})"
        if b.get("mirror"):
            cubes += ".mirror()"
        grow = b.get("grow")
        deform = f", new CubeDeformation({jf(grow)})" if grow else ""
        cubes += f".addBox({jf(x)}, {jf(y)}, {jf(z)}, {jf(w)}, {jf(h)}, {jf(d)}{deform})"
        if b.get("mirror"):
            cubes += ".mirror(false)"
    (px, py, pz), (rx, ry, rz) = p["pivot"], p["rot"]
    lines.append(f"        PartDefinition {var} = {parent_var}.addOrReplaceChild(\"{p['name']}\", {cubes},\n"
                 f"                PartPose.offsetAndRotation({jf(px)}, {jf(py)}, {jf(pz)}, {jf(rx)}, {jf(ry)}, {jf(rz)}));")
    for c in p["children"]:
        java_part(c, var, lines, counter)


def write_java(models):
    methods = []
    for m in models:
        lines = []
        counter = [0]
        for p in m["parts"]:
            java_part(p, "root", lines, counter)
        body = "\n".join(lines)
        methods.append(f'''    /** The {m["id"]} body. */
    public static LayerDefinition {m["id"]}() {{
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
{body}
        return LayerDefinition.create(mesh, {m["tex"][0]}, {m["tex"][1]});
    }}''')
    src = f'''package com.warfront.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * GENERATED by tools/units.py from the unit model specs. Do not edit by hand: change the spec and
 * re-run the script, which also repaints the matching textures.
 */
public final class UnitGeometry {{
    private UnitGeometry() {{}}

{chr(10).join(methods)}
}}
'''
    JAVA.parent.mkdir(parents=True, exist_ok=True)
    JAVA.write_text(src)


if __name__ == "__main__":
    for m in MODELS:
        pack(m)
    for m in MODELS:
        if m["id"].startswith("imp"):
            for key, pal in IMP_PALETTES.items():
                print("painted", paint(m, key, pal))
    write_java(MODELS)
    JSON_OUT.parent.mkdir(parents=True, exist_ok=True)
    JSON_OUT.write_text(json.dumps(MODELS))
    print("wrote", JAVA, "and", JSON_OUT)
