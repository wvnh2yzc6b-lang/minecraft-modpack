#!/usr/bin/env python3
"""
Generates Warfront's textures (procedural pixel art) and its JSON data/asset files.

    pip install pillow
    python3 tools/generate_assets.py

Outputs go to src/main/resources and are committed, so this only needs re-running when
something here changes.
"""
import json
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent / "src" / "main" / "resources"
ASSETS = ROOT / "assets" / "warfront"
DATA = ROOT / "data"
MODID = "warfront"



def write_json(path: Path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")


def hexc(s, a=255):
    s = s.lstrip("#")
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), a)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (c[3],)


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


def save(img: Image.Image, rel: str):
    p = ASSETS / "textures" / rel
    p.parent.mkdir(parents=True, exist_ok=True)
    img.save(p)


# --------------------------------------------------------------------------- items

def sprite(rows, palette):
    """Builds a 16x16 image from 16 strings; each char maps to a palette colour ('.' = clear)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row.ljust(16, ".")):
            if ch != "." and ch in palette:
                img.putpixel((x, y), palette[ch])
    return img


def item_textures():
    baton = [
        "................",
        "............GGG.",
        "...........GYYG.",
        "...........GYRG.",
        "............GG..",
        "...........WB...",
        "..........WB....",
        ".........WB.....",
        "........WB......",
        ".......WB.......",
        "......WB........",
        ".....GG.........",
        "....GYG.........",
        "....GG..........",
        "................",
        "................",
    ]
    save(sprite(baton, {"G": hexc("b8860b"), "Y": hexc("ffd700"), "R": hexc("c0392b"),
                        "W": hexc("8b5a2b"), "B": hexc("5c3a1a")}), "item/commander_baton.png")

    staff = [
        "................",
        "...........CCC..",
        "..........CLLLC.",
        "..........CLWLC.",
        "..........CLLLC.",
        "...........CCC..",
        "..........WB....",
        ".........WB.....",
        "........WB......",
        ".......WB.......",
        "......WB........",
        ".....WB.........",
        "....WB..........",
        "...WB...........",
        "................",
        "................",
    ]
    save(sprite(staff, {"C": hexc("2e8b57"), "L": hexc("7fff7f"), "W": hexc("d4f7d4"),
                        "B": hexc("6b4423")}), "item/healing_staff.png")

    mark = [
        "................",
        "................",
        ".....DDDDDD.....",
        "....DGGGGGGD....",
        "...DGYYYYYYGD...",
        "..DGYYRYYRYYGD..",
        "..DGYYRRRRYYGD..",
        "..DGYYYRRYYYGD..",
        "..DGYYYRRYYYGD..",
        "..DGYYRYYRYYGD..",
        "..DGYYYYYYYYGD..",
        "...DGYYYYYYGD...",
        "....DGGGGGGD....",
        ".....DDDDDD.....",
        "................",
        "................",
    ]
    save(sprite(mark, {"D": hexc("7a5300"), "G": hexc("c9a227"), "Y": hexc("f4d35e"),
                       "R": hexc("8b1a1a")}), "item/war_mark.png")
    # War Map: rolled parchment with a red route and an X.
    rows = [
        "................",
        "..WWWWWWWWWWWW..",
        ".WPPPPPPPPPPPPW.",
        ".WPPpPPPPPPPPPW.",
        ".WPPPrPPPPXPXPW.",
        ".WPPPPrPPPPXPPW.",
        ".WPpPPPrrPXPXPW.",
        ".WPPPPPPPrPPPPW.",
        ".WPPPPPPPPrPPPW.",
        ".WPPpPPPPPPrPPW.",
        ".WPPPPPPPPPPrPW.",
        ".WPPPPPpPPPPPPW.",
        ".WPPPPPPPPPPPPW.",
        "..WWWWWWWWWWWW..",
        "................",
        "................",
    ]
    save(sprite(rows, {"W": hexc("7a5a2a"), "P": hexc("e8d8a8"), "p": hexc("c8b888"), "r": hexc("8b1a1a"),
                       "X": hexc("c0281e")}), "item/war_map.png")

    horn = [
        "................",
        "................",
        "..........BB....",
        ".........BWWB...",
        "........BWWWB...",
        ".......BWWWB....",
        "......BWWWB.....",
        ".....BWWWB......",
        "....BWWWB.......",
        "...BWWGB........",
        "..BWWGGB........",
        "..BWGGB.........",
        "...BBB..........",
        "................",
        "................",
        "................",
    ]
    save(sprite(horn, {"B": hexc("4a3728"), "W": hexc("e8dcc0"), "G": hexc("d4a017")}),
         "item/war_horn.png")


def glider_texture():
    """Mana Glider: a swept wing of blue sailcloth on a wooden frame, mana crystal at the nose."""
    rows = [
        "................",
        ".......CC.......",
        "......CcCF......",
        ".....FBBBBF.....",
        "....FBbBBbBF....",
        "...FBbBBBBbBF...",
        "..FBbBBTTBBbBF..",
        ".FBbBBBTTBBBbBF.",
        "FBbBBBB..BBBBbBF",
        "FBBBBB....BBBBBF",
        "FBBB........BBBF",
        "FB............BF",
        "F..............F",
        "................",
        "................",
        "................",
    ]
    save(sprite(rows, {"C": hexc("6ee8ff"), "c": hexc("d8fbff"), "F": hexc("6a4424"), "B": hexc("2f5fa8"),
                       "b": hexc("4f86d0"), "T": hexc("d4a017")}), "item/mana_glider.png")


def flight_textures():
    """Rocket pack (two green tanks, brass bands, flaming nozzles), angel wings (white, gold edged), Wind Charm (a
    leaf-and-feather charm on a cord around a pale mana stone)."""
    pack = [
        "................",
        "......SS........",
        "......SS........",
        "...TTTTTTTTT....",
        "..TGgBTTBGgBT...",
        "..TGgGTTGGgGT...",
        "..TBBBTETBBBT...",
        "..TGgGTETGgGT...",
        "..TGgGTETGgGT...",
        "..TBBBTTTBBBT...",
        "..TGgGTTTGgGT...",
        "...NNN...NNN....",
        "...NNN...NNN....",
        "...FfF...FfF....",
        "....F.....F.....",
        "................",
    ]
    save(sprite(pack, {"S": hexc("2a2a2a"), "T": hexc("5a5f63"), "G": hexc("4f6b2a"), "g": hexc("6a8a3a"),
                       "B": hexc("b08a3a"), "E": hexc("ffb040"), "N": hexc("2a2a2a"), "F": hexc("ffb040"),
                       "f": hexc("fff0a0")}), "item/rocket_pack.png")
    wings = [
        "................",
        ".GG..........GG.",
        "GWWG........GWWG",
        "GWwWG......GWwWG",
        ".WwWWG....GWWwW.",
        ".WWwWWG..GWWwWW.",
        ".WwWWwWGGWwWWwW.",
        "..WWwWWWWWWwWW..",
        "..WwWWwWWwWWwW..",
        "..WWWwWWWWwWWW..",
        "...WwWW..WWwW...",
        "...WWw....wWW...",
        "....Ww....wW....",
        "....W......W....",
        "................",
        "................",
    ]
    save(sprite(wings, {"G": hexc("d4a017"), "W": hexc("f4f1e8"), "w": hexc("d6d0c0")}), "item/angel_wings.png")
    charm = [
        "................",
        ".......CC.......",
        "......C..C......",
        ".....C....C.....",
        ".....C....C.....",
        "......C..C......",
        ".....LLMMFF.....",
        "....LlLMMmFf....",
        "...LlL.MmM.Ff...",
        "...Ll..MMM..F...",
        "...L....M....F..",
        "........M.......",
        "................",
        "................",
        "................",
        "................",
    ]
    save(sprite(charm, {"C": hexc("8a5a30"), "L": hexc("3f8a3a"), "l": hexc("6ac25a"), "M": hexc("6ee8ff"),
                        "m": hexc("d8fbff"), "F": hexc("f4f1e8"), "f": hexc("c8c0b0")}), "item/wind_charm.png")


def charter_texture():
    """Village Charter: a rolled parchment with a red wax seal and a gold ribbon."""
    rows = [
        "................",
        "................",
        "..PPPPPPPPPPP...",
        ".pPLLLLLLLLLPp..",
        ".pPPPPPPPPPPPp..",
        "..PLLLLLLLLLP...",
        "..PPPPPPPPPPP...",
        "..PLLLLLLRRP....",
        "..PPPPPPRrrRP...",
        "..PLLLLLRrrR....",
        ".pPPPPPPPRRPPp..",
        ".pPPPPPPPGGPPp..",
        "..PPPPPPPG.G....",
        ".........G..G...",
        "................",
        "................",
    ]
    save(sprite(rows, {"P": hexc("eadbb0"), "p": hexc("c8b380"), "L": hexc("8a7a5a"), "R": hexc("a8231a"),
                       "r": hexc("d84a3a"), "G": hexc("d4a017")}), "item/village_charter.png")


def hammer_texture():
    """Mason's Hammer, carried by builders: a squared iron head on a wrapped wooden haft."""
    rows = [
        "................",
        ".........HHHH...",
        "........HhhhhH..",
        ".......HhhhhhhH.",
        "........HhhhhhH.",
        ".........HhhhH..",
        "........WwHHH...",
        ".......Ww.......",
        "......WwW.......",
        ".....LwW........",
        "....LlL.........",
        "...WwL..........",
        "..WwW...........",
        ".WwW............",
        ".WW.............",
        "................",
    ]
    save(sprite(rows, {"H": hexc("3e4046"), "h": hexc("8f939b"), "W": hexc("4a3020"), "w": hexc("8a5a34"),
                       "L": hexc("5a4630"), "l": hexc("b8a27a")}), "item/mason_hammer.png")


