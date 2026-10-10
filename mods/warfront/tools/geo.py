"""High-detail model pipeline for GeckoLib (Bedrock .geo.json + .animation.json + painted textures).

Models are authored in GeckoLib's model space: Y up, feet at y=0, the front faces -z, units are model pixels (16 to a
block). Bones nest like Java ModelParts: a bone's pivot is an offset from its parent's pivot, its cubes are placed
relative to its own pivot, and any cube can carry its own rotation (that is what lets plates angle, skulls round off
and limbs taper). Rotations are degrees, applied Z then Y then X, as GeckoLib does.

On export the coordinates are written the Bedrock way GeckoLib reads them: x is mirrored (origin x = -(x + w), pivot
x negated) and rotations about x and y are negated. tools/codex/geomodel.js undoes exactly the same, so previews match
the game.

Faces use per-face UV at DENSITY texels per model pixel (double Minecraft's usual), painted with baked depth: a bevel
(lit top edge, shadowed lower edges), a top-to-bottom falloff, grain, and whatever the material adds.
"""
import json
import math
import random
from pathlib import Path

from PIL import Image

DENSITY = 2
SIDES = ("north", "south", "east", "west", "up", "down")


class Cube:
    def __init__(self, origin, size, mat, rot=None, pivot=None, inflate=0.0, alpha=255):
        self.origin, self.size, self.mat = list(origin), list(size), mat
        self.rot, self.pivot, self.inflate, self.alpha = rot, pivot, inflate, alpha


class Bone:
    def __init__(self, name, pivot=(0, 0, 0), rot=(0, 0, 0), cubes=(), children=()):
        self.name, self.pivot, self.rot = name, list(pivot), list(rot)
        self.cubes, self.children = list(cubes), list(children)

    def add(self, *items):
        for it in items:
            (self.children if isinstance(it, Bone) else self.cubes).append(it)
        return self


def cube(origin, size, mat, **kw):
    return Cube(origin, size, mat, **kw)


def bone(name, pivot=(0, 0, 0), rot=(0, 0, 0), cubes=(), children=()):
    return Bone(name, pivot, rot, cubes, children)


def mirror(b, swap=("_r", "_l")):
    """Mirrors a bone tree across x = 0 and renames _r to _l (or back)."""
    def rename(n):
        a, z = swap
        return n.replace(a, "\0").replace(z, a).replace("\0", z)
    out = Bone(rename(b.name), (-b.pivot[0], b.pivot[1], b.pivot[2]), (b.rot[0], -b.rot[1], -b.rot[2]))
    for c in b.cubes:
        (x, y, z), (w, h, d) = c.origin, c.size
        out.cubes.append(Cube((-(x + w), y, z), c.size, c.mat,
                              rot=None if c.rot is None else (c.rot[0], -c.rot[1], -c.rot[2]),
                              pivot=None if c.pivot is None else (-c.pivot[0], c.pivot[1], c.pivot[2]),
                              inflate=c.inflate, alpha=c.alpha))
    out.children = [mirror(ch, swap) for ch in b.children]
    return out


def walk(b, parent=None, base=(0, 0, 0)):
    """Yields (bone, parent name, absolute pivot) depth first."""
    abs_pivot = [base[i] + b.pivot[i] for i in range(3)]
    yield b, parent, abs_pivot
    for ch in b.children:
        yield from walk(ch, b.name, abs_pivot)


# ----------------------------------------------------------------------------- faces and UV packing

def face_dims(size, side):
    """Face width and height in model pixels, as seen from outside."""
    w, h, d = size
    return {"north": (w, h), "south": (w, h), "east": (d, h), "west": (d, h), "up": (w, d), "down": (w, d)}[side]


def texels(v):
    return max(1, math.ceil(v * DENSITY - 1e-6)) if v > 0 else 0


def pack(rects, width=512):
    """Shelf packing. rects: list of (key, w, h). Returns {key: (u, v)} and the texture height (a power of two)."""
    out, x, y, shelf = {}, 0, 0, 0
    for key, w, h in sorted(rects, key=lambda r: (-r[2], -r[1])):
        if x + w > width:
            x, y, shelf = 0, y + shelf, 0
        out[key] = (x, y)
        x += w
        shelf = max(shelf, h)
    height = 1
    while height < y + shelf:
        height *= 2
    return out, height


# ----------------------------------------------------------------------------- export

def _vec(v, nd=4):
    return [round(float(a), nd) for a in v]


