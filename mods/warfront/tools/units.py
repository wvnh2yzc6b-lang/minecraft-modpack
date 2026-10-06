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

def imp():
    """Basic demon soldier. Small, gaunt, bat-winged, scorpion-tailed, digitigrade."""
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
        "id": "imp",
        "tex": [128, 128],
        "parts": [head, part("hat"), body, arm_r, mirror(arm_r), leg_r, mirror(leg_r)],
    }


MODELS = [imp()]


# ----------------------------------------------------------------------------- UV packing

def footprint(size):
    w, h, d = size
    return math.ceil(2 * (d + w)), math.ceil(d + h)


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
                  eye="ffd23a", eye_hi="fff4b8", tuft="2a1410"),
    # Burning Horde: darker, ember-veined
    "burning_horde": dict(skin="7a1712", dark="3a0a07", hi="e0602a", deep="160504", claw="0c0808", horn="1a1210",
                          horn_tip="ff7a1a", membrane="4a0d0a", vein="ff5a10", tail="8a6a48", tail_dark="3e2a18",
                          eye="ff7a00", eye_hi="ffe0a0", tuft="1a0a06"),
}


def paint_box(img, b, pal, rng):
    mat = b["mat"]
    for side, (x0, y0, w, h) in faces(b).items():
        for yy in range(h):
            for xx in range(w):
                c = material(mat, side, xx, yy, w, h, pal, rng)
                if c is not None:
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


def material(mat, side, x, y, w, h, pal, rng):
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
                c = hexc(pal["eye"]) if x in (1, 5) else hexc(pal["eye_hi"])
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
    rng = random.Random(f"{model['id']}:{key}")
    for _, b in all_boxes(model["parts"]):
        paint_box(img, b, pal, rng)
    out = RES / key / f"{model['id']}.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out)
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
        cubes += f".addBox({jf(x)}, {jf(y)}, {jf(z)}, {jf(w)}, {jf(h)}, {jf(d)})"
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
    for key, pal in IMP_PALETTES.items():
        print("painted", paint(MODELS[0], key, pal))
    write_java(MODELS)
    JSON_OUT.parent.mkdir(parents=True, exist_ok=True)
    JSON_OUT.write_text(json.dumps(MODELS))
    print("wrote", JAVA, "and", JSON_OUT)
