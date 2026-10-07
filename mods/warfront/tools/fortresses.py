"""Warlord fortresses: seven structure templates (one per enemy faction) and the world-gen files that place them.
Called by generate_assets.py. Placeholder looks from each faction's palette until the owner sends references.

Each fortress is 33 x 16 x 33: a walled courtyard with four corner towers and a gate facing south, an inner keep, and
the fortress core (the warlord's seat) in the middle of the keep. The core block calls the defenders and the warlord
when a player comes near.
"""
import gzip
import json
import struct

SIZE = (33, 16, 33)
FACTIONS = ["marauders", "black_legion", "burning_horde", "the_swarm", "silverwood_reavers", "ironbeard_clan", "fallen_host"]

# faction: (floor, wall, tower, accent, roof)
PALETTES = {
    "marauders": ("minecraft:coarse_dirt", "minecraft:spruce_log", "minecraft:stripped_spruce_log", "minecraft:red_wool",
                  "minecraft:spruce_planks"),
    "black_legion": ("minecraft:cobbled_deepslate", "minecraft:deepslate_bricks", "minecraft:polished_deepslate",
                     "minecraft:black_wool", "minecraft:deepslate_tiles"),
    "burning_horde": ("minecraft:blackstone", "minecraft:nether_bricks", "minecraft:red_nether_bricks", "minecraft:magma_block",
                      "minecraft:polished_blackstone_bricks"),
    "the_swarm": ("minecraft:sculk", "minecraft:deepslate_tiles", "minecraft:sculk", "minecraft:sculk_catalyst",
                  "minecraft:cobbled_deepslate"),
    "silverwood_reavers": ("minecraft:moss_block", "minecraft:birch_log", "minecraft:stripped_birch_log", "minecraft:green_wool",
                           "minecraft:birch_planks"),
    "ironbeard_clan": ("minecraft:polished_andesite", "minecraft:stone_bricks", "minecraft:chiseled_stone_bricks",
                       "minecraft:blue_wool", "minecraft:stone_brick_slab"),
    "fallen_host": ("minecraft:smooth_quartz", "minecraft:quartz_bricks", "minecraft:quartz_pillar", "minecraft:gray_wool",
                    "minecraft:smooth_quartz"),
}

BIOMES = {
    "marauders": ["#minecraft:is_badlands", "#minecraft:is_savanna"],
    "black_legion": ["minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow"],
    "burning_horde": ["#minecraft:is_nether"],
    "the_swarm": ["minecraft:deep_dark", {"id": "deeperdarker:echoing_forest", "required": False},
                  {"id": "deeperdarker:overcast_columns", "required": False},
                  {"id": "deeperdarker:otherside_highlands", "required": False},
                  {"id": "deeperdarker:blooming_caverns", "required": False}],
    "silverwood_reavers": ["#minecraft:is_forest"],
    "ironbeard_clan": ["#minecraft:is_mountain"],
    "fallen_host": ["minecraft:jagged_peaks", "minecraft:frozen_peaks", "minecraft:stony_peaks",
                    {"id": "aether:skyroot_meadow", "required": False}, {"id": "aether:skyroot_grove", "required": False},
                    {"id": "aether:skyroot_woodland", "required": False}, {"id": "aether:skyroot_forest", "required": False}],
}

# Where a fortress sits: on the surface, or set at a fixed height (the Nether's roof and the deep dark).
HEIGHT = {"burning_horde": 40, "the_swarm": -40}


def _nbt(tag_type, value):
    if tag_type == 3:
        return struct.pack(">i", value)
    if tag_type == 8:
        b = value.encode("utf-8")
        return struct.pack(">H", len(b)) + b
    if tag_type == 9:
        elem_type, items = value
        out = struct.pack(">bi", elem_type if items else 0, len(items))
        for it in items:
            out += _nbt(elem_type, it)
        return out
    if tag_type == 10:
        out = b""
        for name, (t, v) in value.items():
            nb = name.encode("utf-8")
            out += struct.pack(">bH", t, len(nb)) + nb + _nbt(t, v)
        return out + b"\x00"
    raise ValueError(tag_type)


