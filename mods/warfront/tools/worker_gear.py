"""Gear for the posted roles (farmer, builder, guard): one model per role and humanoid race, worn
over the soldier's body, plus the imp versions for demons. Imported by units.py.

Humanoid gear models use the vanilla player rig (head, body, arms, legs) so the game can copy the
soldier's pose straight onto them. Each race gets its own headgear and details; NPC factions reuse
their race's geometry with their own colors.
"""
# part, box, overlay, attach and the pixel helpers are injected by units.py (see bind()).

# Factions borrow their race's geometry.
RACE_OF = {
    "human": "human", "elf": "elf", "dwarf": "dwarf", "orc": "orc", "angel": "angel", "hive": "hive",
    "marauders": "orc", "black_legion": "human", "silverwood_reavers": "elf", "ironbeard_clan": "dwarf",
    "fallen_host": "angel", "the_swarm": "hive",
}
HUMANOID_RACES = ["human", "elf", "dwarf", "orc", "angel", "hive"]
ROLES = ["farmer", "builder", "guard"]


def _base(lead):
    """Common palette keys so every material has what it needs."""
    p = dict(leather="7a5230", leather_dark="4a3018", iron="9aa0a8", iron_dark="565c64", rust="8a4a22",
             cloth="7a6a54", cloth_dark="3a3028", bone="d8cdb0", bone_dark="8c7c5c", accent="2f5fa8",
             accent_dark="1c3a6a", trim="c9a44a", straw="d8b45a", straw_dark="9a7a32", wood="9a6a3a",
             wood_dark="5a3c20", apron="b8a888", leaf="5a8a3a", leaf_dark="2e5020", linen="e8e0cc",
             chitin="5a3a6a", chitin_dark="2a1a34", glow_c="ffb040")
    p.update(lead)
    return p


GEAR_PALETTES = {
    "human": _base({}),
    "elf": _base(dict(leather="5f6a3a", leather_dark="333d1c", iron="c8d2d6", iron_dark="7a8a90", cloth="4f7a4a",
                      cloth_dark="263d24", accent="3f8f3f", accent_dark="1f4f22", trim="d8d0a0", wood="b08c58",
                      wood_dark="6a5030", apron="9ab070", glow_c="c8ff9a")),
    "dwarf": _base(dict(leather="6a4226", leather_dark="3a2414", iron="7c7f84", iron_dark="45474c", cloth="8a3a2a",
                        cloth_dark="4a1a12", accent="a83a2a", accent_dark="5a1a12", trim="d4a017", apron="8a6a4a",
                        glow_c="ffd060")),
    "orc": _base(dict(leather="4a3a22", leather_dark="241a0e", iron="6a6460", iron_dark="363230", rust="9a4a1a",
                      cloth="6a5a3a", cloth_dark="2e2618", accent="8a2a1a", accent_dark="4a120a", trim="d8cdb0",
                      apron="6a5a40", glow_c="ff8a3a")),
    "angel": _base(dict(leather="d8c8a8", leather_dark="9a8a6a", iron="e6dcb8", iron_dark="a89a6a", rust="c8a44a",
                        cloth="f0ece0", cloth_dark="b8b0a0", accent="d4a017", accent_dark="8a6a10", trim="fff0a8",
                        apron="e8e0cc", glow_c="fff4c0")),
    "hive": _base(dict(leather="4a2a5a", leather_dark="24142e", iron="7a4a8a", iron_dark="3a2244", rust="b048d0",
                       cloth="3a2a4a", cloth_dark="1a1224", accent="b048d0", accent_dark="5a206a", trim="e070ff",
                       apron="5a3a6a", glow_c="e070ff")),
    "marauders": _base(dict(leather="5a3a1e", leather_dark="2a1a0c", iron="6a6460", iron_dark="363230", rust="9a4a1a",
                            cloth="7a4a2a", cloth_dark="3a2212", accent="9a5a1a", accent_dark="4a2a0a", trim="d8cdb0",
                            apron="7a5a3a", glow_c="ff8a3a")),
    "black_legion": _base(dict(leather="2e2a2a", leather_dark="141212", iron="4a4c54", iron_dark="1e2026", cloth="2a2a30",
                               cloth_dark="121216", accent="1a1a1e", accent_dark="0a0a0c", trim="a83232", apron="3a3a40",
                               glow_c="ff4a3a")),
    "silverwood_reavers": _base(dict(leather="3a4a5a", leather_dark="1a2430", iron="b8c4d0", iron_dark="6a7a8a",
                                     cloth="3a4a5a", cloth_dark="1a222c", accent="8a9aa8", accent_dark="4a5a6a",
                                     trim="dde6ee", apron="5a6a7a", glow_c="b8e0ff")),
    "ironbeard_clan": _base(dict(leather="5a3a22", leather_dark="2e1c10", iron="9aa0a8", iron_dark="565c64",
                                 cloth="3a5a8a", cloth_dark="1a2a4a", accent="3a5a8a", accent_dark="1a2a4a",
                                 trim="d4a017", apron="6a5a4a", glow_c="ffd060")),
    "fallen_host": _base(dict(leather="3a3448", leather_dark="1a1624", iron="4a4458", iron_dark="221e2c", rust="6a5a8a",
                              cloth="2a2632", cloth_dark="121018", accent="4a3a6a", accent_dark="221a34", trim="9a8ac8",
                              apron="3a3448", glow_c="b0a0ff")),
    "the_swarm": _base(dict(leather="3a4a1a", leather_dark="1a240a", iron="4a5a2a", iron_dark="223010", rust="6aa020",
                            cloth="2a3418", cloth_dark="12180a", accent="6aa020", accent_dark="2e4a0e", trim="b8ff4a",
                            chitin="3a4a1a", chitin_dark="1a240a", apron="3a4a24", glow_c="b8ff4a")),
}