def mana_textures():
    shard = [
        "................",
        "................",
        ".........LL.....",
        "........LWBL....",
        ".......LWBBL....",
        "......LWBBDL....",
        ".....LWBBBDL....",
        "....LWBBBDL.....",
        "....LBBBBDL.....",
        "...LBBBBDL......",
        "...LBBBDDL......",
        "...LBBDDL.......",
        "....LDDL........",
        ".....LL.........",
        "................",
        "................",
    ]
    pal = {"L": hexc("1b4f8a"), "W": hexc("e6f7ff"), "B": hexc("3fa9f5"), "D": hexc("1f6fc0")}
    save(sprite(shard, pal), "item/mana_shard.png")
    crystal = [
        "................",
        ".......LL.......",
        "......LWBL......",
        ".....LWBBBL.....",
        "....LWBBBBDL....",
        "...LWBBBBBBDL...",
        "..LWWBBBBBBDDL..",
        "..LWBBBBBBBBDL..",
        "..LBBBBBBBBDDL..",
        "...LBBBBBBDDL...",
        "....LBBBBDDL....",
        ".....LBBDDL.....",
        "......LDDL......",
        ".......LL.......",
        "................",
        "................",
    ]
    pal2 = {"L": hexc("3a1b8a"), "W": hexc("f0e6ff"), "B": hexc("8f6bff"), "D": hexc("5a2fd0")}
    save(sprite(crystal, pal2), "item/mana_crystal.png")
    seeds = [
        "................",
        "................",
        "................",
        "......B.........",
        ".....BLB...B....",
        "......B...BLB...",
        "...........B....",
        "....B...........",
        "...BLB....B.....",
        "....B....BLB....",
        "..........B.....",
        "......B.........",
        ".....BLB........",
        "......B.........",
        "................",
        "................",
    ]
    save(sprite(seeds, {"B": hexc("2f6f9f"), "L": hexc("9fe8ff")}), "item/manabloom_seeds.png")

    def ore(base_img, name):
        rnd = random.Random(hash(name) & 0xffff)
        img = base_img.copy()
        for _ in range(5):
            cx, cy = rnd.randint(2, 13), rnd.randint(2, 13)
            for dx, dy in [(0, 0), (1, 0), (0, 1), (-1, 0), (0, -1)]:
                x, y = cx + dx, cy + dy
                if 0 <= x < 16 and 0 <= y < 16:
                    img.putpixel((x, y), hexc("9fe8ff") if (dx, dy) == (0, 0) else hexc("3fa9f5"))
        save(img, f"block/{name}.png")

    stone = Image.new("RGBA", (16, 16))
    noise_fill(stone, hexc("7f7f7f"), 0.12, 201)
    ore(stone, "mana_ore")
    deep = Image.new("RGBA", (16, 16))
    noise_fill(deep, hexc("4a4a50"), 0.12, 202)
    for y in range(0, 16, 4):
        for x in range(16):
            deep.putpixel((x, y), shade(deep.getpixel((x, y)), 0.8))
    ore(deep, "deepslate_mana_ore")

    # Crop stages: a cross-model plant that grows a glowing blue bloom.
    for stage in range(4):
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        height = 4 + stage * 3
        for y in range(16 - height, 16):
            for x in (5, 10):
                img.putpixel((x, y), hexc("2e7d32"))
        for i in range(stage + 1):
            ly = 15 - 2 - i * 3
            if ly < 0:
                continue
            img.putpixel((4, ly), hexc("43a047"))
            img.putpixel((11, ly), hexc("43a047"))
        if stage >= 2:
            top = 16 - height
            for dx, dy in [(0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)]:
                for cx in (5, 10):
                    x, y = cx + dx, top + dy
                    if 0 <= x < 16 and 0 <= y < 16:
                        img.putpixel((x, y), hexc("9fe8ff") if stage == 3 and (dx, dy) == (0, 0) else hexc("3fa9f5"))
        save(img, f"block/manabloom_stage{stage}.png")


# --------------------------------------------------------------------------- blocks

def noise_fill(img, base, var=0.12, seed=0, box=(0, 0, 16, 16)):
    rnd = random.Random(seed)
    for y in range(box[1], box[3]):
        for x in range(box[0], box[2]):
            img.putpixel((x, y), shade(base, 1 + rnd.uniform(-var, var)))


def brick_face(base, mortar, seed):
    img = Image.new("RGBA", (16, 16))
    noise_fill(img, base, 0.1, seed)
    for y in (0, 4, 8, 12):
        for x in range(16):
            img.putpixel((x, y), mortar)
    for row, y0 in enumerate((0, 4, 8, 12)):
        offs = (0, 8) if row % 2 == 0 else (4, 12)
        for x in offs:
            for y in range(y0, y0 + 4):
                img.putpixel((x, y), mortar)
    return img


def block_textures():
    # Arrow tower: stone brick with arrow slits and a battlement top.
    side = brick_face(hexc("8a8a8a"), hexc("5e5e5e"), 1)
    for y in range(5, 12):
        side.putpixel((7, y), hexc("1b1b1b"))
        side.putpixel((8, y), hexc("1b1b1b"))
    for x in range(6, 10):
        side.putpixel((x, 8), hexc("1b1b1b"))
    save(side, "block/arrow_tower_side.png")
    top = brick_face(hexc("9a9a9a"), hexc("5e5e5e"), 2)
    for x in range(4, 12):
        for y in range(4, 12):
            top.putpixel((x, y), hexc("6b4b2a") if (x + y) % 3 else hexc("553a20"))
    save(top, "block/arrow_tower_top.png")

    # Arcane spire: purple stone with glowing runes.
    side = brick_face(hexc("4b3a6b"), hexc("2c2040"), 3)
    rune = [(7, 3), (8, 3), (6, 4), (9, 4), (7, 5), (8, 5), (7, 6), (8, 6), (6, 9), (9, 9), (7, 10), (8, 10),
            (7, 11), (8, 11), (7, 12)]
    for p in rune:
        side.putpixel(p, hexc("c9a0ff"))
    save(side, "block/arcane_spire_side.png")
    top = Image.new("RGBA", (16, 16))
    noise_fill(top, hexc("3a2c55"), 0.1, 4)
    for x in range(16):
        for y in range(16):
            d = abs(x - 7.5) + abs(y - 7.5)
            if d < 3:
                top.putpixel((x, y), hexc("e6d0ff"))
            elif d < 5:
                top.putpixel((x, y), hexc("9b6bd6"))
    save(top, "block/arcane_spire_top.png")

    # Healing shrine: mossy stone with a green cross.
    side = brick_face(hexc("7d8a72"), hexc("4f5a47"), 5)
    for y in range(4, 13):
        for x in (7, 8):
            side.putpixel((x, y), hexc("5fd35f"))
    for x in range(5, 11):
        for y in (7, 8):
            side.putpixel((x, y), hexc("5fd35f"))
    save(side, "block/healing_shrine_side.png")
    top = Image.new("RGBA", (16, 16))
    noise_fill(top, hexc("6f7d63"), 0.1, 6)
    for x in range(16):
        for y in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 3.2:
                top.putpixel((x, y), hexc("b6ffb6"))
            elif d < 5:
                top.putpixel((x, y), hexc("3faa3f"))
    save(top, "block/healing_shrine_top.png")

    # War standard: wooden pole and a red flag with a gold emblem.
    pole = Image.new("RGBA", (16, 16))
    noise_fill(pole, hexc("6b4423"), 0.12, 7)
    for y in range(16):
        pole.putpixel((0, y), hexc("4a2f18"))
        pole.putpixel((15, y), hexc("4a2f18"))
    save(pole, "block/war_standard_pole.png")
    flag = Image.new("RGBA", (16, 16))
    noise_fill(flag, hexc("9e1b1b"), 0.06, 8)
    for x in range(16):
        flag.putpixel((x, 0), hexc("d4a017"))
        flag.putpixel((x, 15), hexc("d4a017"))
    emblem = [(7, 4), (8, 4), (6, 5), (9, 5), (5, 6), (10, 6), (7, 6), (8, 6), (7, 7), (8, 7), (6, 8), (9, 8),
              (7, 9), (8, 9), (7, 10), (8, 10), (6, 11), (7, 11), (8, 11), (9, 11)]
    for p in emblem:
        flag.putpixel(p, hexc("f4d35e"))
    save(flag, "block/war_standard_flag.png")


# --------------------------------------------------------------------------- soldier skins

# Player-skin regions (64x64 layout). (x, y, w, h)
HEAD = (0, 0, 32, 16)
HEAD_FRONT = (8, 8, 8, 8)
BODY = (16, 16, 24, 16)
R_ARM = (40, 16, 16, 16)
R_LEG = (0, 16, 16, 16)
L_LEG = (16, 48, 16, 16)
L_ARM = (32, 48, 16, 16)


def fill(img, box, color, seed, var=0.06):
    x, y, w, h = box
    noise_fill(img, color, var, seed, (x, y, x + w, y + h))


def skin(name, skin_c, hair_c, tunic_c, trim_c, pants_c, boots_c, eyes_c, extras=None, seed=0):
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    fill(img, HEAD, skin_c, seed)
    # Hair on top and back of head and the upper part of each side.
    fill(img, (8, 0, 8, 8), hair_c, seed + 1)              # top
    fill(img, (24, 8, 8, 8), hair_c, seed + 2)             # back
    fill(img, (0, 8, 8, 3), hair_c, seed + 3)              # right side
    fill(img, (16, 8, 8, 3), hair_c, seed + 4)             # left side
    fill(img, (8, 8, 8, 2), hair_c, seed + 5)              # fringe
    # Face.
    for x in (9, 10):
        img.putpixel((x, 12), (255, 255, 255, 255))
    for x in (13, 14):
        img.putpixel((x, 12), (255, 255, 255, 255))
    img.putpixel((10, 12), eyes_c)
    img.putpixel((13, 12), eyes_c)
    for x in range(10, 14):
        img.putpixel((x, 14), shade(skin_c, 0.7))

    # Body, arms and legs.
    fill(img, BODY, tunic_c, seed + 6)
    for x in range(16, 40):
        img.putpixel((x, 26), trim_c)                       # belt
        img.putpixel((x, 27), shade(trim_c, 0.8))
    fill(img, R_ARM, tunic_c, seed + 7)
    fill(img, L_ARM, tunic_c, seed + 8)
    for box in (R_ARM, L_ARM):                              # hands
        x, y, w, h = box
        fill(img, (x, y + 13, w, 3), skin_c, seed + 9)
    fill(img, R_LEG, pants_c, seed + 10)
    fill(img, L_LEG, pants_c, seed + 11)
    for box in (R_LEG, L_LEG):                              # boots
        x, y, w, h = box
        fill(img, (x, y + 11, w, 5), boots_c, seed + 12)

    if extras:
        extras(img)
    save(img, f"entity/soldier/{name}.png")


def beard(color):
    def apply(img):
        for x in range(8, 16):
            for y in range(13, 16):
                img.putpixel((x, y), shade(color, 1 + ((x * 7 + y) % 3 - 1) * 0.08))
        for x in range(10, 14):
            img.putpixel((x, 13), shade(color, 0.85))
        for x in range(0, 8):
            img.putpixel((x, 15), color)
        for x in range(16, 24):
            img.putpixel((x, 15), color)
    return apply


def elf_ears(skin_c):
    def apply(img):
        for y in (11, 12):
            img.putpixel((1, y), shade(skin_c, 0.9))
            img.putpixel((22, y), shade(skin_c, 0.9))
    return apply


def tusks(img):
    img.putpixel((10, 15), (240, 240, 220, 255))
    img.putpixel((13, 15), (240, 240, 220, 255))


def goblin_grin(img):
    """Slit pupils in yellow eyes and a wide, toothy grin."""
    for x in (10, 13):
        img.putpixel((x, 12), hexc("2a1a00"))
    for x in (9, 14):
        img.putpixel((x, 12), hexc("ffd23a"))
    for x in range(9, 15):
        img.putpixel((x, 14), hexc("3a1a12"))
    for x in (10, 12):
        img.putpixel((x, 14), (236, 228, 200, 255))


def warpaint(img):
    for y in (11, 12, 13):
        img.putpixel((8, y), hexc("8b0000"))
        img.putpixel((15, y), hexc("8b0000"))
    tusks(img)