def layout(faction):
    """The fortress as {(x, y, z): (block, properties)}; every other spot inside is air (so it carves terrain)."""
    floor, wall, tower, accent, roof = PALETTES[faction]
    W, H, D = SIZE
    blocks = {}

    def put(x, y, z, b, props=None):
        if 0 <= x < W and 0 <= y < H and 0 <= z < D:
            blocks[(x, y, z)] = (b, props or {})

    for x in range(W):
        for z in range(D):
            put(x, 0, z, floor)
    # Outer wall with crenellations and a south gate.
    for i in range(W):
        for (x, z) in ((i, 0), (i, D - 1), (0, i), (W - 1, i)):
            for y in range(1, 7):
                put(x, y, z, wall)
            if i % 2 == 0:
                put(x, 7, z, wall)
    for x in range(14, 19):
        for y in range(1, 5):
            blocks.pop((x, y, D - 1), None)
    # Banners of the faction color over the gate.
    for x in (13, 19):
        for y in range(3, 7):
            put(x, y, D - 1, accent)
    # Corner towers.
    for cx, cz in ((0, 0), (W - 5, 0), (0, D - 5), (W - 5, D - 5)):
        for dx in range(5):
            for dz in range(5):
                edge = dx in (0, 4) or dz in (0, 4)
                for y in range(1, 11):
                    if edge:
                        put(cx + dx, y, cz + dz, tower)
                put(cx + dx, 10, cz + dz, roof)
                if edge and (dx + dz) % 2 == 0:
                    put(cx + dx, 11, cz + dz, tower)
    # The inner keep: an 11 x 11 hall with a door to the south.
    k0, k1 = 11, 21
    for x in range(k0, k1 + 1):
        for z in range(k0, k1 + 1):
            edge = x in (k0, k1) or z in (k0, k1)
            for y in range(1, 9):
                if edge:
                    put(x, y, z, wall if (x + y + z) % 5 else tower)
            put(x, 9, z, roof)
    for x in range(15, 18):
        for y in range(1, 4):
            blocks.pop((x, y, k1), None)
    for x, z in ((k0 + 2, k0 + 2), (k1 - 2, k0 + 2), (k0 + 2, k1 - 2), (k1 - 2, k1 - 2)):
        for y in range(1, 9):
            put(x, y, z, tower)                                  # pillars in the hall
    # The warlord's seat: the fortress core.
    put(16, 1, 16, "warfront:fortress_core", {"faction": str(FACTIONS.index(faction))})
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            if dx or dz:
                put(16 + dx, 0, 16 + dz, accent)
    return blocks


def write_template(path, faction):
    W, H, D = SIZE
    placed = layout(faction)
    palette, index = [], {}
    def pal(b, props):
        key = (b, tuple(sorted(props.items())))
        if key not in index:
            entry = {"Name": (8, b)}
            if props:
                entry["Properties"] = (10, {k: (8, v) for k, v in props.items()})
            index[key] = len(palette)
            palette.append(entry)
        return index[key]
    air = pal("minecraft:air", {})
    out = []
    for x in range(W):
        for y in range(H):
            for z in range(D):
                b, props = placed.get((x, y, z), ("minecraft:air", {}))
                out.append({"pos": (9, (3, [x, y, z])), "state": (3, air if b == "minecraft:air" else pal(b, props))})
    root = {
        "DataVersion": (3, 3955),
        "size": (9, (3, [W, H, D])),
        "palette": (9, (10, palette)),
        "blocks": (9, (10, out)),
        "entities": (9, (10, [])),
    }
    data = struct.pack(">bH", 10, 0) + _nbt(10, root)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(gzip.compress(data, mtime=0))


def _json(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")


def generate(data_root, modid="warfront"):
    for i, f in enumerate(FACTIONS):
        write_template(data_root / modid / "structure" / f"fortress_{f}.nbt", f)
        height = HEIGHT.get(f)
        structure = {
            "type": "minecraft:jigsaw",
            "biomes": f"#{modid}:has_structure/fortress_{f}",
            "step": "surface_structures" if height is None else "underground_structures",
            "spawn_overrides": {},
            "terrain_adaptation": "beard_thin" if height is None else "none",
            "start_pool": f"{modid}:fortress/{f}",
            "size": 1,
            "start_height": {"absolute": 0 if height is None else height},
            "max_distance_from_center": 80,
            "use_expansion_hack": False,
        }
        if height is None:
            structure["project_start_to_heightmap"] = "WORLD_SURFACE_WG"
        _json(data_root / modid / "worldgen" / "structure" / f"fortress_{f}.json", structure)
        _json(data_root / modid / "worldgen" / "template_pool" / "fortress" / f"{f}.json", {
            "fallback": "minecraft:empty",
            "elements": [{"weight": 1, "element": {"element_type": "minecraft:single_pool_element",
                                                   "location": f"{modid}:fortress_{f}", "projection": "rigid",
                                                   "processors": "minecraft:empty"}}]})
        # Rare and far apart: roughly one per 1500 blocks.
        _json(data_root / modid / "worldgen" / "structure_set" / f"fortress_{f}.json", {
            "structures": [{"structure": f"{modid}:fortress_{f}", "weight": 1}],
            "placement": {"type": "minecraft:random_spread", "spacing": 96, "separation": 48, "salt": 470233 + i * 7919}})
        _json(data_root / modid / "tags" / "worldgen" / "biome" / "has_structure" / f"fortress_{f}.json",
              {"replace": False, "values": BIOMES[f]})
        _json(data_root / modid / "tags" / "worldgen" / "structure" / "fortress" / f"{f}.json",
              {"replace": False, "values": [f"{modid}:fortress_{f}"]})