# Extra keys the imp palettes need for worker gear.
IMP_WORKER_KEYS = {
    "demon": dict(accent="7a1a14", accent_dark="3e0a08", trim="c9772a", straw="b8944a", straw_dark="7a5a2a",
                  wood="7a5030", wood_dark="4a2c18", apron="6a4a3a", leaf="5a6a2a", leaf_dark="2e3a14", linen="c8b8a0",
                  chitin="3a2020", chitin_dark="1a0c0c", glow_c="ff9a2e"),
    "burning_horde": dict(accent="3a0a08", accent_dark="1a0504", trim="ff6a1a", straw="8a6a3a", straw_dark="4a3418",
                          wood="4a3020", wood_dark="24160c", apron="3a2a20", leaf="4a4a1a", leaf_dark="24240a",
                          linen="8a7a68", chitin="2a1010", chitin_dark="140606", glow_c="ff6a1a"),
}


# ----------------------------------------------------------------------------- shared pieces

def rig(model_id):
    """Empty vanilla player rig to hang gear on; pivots match PlayerModel."""
    return {"id": model_id, "tex": [64, 64], "parts": [
        part("head"), part("hat"), part("body"), part("right_arm", pivot=(-5, 2, 0)),
        part("left_arm", pivot=(5, 2, 0)), part("right_leg", pivot=(-1.9, 12, 0)), part("left_leg", pivot=(1.9, 12, 0)),
    ]}


def both_arms(m, make):
    attach(m, "right_arm", *make("r", -1))
    attach(m, "left_arm", *make("l", 1))


def both_legs(m, make):
    attach(m, "right_leg", *make("r", -1))
    attach(m, "left_leg", *make("l", 1))


def boots(m, mat="leather"):
    both_legs(m, lambda s, sx: [overlay(f"boot_{s}", (-2, 9, -2), (4, 3, 4), mat, 0.3)])


# ----------------------------------------------------------------------------- farmer