def soldier_skins():
    skin("human", hexc("e0ac7e"), hexc("5a3a1e"), hexc("2f4f9f"), hexc("d4a017"), hexc("3b3b3b"),
         hexc("4a2f18"), hexc("2a5db0"), seed=10)
    skin("elf", hexc("f2d6b8"), hexc("e8d27a"), hexc("2f7a3f"), hexc("c0c0c0"), hexc("3f5f3f"),
         hexc("5b4a2a"), hexc("2e8b57"), extras=elf_ears(hexc("f2d6b8")), seed=20)
    skin("dwarf", hexc("d39b78"), hexc("8b3a1a"), hexc("6b4a2a"), hexc("b8860b"), hexc("4a3a2a"),
         hexc("2a1a0a"), hexc("3a2a1a"), extras=beard(hexc("a0461e")), seed=30)
    skin("orc", hexc("6b8e3a"), hexc("1e1e1e"), hexc("6b3a2a"), hexc("9a9a9a"), hexc("3a2a1a"),
         hexc("2a1a0a"), hexc("c0392b"), extras=tusks, seed=40)
    skin("demon", hexc("b22222"), hexc("1a0a0a"), hexc("2b0f0f"), hexc("ff8c00"), hexc("1f0a0a"),
         hexc("0f0505"), hexc("ffd700"), extras=horns(hexc("2a2a2a")), seed=70)
    skin("angel", hexc("f7e7d4"), hexc("ffe680"), hexc("f2f2f2"), hexc("d4a017"), hexc("e6e6e6"),
         hexc("c9a227"), hexc("6fb7ff"), extras=halo, seed=80)
    # Hive: sculk-dark teal chitin with glowing cyan veins, like the Warden and the deep dark.
    skin("hive", hexc("143a44"), hexc("08191f"), hexc("0e2a32"), hexc("29dfeb"), hexc("0a1f26"),
         hexc("050f14"), hexc("49ffc8"), extras=sculk(hexc("29dfeb")), seed=90)

    # Goblins: the orcs' farmers and builders. Small, sallow green, in patched brown work clothes.
    skin("goblin", hexc("9aae44"), hexc("2a2a1a"), hexc("6a5236"), hexc("8a6a3a"), hexc("4a3a22"),
         hexc("2a1a0a"), hexc("ffd23a"), extras=goblin_grin, seed=45)

    # NPC factions (file names match NpcFaction enum names in lower case).
    skin("marauders", hexc("5a7a32"), hexc("111111"), hexc("7a1f1f"), hexc("2b2b2b"), hexc("2b1a10"),
         hexc("1a0f08"), hexc("ff3b1f"), extras=warpaint, seed=50)
    skin("black_legion", hexc("c9a080"), hexc("1a1a1a"), hexc("1f1f24"), hexc("8b0000"), hexc("26262b"),
         hexc("111114"), hexc("8b0000"), seed=60)
    skin("burning_horde", hexc("8b1a1a"), hexc("0a0a0a"), hexc("3b1a0a"), hexc("ff4500"), hexc("240a05"),
         hexc("120503"), hexc("ff6a00"), extras=horns(hexc("111111")), seed=100)
    # The Swarm: the Hive's wild kin, black chitin veined with acid green.
    skin("the_swarm", hexc("1c2622"), hexc("0b1210"), hexc("141d1a"), hexc("8aff3a"), hexc("101714"),
         hexc("070b0a"), hexc("b8ff4a"), extras=sculk(hexc("8aff3a")), seed=110)
    skin("silverwood_reavers", hexc("e8d8c8"), hexc("dcdcdc"), hexc("23402c"), hexc("a8a8a8"), hexc("1e2e22"),
         hexc("3a2e1a"), hexc("9fe8ff"), extras=elf_ears(hexc("e8d8c8")), seed=120)
    skin("ironbeard_clan", hexc("c98a68"), hexc("4a4a4a"), hexc("3f4a5a"), hexc("c0c0c0"), hexc("2f3a44"),
         hexc("1a1a1a"), hexc("2a2a2a"), extras=beard(hexc("6a6a6a")), seed=130)
    skin("fallen_host", hexc("cfc6d8"), hexc("2b2b3b"), hexc("3b3450"), hexc("6b5a9b"), hexc("2a2440"),
         hexc("15121f"), hexc("c050ff"), extras=halo_dark, seed=140)


def horns(color):
    def apply(img):
        # Horns on the hat layer, above the temples.
        for (x, y) in [(41, 0), (42, 0), (46, 0), (45, 0), (41, 1), (46, 1)]:
            img.putpixel((x, y), color)
        for (x, y) in [(40, 8), (40, 9), (47, 8), (47, 9), (55, 8), (55, 9), (48, 8), (48, 9)]:
            img.putpixel((x, y), color)
    return apply


def _ring(img, color):
    for i in range(8):
        img.putpixel((40 + i, 0), color)
        img.putpixel((40 + i, 7), color)
        img.putpixel((40, i), color)
        img.putpixel((47, i), color)


def halo(img):
    _ring(img, hexc("ffd700"))


def halo_dark(img):
    _ring(img, hexc("6b2fa0"))


def mandibles(img):
    jaw = hexc("1e1e1e")
    for (x, y) in [(9, 15), (10, 15), (13, 15), (14, 15), (10, 14), (13, 14)]:
        img.putpixel((x, y), jaw)
    # Carapace segments on the chest.
    for x in range(20, 28):
        for y in (21, 24):
            img.putpixel((x, y), shade(img.getpixel((x, y)), 0.6))


def sculk(vein):
    """Mandibles and chest plates, plus thin glowing veins branching over the chitin like sculk."""
    def apply(img):
        mandibles(img)
        dim = shade(vein, 0.55)
        # Veins: (x, y) runs on the body front, arms and legs.
        for x, y in [(21, 22), (22, 23), (22, 24), (23, 25), (26, 21), (25, 22), (25, 23), (24, 24),
                     (45, 22), (45, 23), (46, 24), (46, 25), (37, 54), (37, 55), (38, 56),
                     (5, 22), (5, 23), (6, 24), (21, 54), (21, 55), (22, 56)]:
            img.putpixel((x, y), vein)
        for x, y in [(20, 21), (27, 22), (44, 21), (47, 26), (36, 53), (4, 21), (7, 25), (20, 53)]:
            img.putpixel((x, y), dim)
    return apply


# --------------------------------------------------------------------------- json