def export(model_id, root, out_geo, painters, palettes, tex_dir, width=512, glow_mats=()):
    """Writes <out_geo> and one texture (plus glowmask) per palette under tex_dir/<skin>/<model_id>.png.

    painters: material -> fn(ctx) returning an RGBA tuple, None (transparent) or ("glow", rgba).
    """
    faces = []                       # (key, cube, side, tw, th)
    bones_json = []
    for b, parent, piv in walk(root):
        entry = {"name": b.name, "pivot": _vec([-piv[0], piv[1], piv[2]])}
        if parent:
            entry["parent"] = parent
        if any(b.rot):
            entry["rotation"] = _vec([-b.rot[0], -b.rot[1], b.rot[2]])
        cubes = []
        for ci, c in enumerate(b.cubes):
            ox, oy, oz = (piv[i] + c.origin[i] for i in range(3))
            w, h, d = c.size
            cj = {"origin": _vec([-(ox + w), oy, oz]), "size": _vec(c.size)}
            if c.inflate:
                cj["inflate"] = c.inflate
            if c.rot is not None:
                cp = [piv[i] + (c.pivot[i] if c.pivot else c.origin[i] + c.size[i] / 2) for i in range(3)]
                cj["pivot"] = _vec([-cp[0], cp[1], cp[2]])
                cj["rotation"] = _vec([-c.rot[0], -c.rot[1], c.rot[2]])
            uv = {}
            for side in SIDES:
                fw, fh = face_dims(c.size, side)
                if fw <= 0 or fh <= 0:
                    continue
                key = (b.name, ci, side)
                faces.append((key, c, side, texels(fw), texels(fh), (ox, oy, oz)))
                uv[side] = key
            cj["uv"] = uv
            cubes.append(cj)
        if cubes:
            entry["cubes"] = cubes
        bones_json.append(entry)

    placed, height = pack([(f[0], f[3], f[4]) for f in faces], width)
    for entry in bones_json:
        for cj in entry.get("cubes", []):
            cj["uv"] = {side: {"uv": list(placed[key]), "uv_size": [next(f[3] for f in faces if f[0] == key),
                                                                     next(f[4] for f in faces if f[0] == key)]}
                        for side, key in cj["uv"].items()}

    geo = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": f"geometry.{model_id}", "texture_width": width, "texture_height": height,
                        "visible_bounds_width": 4, "visible_bounds_height": 4, "visible_bounds_offset": [0, 1.5, 0]},
        "bones": bones_json}]}
    Path(out_geo).parent.mkdir(parents=True, exist_ok=True)
    Path(out_geo).write_text(json.dumps(geo, separators=(",", ":")))

    for skin, pal in palettes.items():
        img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
        glow = Image.new("RGBA", (width, height), (0, 0, 0, 0))
        rng = random.Random(f"{model_id}:{skin}")
        for key, c, side, tw, th, (ox, oy, oz) in faces:
            u0, v0 = placed[key]
            for py in range(th):
                for px in range(tw):
                    ctx = Ctx(c, side, px, py, tw, th, pal, rng, (ox, oy, oz))
                    col = painters[c.mat](ctx)
                    if col is None:
                        continue
                    if isinstance(col, tuple) and len(col) == 2 and col[0] == "glow":
                        col = col[1]
                        glow.putpixel((u0 + px, v0 + py), col)
                    img.putpixel((u0 + px, v0 + py), col)
        d = Path(tex_dir) / skin
        d.mkdir(parents=True, exist_ok=True)
        img.save(d / f"{model_id}.png")
        glow.save(d / f"{model_id}_glowmask.png")
    return geo


class Ctx:
    """What a painter knows about one texel: its face, where it sits on the face (0..1), and the cube."""

    def __init__(self, c, side, px, py, tw, th, pal, rng, origin):
        self.cube, self.side, self.x, self.y, self.w, self.h = c, side, px, py, tw, th
        self.pal, self.rng, self.origin = pal, rng, origin
        self.u = (px + 0.5) / tw
        self.v = (py + 0.5) / th           # 0 at the top of a side face

    @property
    def wall(self):
        return self.side in ("north", "south", "east", "west")

    def col(self, key):
        return hexc(self.pal[key])


# ----------------------------------------------------------------------------- colour helpers and depth shading

def hexc(s, a=255):
    s = s.lstrip("#")
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), a)


def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (a[3] if len(a) > 3 else 255,)


def shade(c, f):
    return tuple(max(0, min(255, round(c[i] * f))) for i in range(3)) + (c[3] if len(c) > 3 else 255,)


def with_alpha(c, a):
    return c[:3] + (a,)


def depth(ctx, c, bevel=0.16, falloff=0.14, grain=0.05):
    """Baked depth on any opaque surface: lit top edge, shadowed lower and side edges, darker toward the bottom of
    walls, darker undersides, and fine grain."""
    f = 1.0
    if ctx.wall:
        f *= 1.0 + falloff * 0.5 - falloff * ctx.v
        if ctx.y == 0 and ctx.h > 2:
            f *= 1.0 + bevel
        elif ctx.y == ctx.h - 1 and ctx.h > 2:
            f *= 1.0 - bevel
        if ctx.w > 2 and ctx.x in (0, ctx.w - 1):
            f *= 1.0 - bevel * 0.6
    elif ctx.side == "up":
        f *= 1.06
        if ctx.w > 2 and ctx.h > 2 and (ctx.x in (0, ctx.w - 1) or ctx.y in (0, ctx.h - 1)):
            f *= 1.0 + bevel * 0.5
    else:
        f *= 0.72
    f *= 1.0 + ctx.rng.uniform(-grain, grain)
    return shade(c, f)


# ----------------------------------------------------------------------------- animations

def export_animations(model_id, anims, out_path, step=0.05):
    """anims: name -> dict(length, loop, fn(t) -> {bone: {"rot": (x,y,z), "pos": (x,y,z)}}) in model space, degrees.
    Sampled every `step` seconds with linear keyframes, written the Bedrock way (x and y rotation negated, x position
    negated)."""
    out = {"format_version": "1.8.0", "animations": {}}
    for name, a in anims.items():
        length, frames = a["length"], {}
        n = max(1, round(length / step))
        for i in range(n + 1):
            t = round(i * length / n, 4)
            for b, ch in a["fn"](t).items():
                for kind, v in ch.items():
                    if kind == "rot":
                        v = (-v[0], -v[1], v[2])
                    else:
                        v = (-v[0], v[1], v[2])
                    frames.setdefault(b, {}).setdefault("rotation" if kind == "rot" else "position", {})[f"{t:g}"] = _vec(v, 3)
        out["animations"][f"animation.{model_id}.{name}"] = {
            "loop": a.get("loop", True), "animation_length": length, "bones": frames}
    Path(out_path).parent.mkdir(parents=True, exist_ok=True)
    Path(out_path).write_text(json.dumps(out, separators=(",", ":")))