def farmer_hat(race):
    if race == "human":
        return [part("hat_brim", pivot=(0, -6.3, 0), rot=(0.06, 0, 0), boxes=[box((-6.5, 0, -6.5), (13, 1, 13), "straw")]),
                overlay("hat_crown", (-4, -10, -4), (8, 4, 8), "straw", 0.3),
                overlay("hat_band", (-4, -7.4, -4), (8, 1, 8), "accent_cloth", 0.5)]
    if race == "dwarf":
        return [overlay("cap_knit", (-4, -9, -4), (8, 3, 8), "wool", 0.4),
                overlay("cap_fold", (-4, -6.6, -4), (8, 1.5, 8), "wool", 0.65),
                part("cap_pompom", pivot=(0, -9.6, 1.5), boxes=[box((-1, -2, -1), (2, 2, 2), "wool")])]
    if race == "elf":
        return [overlay("hood", (-4, -8, -4), (8, 6, 8), "leafweave", 0.5),
                part("hood_leaf_r", pivot=(-2, -7.5, 4), rot=(0.9, -0.3, 0.2), boxes=[box((-1.5, 0, 0), (3, 5, 0), "leaf")]),
                part("hood_leaf_l", pivot=(2, -7.5, 4), rot=(0.9, 0.3, -0.2), boxes=[box((-1.5, 0, 0), (3, 5, 0), "leaf")])]
    if race == "orc":
        return [overlay("bandana", (-4, -8, -4), (8, 3, 8), "accent_cloth", 0.35),
                part("bandana_tail_r", pivot=(-1, -6.5, 4.4), rot=(0.4, 0.2, 0.25), boxes=[box((-1, 0, 0), (2, 4, 0), "accent_cloth")]),
                part("bandana_tail_l", pivot=(1, -6.5, 4.4), rot=(0.5, -0.2, -0.3), boxes=[box((-1, 0, 0), (2, 3, 0), "accent_cloth")])]
    if race == "angel":
        return [part("sunhat_brim", pivot=(0, -6.4, 0), rot=(0.08, 0, 0), boxes=[box((-6, 0, -6), (12, 0.5, 12), "linen")]),
                overlay("sunhat_crown", (-3.5, -9.5, -3.5), (7, 3.5, 7), "linen", 0.3),
                overlay("sunhat_band", (-3.5, -7, -3.5), (7, 0.8, 7), "gold", 0.5)]
    # hive: harvester drones get a carapace hood with feeler antennae
    return [overlay("carapace_hood", (-4, -8.5, -4), (8, 3, 8), "chitin_plate", 0.45),
            part("antenna_r", pivot=(-1.5, -8.6, -2.5), rot=(-0.6, 0, -0.35), boxes=[box((-0.5, -6, -0.5), (1, 6, 1), "antenna")]),
            part("antenna_l", pivot=(1.5, -8.6, -2.5), rot=(-0.6, 0, 0.35), boxes=[box((-0.5, -6, -0.5), (1, 6, 1), "antenna")])]


def gear_farmer(race):
    m = rig(f"gear_farmer_{race}")
    attach(m, "head", *farmer_hat(race))
    hive = race == "hive"
    attach(m, "body",
           overlay("smock", (-4, 0, -2), (8, 12, 4), "chitin_plate" if hive else "smock", 0.3),
           part("apron", pivot=(0, 4, -2.55), rot=(-0.04, 0, 0), boxes=[box((-3, 0, 0), (6, 8, 0), "apron")]),
           overlay("belt", (-4, 9, -2), (8, 1, 4), "leather_belt", 0.5),
           part("seed_pouch", pivot=(3.4, 9.6, -1), rot=(0, 0, -0.12), boxes=[box((-1, 0, -1), (2, 3, 2), "pouch")]),
           part("basket" if not hive else "bio_sac", pivot=(0, 2, 2.4), rot=(0.08, 0, 0),
                boxes=[box((-3, 0, 0), (6, 6, 3.5), "bio_sac" if hive else "wicker")]),
           part("basket_strap_r", pivot=(-2.5, 0, -2.4), boxes=[box((-0.5, 0, 0), (1, 3, 0), "leather")]),
           part("basket_strap_l", pivot=(2.5, 0, -2.4), boxes=[box((-0.5, 0, 0), (1, 3, 0), "leather")]))
    both_arms(m, lambda s, sx: [overlay(f"sleeve_{s}", (-3 if s == "r" else -1, -2, -2), (4, 4, 4),
                                        "chitin_plate" if hive else "smock", 0.35)])
    boots(m, "chitin_plate" if hive else "leather")
    return m