def models_and_states():
    towers = ["arrow_tower", "arcane_spire", "healing_shrine"]
    bottoms = {"arrow_tower": "minecraft:block/stone_bricks", "arcane_spire": "minecraft:block/obsidian",
               "healing_shrine": "minecraft:block/mossy_stone_bricks"}
    for t in towers:
        write_json(ASSETS / "blockstates" / f"{t}.json", {"variants": {"": {"model": f"{MODID}:block/{t}"}}})
        write_json(ASSETS / "models" / "block" / f"{t}.json", {
            "parent": "minecraft:block/cube_bottom_top",
            "textures": {"top": f"{MODID}:block/{t}_top", "bottom": bottoms[t], "side": f"{MODID}:block/{t}_side"},
        })
        write_json(ASSETS / "models" / "item" / f"{t}.json", {"parent": f"{MODID}:block/{t}"})

    write_json(ASSETS / "blockstates" / "war_standard.json",
               {"variants": {"": {"model": f"{MODID}:block/war_standard"}}})
    write_json(ASSETS / "models" / "block" / "war_standard.json", {
        "parent": "minecraft:block/block",
        "textures": {"pole": f"{MODID}:block/war_standard_pole", "flag": f"{MODID}:block/war_standard_flag",
                     "particle": f"{MODID}:block/war_standard_flag"},
        "elements": [
            {"from": [7, 0, 7], "to": [9, 16, 9], "faces": {
                d: {"uv": [7, 0, 9, 16], "texture": "#pole"} for d in ("north", "south", "east", "west")}
             | {"up": {"uv": [7, 7, 9, 9], "texture": "#pole"}, "down": {"uv": [7, 7, 9, 9], "texture": "#pole"}}},
            {"from": [5, 15, 7.5], "to": [11, 16, 8.5], "faces": {
                d: {"uv": [0, 0, 6, 1], "texture": "#pole"} for d in ("north", "south", "east", "west", "up", "down")}},
            {"from": [9, 4, 7.75], "to": [16, 15, 8.25], "faces": {
                "north": {"uv": [0, 1, 7, 12], "texture": "#flag"},
                "south": {"uv": [0, 1, 7, 12], "texture": "#flag"},
                "east": {"uv": [0, 1, 1, 12], "texture": "#flag"},
                "west": {"uv": [0, 1, 1, 12], "texture": "#flag"},
                "up": {"uv": [0, 0, 7, 1], "texture": "#flag"},
                "down": {"uv": [0, 0, 7, 1], "texture": "#flag"}}},
        ],
        "display": {
            "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
            "ground": {"translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
        },
    })
    write_json(ASSETS / "models" / "item" / "war_standard.json", {"parent": f"{MODID}:block/war_standard"})

    for item, parent in [("commander_baton", "handheld"), ("healing_staff", "handheld"), ("mason_hammer", "handheld"),
                         ("war_mark", "generated"), ("war_map", "generated"), ("war_horn", "generated"), ("mana_glider", "generated"),
                         ("rocket_pack", "generated"), ("angel_wings", "generated"), ("wind_charm", "generated"),
                         ("village_charter", "generated")]:
        write_json(ASSETS / "models" / "item" / f"{item}.json",
                   {"parent": f"minecraft:item/{parent}", "textures": {"layer0": f"{MODID}:item/{item}"}})
    write_json(ASSETS / "models" / "item" / "soldier_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})

    for item in ("mana_shard", "mana_crystal", "manabloom_seeds"):
        write_json(ASSETS / "models" / "item" / f"{item}.json",
                   {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MODID}:item/{item}"}})
    for ore in ("mana_ore", "deepslate_mana_ore"):
        write_json(ASSETS / "blockstates" / f"{ore}.json", {"variants": {"": {"model": f"{MODID}:block/{ore}"}}})
        write_json(ASSETS / "models" / "block" / f"{ore}.json",
                   {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MODID}:block/{ore}"}})
        write_json(ASSETS / "models" / "item" / f"{ore}.json", {"parent": f"{MODID}:block/{ore}"})
    stage_of_age = [0, 0, 1, 1, 2, 2, 2, 3]
    write_json(ASSETS / "blockstates" / "manabloom.json", {"variants": {
        f"age={a}": {"model": f"{MODID}:block/manabloom_stage{st}"} for a, st in enumerate(stage_of_age)}})
    for st in range(4):
        write_json(ASSETS / "models" / "block" / f"manabloom_stage{st}.json", {
            "parent": "minecraft:block/crop", "render_type": "minecraft:cutout",
            "textures": {"crop": f"{MODID}:block/manabloom_stage{st}"}})


def effect_icons():
    """18x18 mob effect icons."""
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    dark, red, hot = hexc("5a0d08"), hexc("b8231a"), hexc("ff6a3d")
    # three slanted claw slashes, dark edge, red body, hot core
    for i, x0 in enumerate((3, 7, 11)):
        for t in range(12):
            x, y = x0 + t // 3, 3 + t
            img.putpixel((x, y), dark)
            img.putpixel((min(17, x + 1), y), red)
            if 2 <= t <= 9:
                img.putpixel((min(17, x + 2), y), hot if i == 1 else red)
    save(img, "mob_effect/frenzy.png")

    # Well Fed: a loaf of bread.
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    crust, crumb, dark = hexc("c98a3a"), hexc("f0c070"), hexc("7a4a18")
    for x in range(3, 15):
        for y in range(6, 13):
            edge = x in (3, 14) or y in (6, 12)
            img.putpixel((x, y), dark if edge else crust)
    for x in (6, 9, 12):
        for y in range(7, 11):
            img.putpixel((x, y), crumb)
    save(img, "mob_effect/well_fed.png")

    # Oath of Stone: a gray shield of stone blocks with a pale rune.
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    rim, stone, hi, rune = hexc("3a3f46"), hexc("8a9099"), hexc("b8c0ca"), hexc("e8f4ff")
    for y in range(2, 16):
        half = 6 if y < 11 else 6 - (y - 10)
        for x in range(9 - half, 9 + half):
            edge = x in (9 - half, 9 + half - 1) or y in (2, 15)
            img.putpixel((x, y), rim if edge else (hi if (x + y) % 5 == 0 else stone))
    for x, y in ((8, 5), (9, 5), (8, 6), (8, 7), (9, 8), (10, 9), (8, 9), (8, 10), (8, 11)):
        img.putpixel((x, y), rune)
    save(img, "mob_effect/oath_of_stone.png")

    # Rallied: a gold banner on a pole.
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    pole, cloth, hi = hexc("6a4424"), hexc("e2b55a"), hexc("ffe07a")
    for y in range(1, 17):
        img.putpixel((4, y), pole)
    for y in range(2, 11):
        for x in range(5, 15 - (1 if y > 8 else 0)):
            img.putpixel((x, y), hi if (x + y) % 4 == 0 else cloth)
    for x, y in ((9, 5), (10, 5), (9, 6), (10, 6)):
        img.putpixel((x, y), hexc("b8231a"))
    save(img, "mob_effect/rallied.png")

    # Corroded: green acid drops eating a gray plate.
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    for x in range(3, 15):
        for y in range(4, 14):
            img.putpixel((x, y), hexc("7a8088") if (x + y) % 5 else hexc("5a6068"))
    for x, y in ((5, 3), (5, 4), (6, 5), (10, 6), (10, 7), (11, 8), (8, 10), (8, 11), (9, 12), (12, 11), (12, 12)):
        img.putpixel((x, y), hexc("7fd02a"))
    save(img, "mob_effect/corroded.png")

    # Rank chevron: a light plate with a darker rim, tinted to the race's color when drawn.
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(16):
        for y in range(16):
            rim = x in (0, 15) or y in (0, 15)
            img.putpixel((x, y), hexc("b0b0b0") if rim else hexc("ffffff"))
    save(img, "entity/insignia.png")


def lang():
    names = {
        "itemGroup.warfront": "Warfront",
        "key.categories.warfront": "Warfront",
        "key.warfront.test_panel": "Test Panel (test mode)",
        "key.warfront.recall": "Recall home (during an attack)",
        "key.warfront.war_table": "War Table",
        "block.warfront.mess_hall": "Mess Hall",
        "block.warfront.raid_chest": "Raid Chest",
        "block.warfront.fortress_core": "Warlord's Seat",
        "block.warfront.trophy_banner": "Trophy Banner",
        "item.warfront.skullsplitter": "Skullsplitter",
        "item.warfront.legion_warplate": "Legion Warplate",
        "item.warfront.emberbrand": "Emberbrand",
        "item.warfront.broodfang": "Broodfang",
        "item.warfront.thornbow": "Thornbow",
        "item.warfront.runehammer": "Runehammer",
        "item.warfront.fallen_halo": "Fallen Halo",
        "item.warfront.seal_of_seven": "Seal of the Seven",
        "effect.warfront.trophy_marauders": "Marauder Trophy",
        "effect.warfront.trophy_black_legion": "Legion Trophy",
        "effect.warfront.trophy_burning_horde": "Horde Trophy",
        "effect.warfront.trophy_the_swarm": "Swarm Trophy",
        "effect.warfront.trophy_silverwood_reavers": "Silverwood Trophy",
        "effect.warfront.trophy_ironbeard_clan": "Ironbeard Trophy",
        "effect.warfront.well_fed": "Well Fed",
        "entity.warfront.advisor": "Advisor",
        "entity.warfront.merchant": "Traveling Merchant",
        "block.warfront.arrow_tower": "Arrow Tower",
        "block.warfront.arcane_spire": "Arcane Spire",
        "block.warfront.healing_shrine": "Healing Shrine",
        "block.warfront.war_standard": "War Standard",
        "item.warfront.commander_baton": "Commander's Baton",
        "item.warfront.healing_staff": "Healing Staff",
        "item.warfront.war_mark": "War Mark",
        "item.warfront.war_map": "War Map",
        "item.warfront.war_horn": "War Horn",
        "item.warfront.soldier_spawn_egg": "Raider Spawn Egg",
        "entity.warfront.soldier": "Soldier",
        "block.warfront.mana_ore": "Mana Ore",
        "block.warfront.deepslate_mana_ore": "Deepslate Mana Ore",
        "block.warfront.manabloom": "Manabloom",
        "item.warfront.mana_shard": "Mana Shard",
        "item.warfront.mana_crystal": "Mana Crystal",
        "item.warfront.manabloom_seeds": "Manabloom Seeds",
        "item.warfront.mason_hammer": "Mason's Hammer",
        "block.warfront.mana_well": "Mana Well",
        "block.warfront.mana_pylon": "Mana Pylon",
        "block.warfront.mana_brazier": "Mana Brazier",
        "block.warfront.summoning_altar": "Summoning Altar",
        "effect.warfront.frenzy": "Frenzy",
        "effect.warfront.oath_of_stone": "Oath of Stone",
        "effect.warfront.rallied": "Rallied",
        "effect.warfront.corroded": "Corroded",
        **{f"block.warfront.{t}": v[1] for t, v in RACE_TOWERS.items()},
        "item.warfront.village_charter": "Village Charter",
        "key.warfront.race_power": "Race power (Rally, Oath, Judgment, Swarm)",
        "item.warfront.mana_glider": "Mana Glider",
        "item.warfront.rocket_pack": "Orc Rocket Pack",
        "item.warfront.angel_wings": "Angel Wings",
        "item.warfront.wind_charm": "Wind Charm",
        "item.warfront.rune_drill": "Rune Drill",
        "block.warfront.rune_stone": "Rune Stone",
        "block.warfront.rune_stone_stairs": "Rune Stone Stairs",
        "block.warfront.rune_stone_slab": "Rune Stone Slab",
        "block.warfront.rune_stone_wall": "Rune Stone Wall",
        "block.warfront.spike_floor": "Spike Floor",
        "block.warfront.rune_mine": "Rune Mine",
        "block.warfront.flame_vent": "Flame Vent",
    }
    write_json(ASSETS / "lang" / "en_us.json", names)


def item(id_, count=1):
    return {"id": id_, "count": count}


def ing(x):
    return {"tag": x[1:]} if x.startswith("#") else {"item": x}


def shaped(name, pattern, key, result, count=1):
    write_json(DATA / MODID / "recipe" / f"{name}.json", {
        "type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
        "key": {k: ing(v) for k, v in key.items()}, "result": item(result, count)})


def shapeless(name, ingredients, result, count=1):
    write_json(DATA / MODID / "recipe" / f"{name}.json", {
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [ing(i) for i in ingredients], "result": item(result, count)})


def recipes():
    W = "warfront:war_mark"
    shaped("mana_glider", [" C ", "LSL", "S S"], {"C": "warfront:mana_crystal", "L": "minecraft:leather",
                                                    "S": "minecraft:stick"}, "warfront:mana_glider")
    shaped("rocket_pack", ["ICI", "IFI", "B B"], {"I": "minecraft:iron_ingot", "C": "warfront:mana_crystal",
                                                  "F": "minecraft:blast_furnace", "B": "minecraft:copper_ingot"},
           "warfront:rocket_pack")
    shaped("angel_wings", ["FGF", "FCF", "F F"], {"F": "minecraft:feather", "G": "minecraft:gold_ingot",
                                                  "C": "warfront:mana_crystal"}, "warfront:angel_wings")
    shaped("wind_charm", [" S ", "LCL", " F "], {"S": "minecraft:string", "L": "#minecraft:leaves",
                                                 "C": "warfront:mana_crystal", "F": "minecraft:feather"}, "warfront:wind_charm")
    shaped("commander_baton", ["  G", " S ", "S  "], {"G": "minecraft:gold_ingot", "S": "minecraft:stick"},
           "warfront:commander_baton")
    shaped("healing_staff", ["  M", " S ", "S  "], {"M": "minecraft:glistering_melon_slice", "S": "minecraft:stick"},
           "warfront:healing_staff")
    shapeless("war_horn", ["minecraft:bone", "minecraft:bone", "minecraft:leather", "minecraft:gold_ingot"],
              "warfront:war_horn")
    shaped("war_standard", [" B ", "GSG", " S "],
           {"B": "#minecraft:banners", "G": "minecraft:gold_ingot", "S": "minecraft:stick"}, "warfront:war_standard")
    shaped("arrow_tower", ["SBS", "SDS", "SSS"],
           {"S": "minecraft:stone_bricks", "B": "minecraft:bow", "D": "minecraft:dispenser"}, "warfront:arrow_tower")
    shaped("arcane_spire", ["AWA", "WEW", "OOO"],
           {"A": "minecraft:amethyst_shard", "W": W, "E": "minecraft:ender_eye", "O": "minecraft:obsidian"},
           "warfront:arcane_spire")
    shaped("healing_shrine", ["GWG", "WAW", "MMM"],
           {"G": "minecraft:glistering_melon_slice", "W": W, "A": "minecraft:golden_apple",
            "M": "minecraft:mossy_stone_bricks"}, "warfront:healing_shrine")
    shapeless("emerald_from_war_marks", [W, W, W, W], "minecraft:emerald")
    M = "warfront:mana_shard"
    C = "warfront:mana_crystal"
    shapeless("manabloom_seeds", [M, "minecraft:wheat_seeds"], "warfront:manabloom_seeds", 2)
    shapeless("mana_shards_from_war_marks", [W, W, W], M, 2)
    for kind, time in (("smelting", 200), ("blasting", 100)):
        for ore in ("mana_ore", "deepslate_mana_ore"):
            write_json(DATA / MODID / "recipe" / f"mana_crystal_from_{kind}_{ore}.json", {
                "type": f"minecraft:{kind}", "category": "misc", "ingredient": {"item": f"warfront:{ore}"},
                "result": {"id": C, "count": 1}, "experience": 0.7, "cookingtime": time})

    # Mana infrastructure: built from crystals mined from Mana Ore.
    shaped("mana_well", ["SCS", "CUC", "SSS"],
           {"S": "minecraft:stone_bricks", "C": C, "U": "minecraft:cauldron"}, "warfront:mana_well")
    shaped("mana_pylon", [" C ", " W ", "SSS"],
           {"C": C, "W": "minecraft:stone_brick_wall", "S": "minecraft:stone_brick_slab"}, "warfront:mana_pylon")
    shaped("mana_brazier", ["IMI", " I ", "SSS"],
           {"I": "minecraft:iron_ingot", "M": M, "S": "minecraft:stone_brick_slab"}, "warfront:mana_brazier")
    shaped("mess_hall", ["WBW", "PSP", "PCP"],
           {"W": "minecraft:wheat", "B": "minecraft:bread", "P": "#minecraft:planks", "S": "minecraft:smoker",
            "C": "minecraft:chest"}, "warfront:mess_hall")
    shaped("summoning_altar", ["CBC", "GOG", "SSS"],
           {"C": C, "B": "minecraft:book", "G": "minecraft:gold_ingot", "O": "minecraft:obsidian",
            "S": "minecraft:stone_bricks"}, "warfront:summoning_altar")


def loot_and_tags():
    blocks = ["arrow_tower", "arcane_spire", "healing_shrine", "war_standard",
              "mana_well", "mana_pylon", "mana_brazier", "summoning_altar", "mess_hall"]
    # The trophy banner keeps its faction when broken and picked up again.
    write_json(DATA / MODID / "loot_table" / "blocks" / "trophy_banner.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"{MODID}:trophy_banner",
                    "functions": [{"function": "minecraft:copy_state", "block": f"{MODID}:trophy_banner", "properties": ["faction"]}]}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    for b in blocks:
        write_json(DATA / MODID / "loot_table" / "blocks" / f"{b}.json", {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "bonus_rolls": 0,
                       "entries": [{"type": "minecraft:item", "name": f"{MODID}:{b}"}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}],
            "random_sequence": f"{MODID}:blocks/{b}"})
    ores = ["mana_ore", "deepslate_mana_ore"]
    write_json(DATA / "minecraft" / "tags" / "block" / "mineable" / "pickaxe.json",
               {"replace": False, "values": [f"{MODID}:{b}" for b in blocks[:3] + blocks[4:8] + ores + TUNNEL_BLOCKS
                                             + list(RACE_TOWERS)]})
    # Floors a summoning altar can stand on: any brick or stone-brick block, so every race can build in its style.
    write_json(DATA / MODID / "tags" / "block" / "altar_base.json", {"replace": False, "values": [
        "#minecraft:stone_bricks", "minecraft:polished_blackstone_bricks", "minecraft:cracked_polished_blackstone_bricks",
        "minecraft:deepslate_bricks", "minecraft:cracked_deepslate_bricks", "minecraft:deepslate_tiles",
        "minecraft:cracked_deepslate_tiles", "minecraft:nether_bricks", "minecraft:cracked_nether_bricks",
        "minecraft:red_nether_bricks", "minecraft:bricks", "minecraft:mud_bricks", "minecraft:quartz_bricks",
        "minecraft:end_stone_bricks", "minecraft:prismarine_bricks", "minecraft:tuff_bricks"]})
    write_json(DATA / "minecraft" / "tags" / "block" / "needs_iron_tool.json",
               {"replace": False, "values": [f"{MODID}:{o}" for o in ores + RUNE_FAMILY]})
    write_json(DATA / "c" / "tags" / "block" / "ores.json", {"replace": False, "values": [f"{MODID}:{o}" for o in ores]})
    write_json(DATA / "c" / "tags" / "item" / "ores.json", {"replace": False, "values": [f"{MODID}:{o}" for o in ores]})
    write_json(DATA / "minecraft" / "tags" / "item" / "villager_plantable_seeds.json",
               {"replace": False, "values": [f"{MODID}:manabloom_seeds"]})

    silk = {"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [
        {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}
    for o in ores:
        write_json(DATA / MODID / "loot_table" / "blocks" / f"{o}.json", {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:alternatives", "children": [
                {"type": "minecraft:item", "name": f"{MODID}:{o}", "conditions": [silk]},
                {"type": "minecraft:item", "name": f"{MODID}:mana_crystal", "functions": [
                    {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune",
                     "formula": "minecraft:ore_drops"},
                    {"function": "minecraft:explosion_decay"}]}]}]}],
            "random_sequence": f"{MODID}:blocks/{o}"})

    mature = {"condition": "minecraft:block_state_property", "block": f"{MODID}:manabloom",
              "properties": {"age": "7"}}
    write_json(DATA / MODID / "loot_table" / "blocks" / "manabloom.json", {
        "type": "minecraft:block",
        "pools": [
            {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:alternatives", "children": [
                {"type": "minecraft:item", "name": f"{MODID}:mana_shard", "conditions": [mature], "functions": [
                    {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 3}}]},
                {"type": "minecraft:item", "name": f"{MODID}:manabloom_seeds"}]}]},
            {"rolls": 1, "bonus_rolls": 0, "conditions": [mature], "entries": [
                {"type": "minecraft:item", "name": f"{MODID}:manabloom_seeds", "functions": [
                    {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune",
                     "formula": "minecraft:binomial_with_bonus_count", "parameters": {"extra": 1, "probability": 0.57}}]}]}],
        "functions": [{"function": "minecraft:explosion_decay"}],
        "random_sequence": f"{MODID}:blocks/manabloom"})

    # Mana ore worldgen.
    wg = DATA / MODID / "worldgen"
    write_json(wg / "configured_feature" / "mana_ore.json", {"type": "minecraft:ore", "config": {
        "size": 7, "discard_chance_on_air_exposure": 0.0, "targets": [
            {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"},
             "state": {"Name": f"{MODID}:mana_ore"}},
            {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables"},
             "state": {"Name": f"{MODID}:deepslate_mana_ore"}}]}})
    write_json(wg / "placed_feature" / "mana_ore.json", {"feature": f"{MODID}:mana_ore", "placement": [
        {"type": "minecraft:count", "count": 9},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:height_range", "height": {"type": "minecraft:trapezoid",
                                                       "min_inclusive": {"above_bottom": 0},
                                                       "max_inclusive": {"absolute": 80}}},
        {"type": "minecraft:biome"}]})
    write_json(DATA / MODID / "neoforge" / "biome_modifier" / "mana_ore.json", {
        "type": "neoforge:add_features", "biomes": "#minecraft:is_overworld",
        "features": f"{MODID}:mana_ore", "step": "underground_ores"})

    # Manabloom seeds drop from grass (5%).
    write_json(DATA / MODID / "loot_table" / "gameplay" / "manabloom_seeds.json", {
        "type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
            {"type": "minecraft:item", "name": f"{MODID}:manabloom_seeds"}]}]})
    for grass in ("short_grass", "tall_grass", "fern"):
        write_json(DATA / MODID / "loot_modifiers" / f"seeds_from_{grass}.json", {
            "type": "neoforge:add_table",
            "conditions": [{"condition": "neoforge:loot_table_id", "loot_table_id": f"minecraft:blocks/{grass}"},
                           {"condition": "minecraft:random_chance", "chance": 0.05}],
            "table": f"{MODID}:gameplay/manabloom_seeds"})
    write_json(DATA / "neoforge" / "loot_modifiers" / "global_loot_modifiers.json", {
        "replace": False, "entries": [f"{MODID}:seeds_from_{g}" for g in ("short_grass", "tall_grass", "fern")]})
    write_json(DATA / "minecraft" / "tags" / "block" / "mineable" / "axe.json",
               {"replace": False, "values": [f"{MODID}:war_standard", f"{MODID}:mess_hall"]})


