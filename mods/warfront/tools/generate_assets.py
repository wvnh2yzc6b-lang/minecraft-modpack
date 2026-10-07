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
                         ("war_mark", "generated"), ("war_horn", "generated"), ("mana_glider", "generated")]:
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
        "block.warfront.mess_hall": "Mess Hall",
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
        "item.warfront.mana_glider": "Mana Glider",
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
    for b in blocks:
        write_json(DATA / MODID / "loot_table" / "blocks" / f"{b}.json", {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "bonus_rolls": 0,
                       "entries": [{"type": "minecraft:item", "name": f"{MODID}:{b}"}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}],
            "random_sequence": f"{MODID}:blocks/{b}"})
    ores = ["mana_ore", "deepslate_mana_ore"]
    write_json(DATA / "minecraft" / "tags" / "block" / "mineable" / "pickaxe.json",
               {"replace": False, "values": [f"{MODID}:{b}" for b in blocks[:3] + blocks[4:8] + ores]})
    # Floors a summoning altar can stand on: any brick or stone-brick block, so every race can build in its style.
    write_json(DATA / MODID / "tags" / "block" / "altar_base.json", {"replace": False, "values": [
        "#minecraft:stone_bricks", "minecraft:polished_blackstone_bricks", "minecraft:cracked_polished_blackstone_bricks",
        "minecraft:deepslate_bricks", "minecraft:cracked_deepslate_bricks", "minecraft:deepslate_tiles",
        "minecraft:cracked_deepslate_tiles", "minecraft:nether_bricks", "minecraft:cracked_nether_bricks",
        "minecraft:red_nether_bricks", "minecraft:bricks", "minecraft:mud_bricks", "minecraft:quartz_bricks",
        "minecraft:end_stone_bricks", "minecraft:prismarine_bricks", "minecraft:tuff_bricks"]})
    write_json(DATA / "minecraft" / "tags" / "block" / "needs_iron_tool.json",
               {"replace": False, "values": [f"{MODID}:{o}" for o in ores]})
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
    mana_textures()
    item_textures()
    hammer_texture()
    glider_texture()
    block_textures()
    soldier_skins()
    models_and_states()
    mana_blocks()
    mess_hall()
    effect_icons()
    lang()
    recipes()
    loot_and_tags()
    print("Generated assets in", ROOT)