# ----------------------------------------------------------------------------- builder

def builder_hat(race):
    if race == "human":
        return [overlay("cap", (-4, -8.5, -4), (8, 3, 8), "leather", 0.4),
                part("cap_visor", pivot=(0, -6.1, -4.3), rot=(0.25, 0, 0), boxes=[box((-4, 0, -2.5), (8, 0.6, 2.5), "leather")])]
    if race == "dwarf":
        return [overlay("mining_helm", (-4, -8.8, -4), (8, 3.5, 8), "plate", 0.55),
                part("helm_rim", pivot=(0, -5.6, 0), boxes=[box((-5, 0, -5), (10, 0.6, 10), "plate")]),
                part("candle", pivot=(0, -9.2, -3.2), boxes=[box((-0.5, -2, -0.5), (1, 2, 1), "wax")],
                     children=[part("flame", pivot=(0, -2, 0), boxes=[box((-0.5, -1, -0.5), (1, 1, 1), "flame")])])]
    if race == "elf":
        return [overlay("circlet", (-4, -6.8, -4), (8, 1, 8), "wood", 0.5),
                part("circlet_leaf", pivot=(0, -6.6, -4.6), rot=(-0.2, 0, 0), boxes=[box((-1, -3, 0), (2, 3, 0), "leaf")])]
    if race == "orc":
        return [overlay("skull_cap", (-4, -8.5, -4), (8, 3, 8), "iron_scrap", 0.45),
                part("cap_tusk_r", pivot=(-3, -8.3, -1), rot=(-0.4, 0, -0.6), boxes=[box((-0.5, -3, -0.5), (1, 3, 1), "bone")]),
                part("cap_tusk_l", pivot=(3, -8.3, -1), rot=(-0.4, 0, 0.6), boxes=[box((-0.5, -3, -0.5), (1, 3, 1), "bone")])]
    if race == "angel":
        return [overlay("cap", (-4, -8.5, -4), (8, 2.5, 8), "linen", 0.4),
                overlay("cap_trim", (-4, -6.4, -4), (8, 0.8, 8), "gold", 0.55)]
    return [part("crest_ridge", pivot=(0, -8.4, 0), boxes=[box((-0.5, -2, -3.5), (1, 2, 7), "chitin_plate")])]


def gear_builder(race):
    m = rig(f"gear_builder_{race}")
    attach(m, "head", *builder_hat(race))
    hive = race == "hive"
    load = ([part("resin_lump_1", pivot=(-1.5, 2, 2.3), boxes=[box((-1.5, 0, 0), (3, 3, 2), "resin")]),
             part("resin_lump_2", pivot=(1.5, 4.5, 2.3), boxes=[box((-1, 0, 0), (2, 3, 1.5), "resin")])] if hive else
            [part("plank_bundle", pivot=(0, 0.5, 2.3), rot=(0, 0, 0.22), boxes=[
                box((-3.5, -2, 0), (2, 11, 1), "planks"), box((-1, -3, 0), (2, 12, 1), "planks"),
                box((1.5, -1.5, 0), (2, 10, 1), "planks")],
                children=[part("bundle_strap", pivot=(0, 4, 0), boxes=[box((-4, 0, -0.1), (8, 1, 1.2), "leather")])])])
    attach(m, "body",
           overlay("work_shirt", (-4, 0, -2), (8, 12, 4), "chitin_plate" if hive else "smock", 0.25),
           part("heavy_apron", pivot=(0, 1, -2.5), boxes=[box((-3.5, 0, 0), (7, 10, 0), "heavy_apron")]),
           overlay("tool_belt", (-4, 9, -2), (8, 1.5, 4), "tool_belt", 0.55),
           part("chisel", pivot=(-3.6, 10.3, -1.2), rot=(0.1, 0, 0.15), boxes=[box((-0.5, 0, -0.5), (1, 3, 1), "plate")]),
           part("trowel", pivot=(3.7, 10.3, 0.4), rot=(0, 0, -0.2), boxes=[box((-0.5, 0, -1), (1, 1.5, 2), "wood"),
                                                                        box((-0.5, 1.5, -1.5), (1, 2, 3), "plate")]),
           *load)
    both_arms(m, lambda s, sx: [overlay(f"glove_{s}", (-3 if s == "r" else -1, 6, -2), (4, 4, 4), "leather", 0.35)])
    both_legs(m, lambda s, sx: [overlay(f"knee_pad_{s}", (-2, 4, -2), (4, 2, 4), "leather", 0.35)])
    boots(m)
    return m