# --------------------------------------------------------------------------- dwarf tunnels

RUNE_FAMILY = ["rune_stone", "rune_stone_stairs", "rune_stone_slab", "rune_stone_wall"]
TRAPS = ["spike_floor", "rune_mine", "flame_vent"]
TUNNEL_BLOCKS = RUNE_FAMILY + TRAPS


def _rot(model, x=0, y=0):
    v = {"model": model}
    if x:
        v["x"] = x
    if y:
        v["y"] = y
    if x or y:
        v["uvlock"] = True
    return v


def _stairs_states(name, tex):
    m = f"{MODID}:block/{name}"
    for suffix, parent in (("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")):
        write_json(ASSETS / "models" / "block" / f"{name}{suffix}.json", {
            "parent": f"minecraft:block/{parent}", "textures": {"bottom": tex, "top": tex, "side": tex}})
    base = {"east": 0, "south": 90, "west": 180, "north": 270}
    variants = {}
    for facing, y0 in base.items():
        for half in ("bottom", "top"):
            for shape in ("straight", "inner_left", "inner_right", "outer_left", "outer_right"):
                model = m + ("_inner" if shape.startswith("inner") else "_outer" if shape.startswith("outer") else "")
                y = y0
                if half == "bottom" and shape.endswith("left"):
                    y = y0 + 270
                if half == "top" and shape.endswith("right"):
                    y = y0 + 90
                variants[f"facing={facing},half={half},shape={shape}"] = _rot(model, 180 if half == "top" else 0, y % 360)
    write_json(ASSETS / "blockstates" / f"{name}.json", {"variants": variants})
    write_json(ASSETS / "models" / "item" / f"{name}.json", {"parent": m})


def _slab_states(name, full, tex):
    m = f"{MODID}:block/{name}"
    write_json(ASSETS / "models" / "block" / f"{name}.json", {
        "parent": "minecraft:block/slab", "textures": {"bottom": tex, "top": tex, "side": tex}})
    write_json(ASSETS / "models" / "block" / f"{name}_top.json", {
        "parent": "minecraft:block/slab_top", "textures": {"bottom": tex, "top": tex, "side": tex}})
    write_json(ASSETS / "blockstates" / f"{name}.json", {"variants": {
        "type=bottom": {"model": m}, "type=top": {"model": m + "_top"}, "type=double": {"model": f"{MODID}:block/{full}"}}})
    write_json(ASSETS / "models" / "item" / f"{name}.json", {"parent": m})


def _wall_states(name, tex):
    m = f"{MODID}:block/{name}"
    for suffix, parent in (("_post", "template_wall_post"), ("_side", "template_wall_side"),
                           ("_side_tall", "template_wall_side_tall")):
        write_json(ASSETS / "models" / "block" / f"{name}{suffix}.json", {
            "parent": f"minecraft:block/{parent}", "textures": {"wall": tex}})
    parts = [{"when": {"up": "true"}, "apply": {"model": m + "_post"}}]
    for side, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for height, suffix in (("low", "_side"), ("tall", "_side_tall")):
            apply = {"model": m + suffix, "uvlock": True}
            if y:
                apply["y"] = y
            parts.append({"when": {side: height}, "apply": apply})
    write_json(ASSETS / "blockstates" / f"{name}.json", {"multipart": parts})
    write_json(ASSETS / "models" / "item" / f"{name}.json", {
        "parent": "minecraft:block/wall_inventory", "textures": {"wall": tex}})


def _rune_marks(img, color, seed):
    """Thin glowing rune strokes cut into a face."""
    rnd = random.Random(seed)
    for _ in range(3):
        x, y = rnd.randint(2, 12), rnd.randint(2, 12)
        for i in range(rnd.randint(2, 4)):
            if 0 <= x < 16 and 0 <= y < 16:
                img.putpixel((x, y), color)
            if rnd.random() < 0.5:
                x += rnd.choice((-1, 1))
            else:
                y += rnd.choice((-1, 1))