# ----------------------------------------------------------------------------- guard

def guard_helm(race):
    if race == "human":
        return [overlay("kettle_helm", (-4, -8.5, -4), (8, 4, 8), "plate", 0.55),
                part("kettle_brim", pivot=(0, -5, 0), rot=(0.05, 0, 0), boxes=[box((-5.5, 0, -5.5), (11, 0.7, 11), "plate")])]
    if race == "elf":
        return [overlay("leaf_helm", (-4, -8.5, -4), (8, 4.5, 8), "plate", 0.55),
                part("helm_crest", pivot=(0, -9, 0), rot=(-0.15, 0, 0), boxes=[box((-0.5, -3, -3.5), (1, 3, 7), "accent_cloth")]),
                part("cheek_r", pivot=(-4.3, -4, -2), rot=(0, 0.15, 0), boxes=[box((-0.5, 0, -2), (1, 3, 3), "plate")]),
                part("cheek_l", pivot=(4.3, -4, -2), rot=(0, -0.15, 0), boxes=[box((-0.5, 0, -2), (1, 3, 3), "plate")])]
    if race == "dwarf":
        horn = lambda s, sx: part(f"helm_horn_{s}", pivot=(sx * 4.3, -6.5, 0), rot=(-0.3, 0, sx * 0.9),
                                  boxes=[box((-0.75, -3, -0.75), (1.5, 3, 1.5), "bone")],
                                  children=[part(f"helm_horn_{s}_tip", pivot=(0, -3, 0), rot=(0, 0, -sx * 0.6),
                                                 boxes=[box((-0.5, -3, -0.5), (1, 3, 1), "bone")])])
        return [overlay("great_helm", (-4, -8, -4), (8, 8, 8), "great_helm", 0.6), horn("r", -1), horn("l", 1)]
    if race == "orc":
        spike = lambda n, x, z, r: part(n, pivot=(x, -8.7, z), rot=r, boxes=[box((-0.5, -3, -0.5), (1, 3, 1), "iron_spike")])
        return [overlay("spiked_cap", (-4, -8.5, -4), (8, 3.5, 8), "iron_scrap", 0.5),
                spike("cap_spike_c", 0, 0, (0, 0, 0)), spike("cap_spike_r", -2.5, 1, (0.2, 0, -0.35)),
                spike("cap_spike_l", 2.5, 1, (0.2, 0, 0.35)),
                part("jaw_guard", pivot=(0, -1.5, -4.5), boxes=[box((-3, 0, -0.5), (6, 2, 1), "iron_scrap")])]
    if race == "angel":
        wing = lambda s, sx: part(f"helm_wing_{s}", pivot=(sx * 4.4, -6.5, 0), rot=(0.2, sx * -0.35, sx * 0.25),
                                  boxes=[box((-0.5 if sx > 0 else -0.5, -5, 0), (0, 5, 4), "feather")])
        return [overlay("winged_helm", (-4, -8.5, -4), (8, 4, 8), "gold", 0.55), wing("r", -1), wing("l", 1)]
    crest = [part(f"crest_spine_{i}", pivot=(0, -8.6, -2.5 + i * 2.5), rot=(-0.5, 0, 0),
                  boxes=[box((-0.5, -3 + i * 0.5, -0.5), (1, 3 - i * 0.5, 1), "antenna")]) for i in range(3)]
    return [overlay("carapace_helm", (-4, -8.5, -4), (8, 4.5, 8), "chitin_plate", 0.55), *crest]


def gear_guard(race):
    m = rig(f"gear_guard_{race}")
    attach(m, "head", *guard_helm(race))
    hive = race == "hive"
    plate = "chitin_plate" if hive else "plate"
    body = [overlay("mail", (-4, 0, -2), (8, 12, 4), "chitin_plate" if hive else "mail", 0.35),
            overlay("guard_belt", (-4, 9, -2), (8, 1, 4), "leather_belt", 0.6),
            part("lantern", pivot=(4.7, 9.5, -0.5), boxes=[box((-1, 0, -1), (2, 3, 2), "lantern")],
                 children=[part("lantern_hook", pivot=(0, 0, 0), boxes=[box((-0.5, -1, -0.5), (1, 1, 1), "plate")])])]
    if not hive:
        body += [part("tabard_front", pivot=(0, 1, -2.7), boxes=[box((-3, 0, 0), (6, 11, 0), "tabard")]),
                 part("tabard_back", pivot=(0, 1, 2.7), boxes=[box((-3, 0, 0), (6, 11, 0), "tabard")])]
    attach(m, "body", *body)
    both_arms(m, lambda s, sx: [
        part(f"pauldron_{s}", pivot=(sx * 1, -2.4, 0), rot=(0, 0, sx * -0.18),
             boxes=[box((-3 if s == "r" else -2, -1, -2.5), (5, 2.5, 5), plate)]),
        overlay(f"vambrace_{s}", (-3 if s == "r" else -1, 5, -2), (4, 5, 4), plate, 0.35)])
    both_legs(m, lambda s, sx: [overlay(f"greave_{s}", (-2, 5, -2), (4, 7, 4), plate, 0.35)])
    return m


# ----------------------------------------------------------------------------- imp workers

def imp_workers(imp_base, belt_and_loincloth):
    farmer = imp_base("imp_farmer")
    belt_and_loincloth(farmer, cloth="rag")
    attach(farmer, "head",
           part("hat_brim", pivot=(0, -7.4, 0), rot=(0.12, 0, 0.08), boxes=[box((-5.5, 0, -5.5), (11, 0.6, 11), "straw")]),
           overlay("hat_crown", (-2.5, -9.6, -2.5), (5, 2.4, 5), "straw", 0.25),
           overlay("hat_band", (-2.5, -8, -2.5), (5, 0.7, 5), "accent_cloth", 0.4))
    attach(farmer, "body", overlay("smock", (-3, 0, -1.5), (6, 9, 3), "smock", 0.3),
           part("seed_sack", pivot=(2.6, 9.5, -1), rot=(0, 0, -0.2), boxes=[box((-1, 0, -1), (2, 2.5, 2), "pouch")]))

    builder = imp_base("imp_builder")
    belt_and_loincloth(builder, cloth="rag_dark")
    attach(builder, "head", overlay("cap", (-3.5, -7.6, -3.5), (7, 2.5, 7), "leather", 0.35))
    attach(builder, "body", overlay("tool_belt", (-2.5, 8.6, -1.5), (5, 1.5, 3), "tool_belt", 0.55),
           part("chisel", pivot=(-2.4, 9.8, -1.4), rot=(0.1, 0, 0.2), boxes=[box((-0.5, 0, -0.5), (1, 2.5, 1), "plate")]),
           part("plank_bundle", pivot=(0, 1, 1.7), rot=(0, 0, 0.3), boxes=[
               box((-2.5, -1, 0), (1.5, 8, 1), "planks"), box((-0.75, -2, 0), (1.5, 9, 1), "planks"),
               box((1, -1, 0), (1.5, 7, 1), "planks")]))
    for side in ("r", "l"):
        attach(builder, f"forearm_{side}", overlay(f"glove_{side}", (-1, 4, -1), (2, 3, 2), "leather", 0.3))

    guard = imp_base("imp_guard")
    belt_and_loincloth(guard, cloth="rag_dark")
    attach(guard, "head", overlay("kettle_cap", (-3.5, -7.5, -3.5), (7, 3, 7), "plate", 0.45),
           part("kettle_brim", pivot=(0, -4.9, 0), boxes=[box((-4.5, 0, -4.5), (9, 0.6, 9), "plate")]))
    attach(guard, "body", overlay("mail", (-3, 0, -1.5), (6, 9, 3), "mail", 0.35),
           part("lantern", pivot=(3.2, 9, -0.4), boxes=[box((-1, 0, -1), (2, 2.5, 2), "lantern")]))
    for side, arm in (("r", "right_arm"), ("l", "left_arm")):
        sx = -1 if side == "r" else 1
        attach(guard, arm, part(f"pauldron_{side}", pivot=(0, -1, 0), rot=(0, 0, sx * -0.2),
                                boxes=[box((-1.5, -1, -1.5), (3, 2, 3), "plate")]))
    return [farmer, builder, guard]