def tunnel_assets():
    """Rune stone and its stairs/slab/wall, the three dwarf traps and the Rune Drill."""
    rune = hexc("5fd4e8")
    stone = brick_face(hexc("4a4f57"), hexc("2c3036"), 71)
    _rune_marks(stone, rune, 72)
    save(stone, "block/rune_stone.png")
    tex = f"{MODID}:block/rune_stone"
    write_json(ASSETS / "blockstates" / "rune_stone.json", {"variants": {"": {"model": f"{MODID}:block/rune_stone"}}})
    write_json(ASSETS / "models" / "block" / "rune_stone.json", {"parent": "minecraft:block/cube_all", "textures": {"all": tex}})
    write_json(ASSETS / "models" / "item" / "rune_stone.json", {"parent": f"{MODID}:block/rune_stone"})
    _stairs_states("rune_stone_stairs", tex)
    _slab_states("rune_stone_slab", "rune_stone", tex)
    _wall_states("rune_stone_wall", tex)

    # Spike floor: plain stone-brick floor with small holes; the sprung version has iron points in them.
    floor = brick_face(hexc("8a8a8a"), hexc("5e5e5e"), 81)
    holes = [(2, 2), (6, 2), (10, 2), (14, 2), (4, 6), (8, 6), (12, 6), (2, 10), (6, 10), (10, 10), (14, 10), (4, 14),
             (8, 14), (12, 14)]
    for x, y in holes:
        floor.putpixel((x, y), hexc("3a3a3a"))
    save(floor, "block/spike_floor_top.png")
    sprung = floor.copy()
    for x, y in holes:
        sprung.putpixel((x, y), hexc("d8dde0"))
        sprung.putpixel((x, y - 1 if y > 0 else y), hexc("a9b0b5"))
    save(sprung, "block/spike_floor_sprung.png")
    side = brick_face(hexc("8a8a8a"), hexc("5e5e5e"), 82)
    save(side, "block/spike_floor_side.png")
    for name, top in (("spike_floor", "spike_floor_top"), ("spike_floor_sprung", "spike_floor_sprung")):
        write_json(ASSETS / "models" / "block" / f"{name}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
            "top": f"{MODID}:block/{top}", "side": f"{MODID}:block/spike_floor_side", "bottom": f"{MODID}:block/spike_floor_side"}})
    write_json(ASSETS / "blockstates" / "spike_floor.json", {"variants": {
        "extended=false": {"model": f"{MODID}:block/spike_floor"}, "extended=true": {"model": f"{MODID}:block/spike_floor_sprung"}}})
    write_json(ASSETS / "models" / "item" / "spike_floor.json", {"parent": f"{MODID}:block/spike_floor"})

    # Rune mine: a cobbled deepslate floor tile with one faint rune.
    mine = Image.new("RGBA", (16, 16))
    noise_fill(mine, hexc("4d4d52"), 0.14, 91)
    for x, y in ((7, 6), (8, 6), (6, 7), (9, 7), (6, 8), (9, 8), (7, 9), (8, 9), (7, 7), (8, 8)):
        mine.putpixel((x, y), hexc("3e7f8c"))
    save(mine, "block/rune_mine.png")
    write_json(ASSETS / "models" / "block" / "rune_mine.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": f"{MODID}:block/rune_mine", "side": "minecraft:block/cobbled_deepslate", "bottom": "minecraft:block/cobbled_deepslate"}})
    write_json(ASSETS / "blockstates" / "rune_mine.json", {"variants": {"": {"model": f"{MODID}:block/rune_mine"}}})
    write_json(ASSETS / "models" / "item" / "rune_mine.json", {"parent": f"{MODID}:block/rune_mine"})

    # Flame vent: a blackened grate glowing orange, set in dark brick.
    vent = brick_face(hexc("3a2e2e"), hexc("1e1616"), 101)
    for x in range(3, 13):
        for y in range(3, 13):
            vent.putpixel((x, y), hexc("ff8a2a") if (x % 2 == 1 and 4 <= y <= 11) else hexc("1a1414"))
    save(vent, "block/flame_vent_front.png")
    save(brick_face(hexc("3a2e2e"), hexc("1e1616"), 102), "block/flame_vent_side.png")
    write_json(ASSETS / "models" / "block" / "flame_vent.json", {"parent": "minecraft:block/orientable", "textures": {
        "front": f"{MODID}:block/flame_vent_front", "side": f"{MODID}:block/flame_vent_side", "top": f"{MODID}:block/flame_vent_side"}})
    write_json(ASSETS / "blockstates" / "flame_vent.json", {"variants": {
        f"facing={f}": _rot(f"{MODID}:block/flame_vent", 0, y) for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    write_json(ASSETS / "models" / "item" / "flame_vent.json", {"parent": f"{MODID}:block/flame_vent"})

    drill = [
        "................",
        "...........CC...",
        "..........CcCC..",
        ".........IIIC...",
        "........IIIII...",
        ".......IIRIII...",
        "......IIIIIG....",
        ".....WIIIIG.....",
        "....WWIIIG......",
        "...WWW.GG.......",
        "..WWW...........",
        ".WWW............",
        ".WW.............",
        "................",
        "................",
        "................",
    ]
    save(sprite(drill, {"C": hexc("6ee8ff"), "c": hexc("d8fbff"), "I": hexc("8c949c"), "R": hexc("5fd4e8"),
                        "G": hexc("b08a3a"), "W": hexc("6a4424")}), "item/rune_drill.png")
    write_json(ASSETS / "models" / "item" / "rune_drill.json",
               {"parent": "minecraft:item/handheld", "textures": {"layer0": f"{MODID}:item/rune_drill"}})

    # Loot: each block drops itself (a double slab drops two).
    for b in TUNNEL_BLOCKS:
        entry = {"type": "minecraft:item", "name": f"{MODID}:{b}"}
        if b == "rune_stone_slab":
            entry["functions"] = [{"function": "minecraft:set_count", "count": 2, "add": False, "conditions": [
                {"condition": "minecraft:block_state_property", "block": f"{MODID}:{b}", "properties": {"type": "double"}}]},
                {"function": "minecraft:explosion_decay"}]
        write_json(DATA / MODID / "loot_table" / "blocks" / f"{b}.json", {"type": "minecraft:block", "pools": [
            {"rolls": 1, "bonus_rolls": 0, "entries": [entry], "conditions": [{"condition": "minecraft:survives_explosion"}]}],
            "random_sequence": f"{MODID}:blocks/{b}"})
    write_json(DATA / MODID / "tags" / "entity_type" / "smite_bonus.json", {"replace": False, "values": []})
    write_json(DATA / MODID / "tags" / "block" / "raider_unbreakable.json",
               {"replace": False, "values": [f"{MODID}:{b}" for b in RUNE_FAMILY]})
    for kind, b in (("stairs", "rune_stone_stairs"), ("slabs", "rune_stone_slab"), ("walls", "rune_stone_wall")):
        write_json(DATA / "minecraft" / "tags" / "block" / f"{kind}.json", {"replace": False, "values": [f"{MODID}:{b}"]})
        write_json(DATA / "minecraft" / "tags" / "item" / f"{kind}.json", {"replace": False, "values": [f"{MODID}:{b}"]})

    shaped("rune_stone", ["SSS", "SMS", "SSS"], {"S": "minecraft:stone", "M": "warfront:mana_shard"}, "warfront:rune_stone", 8)
    shaped("rune_stone_from_deepslate", ["SSS", "SMS", "SSS"], {"S": "minecraft:cobbled_deepslate", "M": "warfront:mana_shard"},
           "warfront:rune_stone", 8)
    shaped("rune_stone_stairs", ["S  ", "SS ", "SSS"], {"S": "warfront:rune_stone"}, "warfront:rune_stone_stairs", 4)
    shaped("rune_stone_slab", ["SSS"], {"S": "warfront:rune_stone"}, "warfront:rune_stone_slab", 6)
    shaped("rune_stone_wall", ["SSS", "SSS"], {"S": "warfront:rune_stone"}, "warfront:rune_stone_wall", 6)
    shaped("spike_floor", ["NNN", "SPS"], {"N": "minecraft:iron_nugget", "S": "minecraft:stone_bricks",
                                           "P": "minecraft:stone_pressure_plate"}, "warfront:spike_floor", 2)
    shaped("rune_mine", [" T ", "SCS"], {"T": "minecraft:tnt", "C": "warfront:mana_crystal", "S": "minecraft:cobbled_deepslate"},
           "warfront:rune_mine", 2)
    shaped("flame_vent", ["BBB", "BFB", "BMB"], {"B": "minecraft:deepslate_bricks", "F": "minecraft:fire_charge",
                                                 "M": "warfront:mana_shard"}, "warfront:flame_vent")
    shaped("village_charter", ["P", "W"], {"P": "minecraft:paper", "W": "minecraft:honeycomb"}, "warfront:village_charter")
    shaped("rune_drill", ["CDC", " I ", " I "], {"C": "warfront:mana_crystal", "D": "minecraft:diamond_pickaxe",
                                                 "I": "minecraft:iron_ingot"}, "warfront:rune_drill")


# --------------------------------------------------------------------------- race towers

# id: (race, name, plinth stone, mortar, signature ingredient)
RACE_TOWERS = {
    "ballista": ("human", "Ballista", "9a9a96", "6c6c68", "minecraft:crossbow"),
    "thornwood_sentinel": ("elf", "Thornwood Sentinel", "b8c4a0", "7f8c68", "minecraft:sweet_berries"),
    "rune_cannon": ("dwarf", "Rune Cannon", "5a5f66", "3a3e44", "minecraft:iron_block"),
    "war_drum_totem": ("orc", "War Drum Totem", "6b6a5a", "45443a", "minecraft:leather"),
    "soul_pyre": ("demon", "Soul Pyre", "3a2e2e", "1e1616", "minecraft:soul_soil"),
    "sun_lance": ("angel", "Sun Lance", "eeeae0", "c8c2b0", "minecraft:gold_block"),
    "lurker_pit": ("hive", "Lurker Pit", "1f3f44", "102528", "minecraft:sculk"),
    "watchtower_bell": ("human", "Watchtower Bell", "9a9a96", "6c6c68", "minecraft:bell"),
    "moonwell_grove": ("elf", "Moonwell Grove", "b8c4a0", "7f8c68", "minecraft:glow_berries"),
    "stone_warden": ("dwarf", "Stone Warden", "5a5f66", "3a3e44", "minecraft:shield"),
    "goblin_catapult": ("orc", "Goblin Bomb Catapult", "6b6a5a", "45443a", "minecraft:tnt"),
    "brimstone_chains": ("demon", "Brimstone Chains", "3a2e2e", "1e1616", "minecraft:chain"),
    "choir_bell": ("angel", "Choir Bell", "eeeae0", "c8c2b0", "minecraft:golden_apple"),
    "brood_nest": ("hive", "Brood Nest", "1f3f44", "102528", "minecraft:slime_ball"),
    "trebuchet": ("human", "Trebuchet", "9a9a96", "6c6c68", "minecraft:diamond_block"),
    "elder_treant_spire": ("elf", "Elder Treant Spire", "b8c4a0", "7f8c68", "minecraft:diamond_block"),
    "thunder_forge": ("dwarf", "Thunder Forge", "5a5f66", "3a3e44", "minecraft:diamond_block"),
    "waaagh_banner": ("orc", "Waaagh Banner", "6b6a5a", "45443a", "minecraft:diamond_block"),
    "hellgate": ("demon", "Hellgate", "3a2e2e", "1e1616", "minecraft:diamond_block"),
    "seraphic_obelisk": ("angel", "Seraphic Obelisk", "eeeae0", "c8c2b0", "minecraft:diamond_block"),
    "synapse_spire": ("hive", "Synapse Spire", "1f3f44", "102528", "minecraft:diamond_block"),
}


def race_tower_assets():
    """Plinth blocks for the race towers (the 3D tower is drawn on top by RaceTowerRenderer), names, loot, recipes."""
    for i, (tid, (race, name, stone, mortar, sig)) in enumerate(RACE_TOWERS.items()):
        if tid == "lurker_pit":
            # Hidden in the ground: it looks like the floor around it.
            img = Image.new("RGBA", (16, 16))
            noise_fill(img, hexc("2a2f2e"), 0.18, 300 + i)
            for x, y in ((3, 4), (11, 6), (6, 11), (12, 12)):
                img.putpixel((x, y), hexc("3fd8d0"))
        else:
            img = brick_face(hexc(stone), hexc(mortar), 300 + i)
        save(img, f"block/{tid}.png")
        write_json(ASSETS / "blockstates" / f"{tid}.json", {"variants": {"": {"model": f"{MODID}:block/{tid}"}}})
        write_json(ASSETS / "models" / "block" / f"{tid}.json", {"parent": "minecraft:block/cube_all",
                                                                 "textures": {"all": f"{MODID}:block/{tid}"}})
        write_json(ASSETS / "models" / "item" / f"{tid}.json", {"parent": f"{MODID}:block/{tid}"})
        write_json(DATA / MODID / "loot_table" / "blocks" / f"{tid}.json", {"type": "minecraft:block", "pools": [
            {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"{MODID}:{tid}"}],
             "conditions": [{"condition": "minecraft:survives_explosion"}]}], "random_sequence": f"{MODID}:blocks/{tid}"})
        shaped(tid, ["SCS", "SMS", "SSS"], {"S": "minecraft:stone_bricks", "C": "warfront:mana_crystal", "M": sig},
               f"warfront:{tid}")


# --------------------------------------------------------------------------- mana infrastructure

def _crystal_px(x, y, seed):
    """Facetted pale-blue crystal: bright core, darker facet edges."""
    rnd = random.Random(seed * 997 + x * 31 + y)
    c = hexc("7fd8ff") if (x + y) % 3 else hexc("bff0ff")
    if (x * 2 + y) % 5 == 0:
        c = hexc("3a8ad0")
    return shade(c, 1 + rnd.uniform(-0.08, 0.08))


def _cube(frm, to, tex, uv_side, uv_top):
    return {"from": frm, "to": to, "faces": {
        **{d: {"uv": uv_side, "texture": tex} for d in ("north", "south", "east", "west")},
        "up": {"uv": uv_top, "texture": tex}, "down": {"uv": uv_top, "texture": tex}}}


def mana_blocks():
    """Textures, models and blockstates for the Mana Well, Pylon, Brazier and Summoning Altar."""
    stone, mortar = hexc("5c5f6b"), hexc("383a44")

    # Well: dark stone brick with a glowing channel; the top shows the basin filling up.
    side = brick_face(stone, mortar, 41)
    for y in range(2, 15):
        side.putpixel((7, y), hexc("3a8ad0"))
        side.putpixel((8, y), hexc("7fd8ff") if y % 3 else hexc("bff0ff"))
    save(side, "block/mana_well_side.png")
    for fill in range(5):
        top = brick_face(stone, mortar, 42)
        for y in range(3, 13):
            for x in range(3, 13):
                if fill == 0:
                    c = shade(hexc("23252c"), 1 + ((x * 7 + y * 3) % 5) * 0.03)
                else:
                    c = mix(hexc("1c4a78"), hexc("8fe4ff"), min(1.0, fill / 4 * (0.65 + ((x + y * 2) % 4) * 0.1)))
                top.putpixel((x, y), c)
        save(top, f"block/mana_well_top_{fill}.png")
    write_json(ASSETS / "blockstates" / "mana_well.json", {"variants": {
        f"fill={f}": {"model": f"{MODID}:block/mana_well_{f}"} for f in range(5)}})
    for f in range(5):
        write_json(ASSETS / "models" / "block" / f"mana_well_{f}.json", {
            "parent": "minecraft:block/cube_bottom_top",
            "textures": {"top": f"{MODID}:block/mana_well_top_{f}", "bottom": "minecraft:block/stone_bricks",
                         "side": f"{MODID}:block/mana_well_side"}})
    write_json(ASSETS / "models" / "item" / "mana_well.json", {"parent": f"{MODID}:block/mana_well_2"})

    # Pylon and brazier share a layout: stone or iron on the left half of the texture, crystal on the right.
    for name, metal in (("mana_pylon", None), ("mana_brazier", hexc("4a4c54"))):
        img = Image.new("RGBA", (16, 16))
        if metal is None:
            img.paste(brick_face(stone, mortar, 43).crop((0, 0, 8, 16)), (0, 0))
        else:
            noise_fill(img, metal, 0.12, 44, (0, 0, 8, 16))
            for y in (0, 5, 10, 15):
                for x in range(8):
                    img.putpixel((x, y), shade(metal, 1.35))
        for y in range(16):
            for x in range(8, 16):
                img.putpixel((x, y), _crystal_px(x, y, 45))
        save(img, f"block/{name}.png")
    t = "#t"
    pylon = [_cube([4, 0, 4], [12, 3, 12], t, [0, 0, 8, 3], [0, 0, 8, 8]),
             _cube([6, 3, 6], [10, 11, 10], t, [2, 3, 6, 11], [2, 2, 6, 6]),
             _cube([5.5, 11, 5.5], [10.5, 16, 10.5], t, [9, 0, 14, 5], [9, 6, 14, 11])]
    brazier = [_cube([3, 0, 3], [13, 2, 13], t, [0, 0, 8, 2], [0, 0, 8, 8]),
               _cube([6, 2, 6], [10, 8, 10], t, [2, 2, 6, 8], [2, 2, 6, 6]),
               _cube([3, 8, 3], [13, 11, 13], t, [0, 8, 8, 11], [0, 4, 8, 12]),
               _cube([5, 11, 5], [11, 15, 11], t, [9, 0, 15, 4], [9, 6, 15, 12])]
    for name, elements in (("mana_pylon", pylon), ("mana_brazier", brazier)):
        write_json(ASSETS / "blockstates" / f"{name}.json", {"variants": {"": {"model": f"{MODID}:block/{name}"}}})
        write_json(ASSETS / "models" / "block" / f"{name}.json", {
            "parent": "minecraft:block/block", "render_type": "minecraft:cutout",
            "textures": {"t": f"{MODID}:block/{name}", "particle": f"{MODID}:block/{name}"}, "elements": elements})
        write_json(ASSETS / "models" / "item" / f"{name}.json", {"parent": f"{MODID}:block/{name}"})

    # Summoning altar: an obsidian-dark pedestal with a glowing summoning circle and a crystal at its heart.
    dark, seam = hexc("2a2333"), hexc("15111b")
    side = brick_face(dark, seam, 46)
    for x in range(1, 15):
        side.putpixel((x, 3), hexc("9a6ad8") if x % 2 else hexc("c9a0ff"))
    save(side, "block/summoning_altar_side.png")
    top = Image.new("RGBA", (16, 16))
    noise_fill(top, dark, 0.1, 47)
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if 5.2 < d < 6.4 or 2.6 < d < 3.4:
                top.putpixel((x, y), hexc("c9a0ff"))
    for x, y in ((7, 1), (8, 1), (14, 7), (14, 8), (7, 14), (8, 14), (1, 7), (1, 8)):
        top.putpixel((x, y), hexc("ffe28a"))
    save(top, "block/summoning_altar_top.png")
    write_json(ASSETS / "blockstates" / "summoning_altar.json",
               {"variants": {"": {"model": f"{MODID}:block/summoning_altar"}}})
    write_json(ASSETS / "models" / "block" / "summoning_altar.json", {
        "parent": "minecraft:block/block", "render_type": "minecraft:cutout",
        "textures": {"side": f"{MODID}:block/summoning_altar_side", "top": f"{MODID}:block/summoning_altar_top",
                     "crystal": f"{MODID}:block/mana_pylon", "particle": f"{MODID}:block/summoning_altar_side"},
        "elements": [
            {"from": [1, 0, 1], "to": [15, 10, 15], "faces": {
                **{d: {"uv": [1, 6, 15, 16], "texture": "#side"} for d in ("north", "south", "east", "west")},
                "up": {"uv": [1, 1, 15, 15], "texture": "#top"}, "down": {"uv": [1, 1, 15, 15], "texture": "#side"}}},
            _cube([6.5, 10, 6.5], [9.5, 14, 9.5], "#crystal", [10, 0, 13, 4], [10, 6, 13, 9])]})
    write_json(ASSETS / "models" / "item" / "summoning_altar.json", {"parent": f"{MODID}:block/summoning_altar"})


# --------------------------------------------------------------------------- game test structure

def raid_chests():
    """One raid chest per enemy faction, the loot at the heart of a siege outpost."""
    looks = {
        # faction: (body, band, accent, label)
        "marauders": ("7a5230", "3a2414", "d8cdb0", "war crate"),          # lashed planks, bone latch
        "black_legion": ("2a2a30", "8a8c92", "ece8dc", "strongbox"),       # black iron, steel bands, skull
        "burning_horde": ("d8cdb0", "5a1a12", "ff6a1a", "bone chest"),     # bone slats, ember lock
        "the_swarm": ("141d1a", "2e7a10", "8aff3a", "chitin pod"),         # black chitin, acid seams
        "silverwood_reavers": ("5a4630", "2e4a22", "dde6ee", "root-bound coffer"),
        "ironbeard_clan": ("5a5c62", "d4a017", "5ab0ff", "vault"),         # iron, gold bands, rune
        "fallen_host": ("e6dcb8", "6a5a8a", "b8a870", "reliquary"),        # tarnished gilt
    }
    variants = {}
    for i, (key, (body, band, accent, _)) in enumerate(looks.items()):
        side = Image.new("RGBA", (16, 16))
        noise_fill(side, hexc(body), 0.1, 70 + i)
        for x in range(16):
            for y in (2, 13):
                side.putpixel((x, y), hexc(band))
        for y in range(16):
            for x in (0, 15):
                side.putpixel((x, y), shade(hexc(band), 0.8))
        if key == "silverwood_reavers":
            for y in range(16):                                   # roots winding over the lid
                side.putpixel(((y * 3) % 16, y), hexc(band))
        if key == "the_swarm":
            for x in range(1, 15, 3):
                side.putpixel((x, 8), hexc(accent))
        for x in range(6, 10):                                    # lock plate
            for y in range(6, 10):
                side.putpixel((x, y), hexc(accent) if (x, y) not in ((6, 6), (9, 6), (6, 9), (9, 9)) else hexc(band))
        if key == "black_legion":
            for x, y in ((7, 7), (8, 7)):
                side.putpixel((x, y), hexc("141416"))
        save(side, f"block/raid_chest_{key}_side.png")
        top = Image.new("RGBA", (16, 16))
        noise_fill(top, shade(hexc(body), 1.1), 0.08, 90 + i)
        for x in range(16):
            for y in (0, 15, 7, 8):
                top.putpixel((x, y), hexc(band))
        save(top, f"block/raid_chest_{key}_top.png")
        write_json(ASSETS / "models" / "block" / f"raid_chest_{key}.json", {
            "parent": "minecraft:block/cube_bottom_top",
            "textures": {"side": f"{MODID}:block/raid_chest_{key}_side", "top": f"{MODID}:block/raid_chest_{key}_top",
                         "bottom": f"{MODID}:block/raid_chest_{key}_top"}})
        variants[f"faction={i}"] = {"model": f"{MODID}:block/raid_chest_{key}"}
    write_json(ASSETS / "blockstates" / "raid_chest.json", {"variants": variants})
    write_json(ASSETS / "models" / "item" / "raid_chest.json", {"parent": f"{MODID}:block/raid_chest_marauders"})


def campaign_assets():
    """The Warlord's Seat, the seven trophy banners, the warlords' gear and the Seal of the Seven."""
    keys = ["marauders", "black_legion", "burning_horde", "the_swarm", "silverwood_reavers", "ironbeard_clan", "fallen_host"]
    colors = {"marauders": ("8a2a1a", "d8cdb0"), "black_legion": ("1a1a1e", "ece8dc"), "burning_horde": ("5a1a12", "ff6a1a"),
              "the_swarm": ("141d1a", "8aff3a"), "silverwood_reavers": ("2e4a22", "dde6ee"),
              "ironbeard_clan": ("1a2a4a", "d4a017"), "fallen_host": ("4a4458", "b8a870")}
    # Warlord's Seat: black stone with an ember crown.
    seat = Image.new("RGBA", (16, 16))
    noise_fill(seat, hexc("1a1418"), 0.12, 501)
    for x in range(16):
        seat.putpixel((x, 0), hexc("ff7a1a"))
        seat.putpixel((x, 15), hexc("3a0a04"))
    for x in (2, 5, 8, 11, 14):
        for y in range(1, 4):
            seat.putpixel((x, y), hexc("ff9a3a"))
    save(seat, "block/fortress_core.png")
    write_json(ASSETS / "models" / "block" / "fortress_core.json",
               {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MODID}:block/fortress_core"}})
    write_json(ASSETS / "blockstates" / "fortress_core.json",
               {"variants": {f"faction={i}": {"model": f"{MODID}:block/fortress_core"} for i in range(7)}})
    write_json(ASSETS / "models" / "item" / "fortress_core.json", {"parent": f"{MODID}:block/fortress_core"})
    # Trophy banners: the War Standard shape, a dark pole and the beaten warlord's colors.
    pole = Image.new("RGBA", (16, 16))
    noise_fill(pole, hexc("2a2024"), 0.1, 502)
    for x in range(16):
        pole.putpixel((x, 0), hexc("d4a017"))
    save(pole, "block/trophy_banner_pole.png")
    variants = {}
    for i, k in enumerate(keys):
        base, mark = colors[k]
        flag = Image.new("RGBA", (16, 16))
        noise_fill(flag, hexc(base), 0.08, 510 + i)
        for y in range(1, 13):
            flag.putpixel((0, y), hexc("d4a017"))
            flag.putpixel((6, y), hexc("d4a017"))
        for x, y in ((3, 4), (2, 5), (3, 5), (4, 5), (3, 6), (2, 7), (4, 7), (3, 8), (3, 9)):   # a trophy emblem
            flag.putpixel((x, y), hexc(mark))
        for x in range(7):
            flag.putpixel((x, 12), hexc("d4a017") if x % 2 == 0 else hexc(base))
        save(flag, f"block/trophy_banner_{k}.png")
        write_json(ASSETS / "models" / "block" / f"trophy_banner_{k}.json", {
            "parent": f"{MODID}:block/war_standard",
            "textures": {"pole": f"{MODID}:block/trophy_banner_pole", "flag": f"{MODID}:block/trophy_banner_{k}",
                         "particle": f"{MODID}:block/trophy_banner_{k}"}})
        variants[f"faction={i}"] = {"model": f"{MODID}:block/trophy_banner_{k}"}
    write_json(ASSETS / "blockstates" / "trophy_banner.json", {"variants": variants})
    write_json(ASSETS / "models" / "item" / "trophy_banner.json", {"parent": f"{MODID}:block/trophy_banner_marauders"})

    # The warlords' gear.
    def axe(head, haft, glow):
        return sprite([
            "................", "........HHH.....", ".......HHHHH....", "......HHHgHHH...", ".......HHHHHH...",
            "........HHsHH...", ".........sHH....", "........s.......", ".......s........", "......s.........",
            ".....s..........", "....s...........", "...s............", "..s.............", "................",
            "................"], {"H": hexc(head), "g": hexc(glow), "s": hexc(haft)})

    def sword(blade, hilt, glow):
        return sprite([
            "................", "..............B.", ".............BB.", "............BgB.", "...........BgB..",
            "..........BgB...", ".........BgB....", "........BgB.....", ".......BgB......", "..h...BgB.......",
            "...h.BBB........", "....hh..........", "....sh..........", "...s..h.........", "..s.............",
            "................"], {"B": hexc(blade), "g": hexc(glow), "h": hexc(hilt), "s": hexc(hilt)})
    save(axe("8a8c92", "5a3a22", "c8281e"), "item/skullsplitter.png")
    save(sword("3a2a24", "1a1210", "ff6a1a"), "item/emberbrand.png")
    save(sword("1c2622", "0b1210", "8aff3a"), "item/broodfang.png")
    save(axe("5a5c62", "3a2414", "5ab0ff"), "item/runehammer.png")
    save(sprite([
        "................", "...WWW..........", "..W..WW.........", ".W.....W........", ".W......W.......",
        "W........W......", "W.........W.....", "W.........sW....", "W.........sW....", "W........W......",
        ".W......W.......", ".W.....W........", "..W..WW.........", "...WWW..........", "................",
        "................"], {"W": hexc("e6eef4"), "s": hexc("2e4a22")}), "item/thornbow.png")
    save(sprite([
        "................", "...PP......PP...", "..PPPP....PPPP..", "..PPPPPPPPPPPP..", "...PPPPPPPPPP...",
        "...PPPPggPPPP...", "...PPPPPPPPPP...", "...PPPPPPPPPP...", "...PPPPggPPPP...", "...PPPPPPPPPP...",
        "...PPPPPPPPPP...", "....PPPPPPPP....", "................", "................", "................",
        "................"], {"P": hexc("1e1e22"), "g": hexc("ece8dc")}), "item/legion_warplate.png")
    save(sprite([
        "................", "................", "................", "....HHH.HHH.....", "...H.........H..",
        "..H...........H.", "..H...........H.", "...H.........H..", "....HHH..HHHH...", "................",
        "................", "................", "................", "................", "................",
        "................"], {"H": hexc("b8a870")}), "item/fallen_halo.png")
    save(sprite([
        "................", "......WWWW......", "....WWrrrrWW....", "...WrrRRRRrrW...", "..WrRRWWWWRRrW..",
        "..WrRWRRRRWRrW..", ".WrRWRRggRRWRrW.", ".WrRWRggggRWRrW.", ".WrRWRRggRRWRrW.", "..WrRWRRRRWRrW..",
        "..WrRRWWWWRRrW..", "...WrrRRRRrrW...", "....WWrrrrWW....", "......WWWW......", "................",
        "................"], {"W": hexc("7a5a2a"), "r": hexc("e8d8a8"), "R": hexc("8b1a1a"), "g": hexc("d4a017")}),
        "item/seal_of_seven.png")
    for item, parent in [("skullsplitter", "handheld"), ("emberbrand", "handheld"), ("broodfang", "handheld"),
                         ("runehammer", "handheld"), ("legion_warplate", "generated"), ("fallen_halo", "generated"),
                         ("seal_of_seven", "generated")]:
        write_json(ASSETS / "models" / "item" / f"{item}.json",
                   {"parent": f"minecraft:item/{parent}", "textures": {"layer0": f"{MODID}:item/{item}"}})
    # The Thornbow draws like a vanilla bow.
    write_json(ASSETS / "models" / "item" / "thornbow.json", {
        "parent": "minecraft:item/bow", "textures": {"layer0": f"{MODID}:item/thornbow"},
        "overrides": [
            {"predicate": {"pulling": 1}, "model": "minecraft:item/bow_pulling_0"},
            {"predicate": {"pulling": 1, "pull": 0.65}, "model": "minecraft:item/bow_pulling_1"},
            {"predicate": {"pulling": 1, "pull": 0.9}, "model": "minecraft:item/bow_pulling_2"}]})


def mess_hall():
    """The Mess Hall: a plank-and-barrel larder with a laid table on top."""
    plank, dark = hexc("9c6b3c"), hexc("6b4423")
    side = Image.new("RGBA", (16, 16))
    noise_fill(side, plank, 0.08, 61)
    for y in (0, 5, 10, 15):
        for x in range(16):
            side.putpixel((x, y), dark)
    for x, y in ((3, 7), (4, 7), (5, 7), (10, 7), (11, 7), (12, 7), (3, 8), (12, 8)):   # hanging sausages and a loaf
        side.putpixel((x, y), hexc("a83a2a") if x < 8 else hexc("d9a441"))
    save(side, "block/mess_hall_side.png")
    top = Image.new("RGBA", (16, 16))
    noise_fill(top, hexc("b07a45"), 0.06, 62)
    for x in range(4, 12):
        for y in range(5, 11):
            top.putpixel((x, y), hexc("e8e2d0") if (x + y) % 2 else hexc("d8d0bc"))   # tablecloth
    for x, y in ((6, 7), (7, 7), (8, 8), (9, 8)):
        top.putpixel((x, y), hexc("d9a441"))
    save(top, "block/mess_hall_top.png")
    write_json(ASSETS / "blockstates" / "mess_hall.json", {"variants": {"": {"model": f"{MODID}:block/mess_hall"}}})
    write_json(ASSETS / "models" / "block" / "mess_hall.json", {
        "parent": "minecraft:block/cube_bottom_top",
        "textures": {"side": f"{MODID}:block/mess_hall_side", "top": f"{MODID}:block/mess_hall_top",
                     "bottom": "minecraft:block/oak_planks"}})
    write_json(ASSETS / "models" / "item" / "mess_hall.json", {"parent": f"{MODID}:block/mess_hall"})


def _nbt_payload(tag_type, value):
    import struct
    if tag_type == 3:
        return struct.pack(">i", value)
    if tag_type == 8:
        b = value.encode("utf-8")
        return struct.pack(">H", len(b)) + b
    if tag_type == 9:
        elem_type, items = value
        out = struct.pack(">bi", elem_type if items else 0, len(items))
        for it in items:
            out += _nbt_payload(elem_type, it)
        return out
    if tag_type == 10:
        out = b""
        for name, (t, v) in value.items():
            nb = name.encode("utf-8")
            out += struct.pack(">bH", t, len(nb)) + nb + _nbt_payload(t, v)
        return out + b"\x00"
    raise ValueError(tag_type)


def platform_structure():
    """A 9x4x9 test arena: stone floor, open air above. Used by the mod's game tests."""
    import gzip
    import struct
    size = (9, 4, 9)
    blocks = []
    for x in range(size[0]):
        for z in range(size[2]):
            blocks.append({"pos": (9, (3, [x, 0, z])), "state": (3, 0)})
            for y in range(1, size[1]):
                blocks.append({"pos": (9, (3, [x, y, z])), "state": (3, 1)})
    root = {
        "DataVersion": (3, 3955),
        "size": (9, (3, list(size))),
        "palette": (9, (10, [{"Name": (8, "minecraft:stone")}, {"Name": (8, "minecraft:air")}])),
        "blocks": (9, (10, blocks)),
        "entities": (9, (10, [])),
    }
    data = struct.pack(">bH", 10, 0) + _nbt_payload(10, root)
    p = DATA / MODID / "structure" / "platform.nbt"
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_bytes(gzip.compress(data))


if __name__ == "__main__":
    platform_structure()
    import fortresses
    fortresses.generate(DATA)
    mana_textures()
    item_textures()
    hammer_texture()
    glider_texture()
    flight_textures()
    charter_texture()
    block_textures()
    soldier_skins()
    models_and_states()
    mana_blocks()
    mess_hall()
    raid_chests()
    campaign_assets()
    effect_icons()
    lang()
    recipes()
    loot_and_tags()
    tunnel_assets()
    race_tower_assets()
    print("Generated assets in", ROOT)