def all_gear():
    return [{"farmer": gear_farmer, "builder": gear_builder, "guard": gear_guard}[role](race)
            for role in ROLES for race in HUMANOID_RACES]


# ----------------------------------------------------------------------------- materials

def bind(ns):
    """Receives the model and painting helpers from units.py."""
    globals().update(ns)


def worker_material(mat, side, x, y, w, h, pal, rng):
    """Materials for worker and guard gear. Returns NotImplemented for anything else."""
    top = side == "top"
    if mat == "straw":
        c = mix(hexc(pal["straw"]), hexc(pal["straw_dark"]), 0.15 + 0.35 * (((x + y * 2) % 4) / 3))   # diagonal plait
        if rng.random() < 0.1:
            c = shade(c, 1.15)                                            # sun-bleached strands
        if edge(x, y, w, h) and not top:
            c = shade(c, 0.82)
        return shade(c, 1.1) if top else c
    if mat == "wicker":
        band = (y // 2) % 2
        c = hexc(pal["straw"] if band else pal["straw_dark"])
        if x % 3 == 0:
            c = mix(c, hexc(pal["wood_dark"]), 0.45)                     # upright stakes
        if y == 0 and side in ("front", "back", "left", "right"):
            c = shade(hexc(pal["straw"]), 1.12)                           # rolled rim
        return shade(c, rng.uniform(0.92, 1.06))
    if mat in ("smock", "linen"):
        return cloth_px(pal, rng, x, y, w, h, side, base="linen" if mat == "linen" else "cloth")
    if mat == "accent_cloth":
        c = cloth_px(pal, rng, x, y, w, h, side, base="accent")
        return c
    if mat == "apron":
        c = cloth_px(pal, rng, x, y, w, h, side, base="apron", fray=1)
        if c is not None and side == "front" and y == h // 2 and 1 <= x <= w - 2:
            c = shade(c, 0.8)                                             # pocket seam
        return c
    if mat == "wool":
        c = hexc(pal["accent"])
        c = shade(c, 1.1 if x % 2 == 0 else 0.88)                         # knit ribs
        if y % 2 == 1:
            c = shade(c, 0.95)
        return shade(c, 1.1) if top else c
    if mat in ("leafweave", "leaf"):
        c = mix(hexc(pal["leaf"]), hexc(pal["leaf_dark"]), rng.uniform(0, 0.5))
        if (x + y) % 3 == 0:
            c = mix(c, hexc(pal["leaf_dark"]), 0.5)                       # veins
        if rng.random() < 0.08:
            c = shade(c, 1.3)
        if mat == "leaf" and side in ("front", "back") and (x in (0, w - 1)) and y < 2:
            return None                                                   # pointed leaf tip
        return c
    if mat == "gold":
        t = y / max(1, h - 1)
        c = mix(hexc("f0d070"), hexc("a07818"), 0.2 + 0.5 * t)
        if rng.random() < 0.08:
            c = shade(c, 1.2)
        return c
    if mat == "pouch":
        c = leather_px(pal, rng, x, y, w, h, side)
        if y == 0 and side != "top":
            c = shade(c, 0.75)                                            # flap
        return c
    if mat == "heavy_apron":
        c = leather_px(pal, rng, x, y, w, h, side, stitch_row=h - 1)
        if side == "front" and (y in (5, 6)) and 1 <= x <= w - 2:
            c = shade(c, 0.82 if y == 5 else 1.05)                        # tool pocket
        if side == "front" and x == w // 2 and y >= 5:
            c = shade(c, 0.85)
        return c
    if mat == "tool_belt":
        c = leather_px(pal, rng, x, y, w, h, side)
        if side in ("front", "back", "left", "right") and x % 3 == 0:
            c = shade(c, 0.7)                                             # tool loops
        if side == "front" and x == w // 2:
            c = hexc("c4c8ce")                                            # buckle
        return c
    if mat == "planks":
        c = mix(hexc(pal["wood"]), hexc(pal["wood_dark"]), 0.15 + 0.25 * ((y * 3 + x) % 5 == 0))
        if side in ("front", "back", "left", "right") and x % 2 == 0:
            c = shade(c, 0.92)                                            # grain
        if rng.random() < 0.04:
            c = mix(c, hexc(pal["wood_dark"]), 0.7)                       # knot
        if edge(x, y, w, h):
            c = shade(c, 0.8)
        return shade(c, 1.15) if top else c
    if mat == "wood":
        c = mix(hexc(pal["wood"]), hexc(pal["wood_dark"]), rng.uniform(0.1, 0.4))
        return shade(c, 0.9) if x % 2 else c
    if mat == "wax":
        return shade(hexc("ece0c0"), 1.0 if top else 0.92)
    if mat == "flame":
        return glow(hexc("ffd060") if top or y == 0 else hexc("ff8a20"))
    if mat in ("plate", "great_helm"):
        c = iron_px(pal, rng, x, y, w, h, side, rivets=(mat == "great_helm"))
        if side in ("front", "back", "left", "right") and y == 1:
            c = mix(c, hexc("eef2f6"), 0.4)                               # polished highlight
        if mat == "great_helm" and side == "front" and y == 3 and 1 <= x <= w - 2 and x != w // 2:
            return hexc("0a0a0c")                                         # eye slit
        if mat == "great_helm" and side == "front" and x == w // 2 and y >= 3:
            c = shade(c, 1.2)                                             # nasal bar
        return c
    if mat == "mail":
        ring = (x + (y % 2)) % 2 == 0
        c = mix(hexc(pal["iron"]), hexc(pal["iron_dark"]), 0.25 if ring else 0.75)
        if rng.random() < 0.05:
            c = mix(c, hexc(pal["rust"]), 0.5)
        return shade(c, 1.1) if top else c
    if mat == "tabard":
        c = cloth_px(pal, rng, x, y, w, h, side, base="accent", fray=1)
        if c is None:
            return None
        if x in (0, w - 1) or y == 0:
            return mix(hexc(pal["trim"]), c, 0.2)                         # trimmed border
        if side in ("front", "back") and x in (w // 2 - 1, w // 2) and 2 <= y <= 7:
            return mix(hexc(pal["trim"]), c, 0.3)                         # faction stripe
        return c
    if mat == "lantern":
        if edge(x, y, w, h) or side in ("top", "bottom"):
            return shade(hexc(pal["iron_dark"]), 0.9)
        return glow(hexc(pal["glow_c"]))
    if mat == "feather":
        c = mix(hexc("f4f0e8"), hexc(pal["trim"]), 0.25 * (y % 3 == 0))
        if x % 2 == 0:
            c = shade(c, 0.92)
        return c
    if mat in ("chitin_plate", "antenna"):
        t = y / max(1, h - 1)
        c = mix(hexc(pal["chitin"]), hexc(pal["chitin_dark"]), 0.2 + 0.5 * t)
        if (x + y) % 5 == 0:
            c = mix(c, hexc(pal["accent"]), 0.35)                         # sheen
        if mat == "chitin_plate" and y % 3 == 2 and side != "top":
            c = shade(c, 0.75)                                            # segment ridges
        return shade(c, 1.15) if top else c
    if mat == "bio_sac":
        c = mix(hexc(pal["chitin"]), hexc(pal["accent"]), 0.4)
        if not edge(x, y, w, h) and (x + y) % 3 != 0:
            return glow(mix(hexc(pal["glow_c"]), c, 0.4))
        return c
    if mat == "resin":
        c = mix(hexc("c88a2a"), hexc("7a4a10"), rng.uniform(0, 0.5))
        if rng.random() < 0.2:
            return glow(shade(c, 1.3))
        return c
    return NotImplemented
