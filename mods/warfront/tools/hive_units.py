"""Hive unit models: the Lancer-Drone (spearman) and the Deepmaw (beast). Imported by units.py.

Both are built on the vanilla humanoid rig (head, hat, body, arms, legs) so the game can animate them with the
same code as other soldiers; extra limbs hang off the body. They follow the owner's references
(docs/art-reference/hive-spearman.png and hive-beast.png), recoloured to the Hive's sculk look: dark teal
chitin with glowing veins.
"""
import math

# part, box, mirror, attach, hexc, mix, shade, glow and edge are injected by units.py (see bind()).

PALETTES = {
    # Player Hive: sculk-dark teal chitin, cyan veins, green-cyan eyes.
    "hive": dict(chitin="143a44", chitin_dark="08191f", chitin_hi="2a6670", vein="29dfeb", eye="49ffc8",
                 eye_hi="c8fff0", bone="b9c4bc", bone_dark="56625c", wrap="3c2a5a", wrap_dark="1e1430",
                 straw="5a6a4a", straw_dark="2e3a26", haft="2a2018", blade="b8c8cc", blade_edge="e8f4f4"),
    # The Swarm: black chitin veined with acid green.
    "the_swarm": dict(chitin="1c2622", chitin_dark="0b1210", chitin_hi="3a4a40", vein="8aff3a", eye="b8ff4a",
                      eye_hi="f0ffd0", bone="a8a890", bone_dark="4a4a3a", wrap="4a1a1a", wrap_dark="240c0c",
                      straw="5a5a3a", straw_dark="2a2a1a", haft="201810", blade="9aa4a0", blade_edge="d8e0dc"),
}


def bind(ns):
    """Receives the model and painting helpers from units.py."""
    globals().update(ns)


def _leg(side_x, thigh_mat, shin_mat, spikes=True):
    """Digitigrade insect leg: thigh, backward-bent shin with spurs, ankle, and a hooked foot."""
    shin_children = [part("ankle_r", pivot=(0, 5.5, 0), rot=(-0.8, 0, 0),
                          boxes=[box((-1.1, 0, -1.1), (2.2, 3.5, 2.2), shin_mat)], children=[
                              part("foot_r", pivot=(0, 3.5, 0), rot=(0.3, 0, 0),
                                   boxes=[box((-1.5, -0.5, -2.25), (3, 1, 3.5), shin_mat)],
                                   children=[part("toe_r", pivot=(0, 0, -2), rot=(0.4, 0, 0),
                                                  boxes=[box((-0.5, -0.5, -2), (1, 1, 2), "hook")])])])]
    if spikes:
        shin_children += [part(f"shin_spur_r{i}", pivot=(0, 1.5 + i * 2, 0.9), rot=(1.1, 0, 0),
                               boxes=[box((-0.5, -2, -0.5), (1, 2, 1), "spike")]) for i in range(2)]
    return part("right_leg", pivot=(side_x, 12, 0), rot=(-0.6, 0, 0),
                boxes=[box((-1.75, 0, -1.75), (3.5, 5, 3.5), thigh_mat),
                       box((-2, 0.5, -2.1), (4, 3, 1), "hb_shell")], children=[
                    part("shin_r", pivot=(0, 5, 0), rot=(1.1, 0, 0),
                         boxes=[box((-1.5, 0, -1.5), (3, 5.5, 3), shin_mat)], children=shin_children)])


# ----------------------------------------------------------------------------- Lancer-Drone

def hive_lancer():
    """Hive spearman: a sturdy, armored four-armed ant warrior with a double-bladed polearm and a throwing blade."""
    head = part("head", pivot=(0, 0, -0.5), boxes=[
        box((-2.5, -7.5, -3), (5, 4.5, 6), "hc_head"),
        box((-2.25, -3.5, -3.75), (4.5, 3, 4.75), "hc_head"),
    ], children=[
        part("eye_r", pivot=(-2.4, -5.6, -1.6), rot=(0, 0.25, 0), boxes=[box((-0.8, -1.5, -1.6), (1, 3, 3.2), "eye")]),
        part("mandible_r", pivot=(-1.2, -0.6, -3.4), rot=(0.25, 0.3, 0), boxes=[box((-0.5, 0, -0.5), (1, 2, 1), "mandible")],
             children=[part("mandible_r_tip", pivot=(0, 2, 0), rot=(-0.8, 0, 0.35),
                            boxes=[box((-0.5, 0, -0.5), (1, 1.5, 1), "mandible")])]),
        part("antenna_r", pivot=(-1.2, -7.4, -2.4), rot=(-0.55, -0.2, -0.25),
             boxes=[box((-0.25, -7, -0.25), (0.5, 7, 0.5), "antenna")],
             children=[part("antenna_r_tip", pivot=(0, -7, 0), rot=(-0.75, 0, -0.15),
                            boxes=[box((-0.25, -6, -0.25), (0.5, 6, 0.5), "antenna")])]),
    ])
    head["children"] += [mirror(c) for c in list(head["children"])]

    lower_arm = part("lower_arm_r", pivot=(-3.75, 5.5, -0.5), rot=(-0.55, 0, 0.35),
                     boxes=[box((-1.25, 0, -1.25), (2.5, 4.5, 2.5), "hc_plate")], children=[
                         part("lower_forearm_r", pivot=(0, 4.5, 0), rot=(-0.9, 0, -0.25),
                              boxes=[box((-1.1, 0, -1.1), (2.2, 4.5, 2.2), "hc_dark")], children=[
                                  part("lower_hand_r", pivot=(0, 4.5, 0),
                                       boxes=[box((-1.25, 0, -1.25), (2.5, 2, 2.5), "hc_dark")])])])
    lower_arm_l = mirror(lower_arm)
    attach({"parts": [lower_arm_l]}, "lower_hand_l",
           part("chatkcha", pivot=(0, 1.2, -0.6), rot=(0.2, 0, 0.5), boxes=[box((-2, -0.25, -2), (4, 0.5, 4), "chatkcha")]))

    body = part("body", rot=(0.08, 0, 0), boxes=[
        box((-4, 0, -2.5), (8, 5.5, 5), "hc_plate"),                      # chest
        box((-3.5, 0.5, -3.1), (7, 4, 0.75), "hb_shell"),                 # breast plate
        box((-3.5, 0, 2.2), (7, 7, 1.3), "hb_shell"),                     # back shell
        box((-2.25, 5.5, -1.75), (4.5, 3.5, 3.5), "hc_dark"),             # waist
        box((-3.5, 9, -2.25), (7, 3, 4.5), "hc_plate"),                   # hips
    ], children=[
        part("wrap", boxes=[box((-5, -0.6, -3.2), (10, 2.6, 6.4), "wrap")], children=[
            part("wrap_tail", pivot=(1.5, 2, 3.2), rot=(0.25, 0, 0.12), boxes=[box((-2, 0, 0), (4, 7, 0), "wrap_rag")])]),
        part("skirt_front", pivot=(0, 11, -2.4), rot=(-0.06, 0, 0), boxes=[box((-3.5, 0, 0), (7, 6, 0), "grass")]),
        part("skirt_back", pivot=(0, 11, 2.4), rot=(0.06, 0, 0), boxes=[box((-3.5, 0, 0), (7, 6, 0), "grass")]),
        lower_arm, lower_arm_l,
    ])

    arm = part("right_arm", pivot=(-5.5, 2, 0), boxes=[box((-2, -2, -1.75), (3.5, 5.5, 3.5), "hc_plate"),
                                                       box((-2.5, -2.75, -2.25), (4, 2.5, 4.5), "hb_shell")], children=[
        part("forearm_r", pivot=(-0.25, 3.5, 0), rot=(-0.15, 0, 0),
             boxes=[box((-1.5, 0, -1.5), (3, 6.5, 3), "hc_dark")], children=[
                 part("forearm_r_spur", pivot=(0, 2, 1.4), rot=(1.0, 0, 0), boxes=[box((-0.5, -2.5, -0.5), (1, 2.5, 1), "spike")]),
                 part("hand_r", pivot=(0, 6.5, 0), boxes=[box((-1.5, 0, -1.5), (3, 2.5, 3), "hc_dark")])])])
    arm_l = mirror(arm)
    # The gythka: a long haft with a curved blade at each end, gripped in the upper right hand.
    attach({"parts": [arm]}, "hand_r", part("gythka", pivot=(0, 1, -0.5), rot=(1.35, 0, 0.15),
                               boxes=[box((-0.5, -15, -0.5), (1, 30, 1), "haft")], children=[
                                   part("gythka_top", pivot=(0, -15, 0), rot=(-0.2, 0, 0),
                                        boxes=[box((-0.5, -7, -1.5), (1, 7, 3), "blade")]),
                                   part("gythka_bottom", pivot=(0, 15, 0), rot=(0.2, 0, 0),
                                        boxes=[box((-0.5, 0, -1.5), (1, 6, 3), "blade")])]))

    leg = _leg(-2, "hc_plate", "hc_dark")
    return {"id": "hive_lancer", "tex": [128, 128],
            "parts": [head, part("hat"), body, arm, arm_l, leg, mirror(leg)]}


# ----------------------------------------------------------------------------- Deepmaw (beast)

def _pincer():
    """One claw arm: plated shoulder, upper arm held out to the side, forearm bent forward, and a big pincer
    whose lower jaw (claw_r) opens."""
    return part("right_arm", pivot=(-5.5, 2.5, -8), rot=(0, 0, 1.05),
                boxes=[box((-2.25, -1, -2.25), (4.5, 6, 4.5), "hb_plate"),
                       box((-3, -2.5, -3), (6, 3, 6), "hb_shell")], children=[
                    part("forearm_r", pivot=(0, 5.5, 0), rot=(-1.25, 0, 0.3),
                         boxes=[box((-2, 0, -2), (4, 6.5, 4), "hb_plate")], children=[
                             *[part(f"forearm_r_spike{i}", pivot=(-1.9, 1.5 + i * 2.5, 0), rot=(0, 0, 0.9),
                                    boxes=[box((-0.5, -2.5, -0.5), (1, 2.5, 1), "spike")]) for i in range(2)],
                             part("pincer_r", pivot=(0, 6.5, 0), rot=(0, 0, 0.2),
                                  boxes=[box((-3, 0, -2.5), (6, 6.5, 5), "hb_claw")], children=[
                                      part("pincer_r_fixed", pivot=(0, 6.5, -1.25), rot=(0.1, 0, 0),
                                           boxes=[box((-2, 0, -1.25), (4, 7, 2.5), "hb_claw")], children=[
                                               part("pincer_r_tip", pivot=(0, 7, 0), rot=(0.5, 0, 0),
                                                    boxes=[box((-1.25, 0, -1), (2.5, 3, 2), "hook")])]),
                                      part("claw_r", pivot=(0, 6, 1.5), rot=(0.45, 0, 0),
                                           boxes=[box((-1.75, 0, -1.25), (3.5, 7, 2.5), "hb_claw")], children=[
                                               part("claw_r_tip", pivot=(0, 7, 0), rot=(-0.6, 0, 0),
                                                    boxes=[box((-1.1, 0, -0.9), (2.2, 3, 1.8), "hook")])])])])])


# Walking legs: four per side under the carapace, splayed from front to back. Each leg rises to a high knee
# and comes down to a hooked tip, like a crab or lobster.
LEG_Z = (-7, -3.5, 0, 3.5)
LEG_YAW = (-0.65, -0.2, 0.2, 0.6)


def _walking_leg(i):
    return part(f"leg_r{i}", pivot=(-4, 2, LEG_Z[i]), rot=(0, LEG_YAW[i], 0), children=[
        part(f"leg_r{i}_upper", rot=(0, 0, 2.2), boxes=[box((-0.75, 0, -0.75), (1.5, 7, 1.5), "hb_plate")], children=[
            part(f"leg_r{i}_lower", pivot=(0, 7, 0), rot=(0, 0, -1.95),
                 boxes=[box((-0.6, 0, -0.6), (1.2, 10, 1.2), "hb_leg")], children=[
                     part(f"leg_r{i}_tip", pivot=(0, 10, 0), rot=(0, 0, 0.15),
                          boxes=[box((-0.4, 0, -0.4), (0.8, 1.5, 0.8), "hook")])])])])


def hive_beast():
    """Hive beast: a lobster centaur. The lower body is a low carapace on eight crawling legs with a segmented
    tail; an upright plated torso rises from its front, with claw arms held out to the sides and a head with
    eye stalks, antennae and tusk-mandibles. Sculk-dark shell with glowing veins and a finned crest."""
    # Head after the reference: a tall spiked crown with two horns sweeping out, a narrow snout with a cluster of
    # small eyes under a heavy brow, broad bone mandible-blades curving down past the chest, and thin fangs between.
    head = part("head", pivot=(0, -0.5, -8.5), boxes=[
        box((-2.5, -4.5, -2.5), (5, 4.5, 5), "hb_head"),                  # skull
        box((-2.75, -5, -4.5), (5.5, 1.5, 2.5), "hb_shell"),              # heavy brow, overhanging the eyes
        box((-2, -3.5, -3.6), (4, 1.4, 1.1), "hb_leg"),                   # dark recess under the brow
        *[box((x, -3.2, -3.85), (0.6, 0.6, 0.3), "eye") for x in (-1.7, -0.75, 0.15, 1.1)],
        box((-1.75, -2.1, -6), (3.5, 2.6, 3.5), "hb_head"),               # snout, jutting forward
        box((-1.25, -2.5, -5.75), (2.5, 0.5, 3), "hb_shell"),             # snout ridge
        box((-1.5, 0.5, -5.5), (3, 1, 3), "hb_leg"),                      # dark mouth under the snout
    ], children=[
        part("crown_c", pivot=(0, -4.5, -1.5), rot=(-0.45, 0, 0), boxes=[box((-1.75, -9, -0.5), (3.5, 9, 1), "frill")], children=[
            *[part(f"crown_spike_{i}", pivot=(0, -2.5 - i * 3, 0.5), rot=(0.9, 0, 0),
                   boxes=[box((-0.4, -2, -0.4), (0.8, 2, 0.8), "spike")]) for i in range(2)]]),
        part("horn_r", pivot=(-2.25, -4, -1), rot=(-0.2, 0, -0.8), boxes=[box((-1, -6, -0.6), (2, 6, 1.2), "frill")], children=[
            part("horn_r_tip", pivot=(0, -6, 0), rot=(0, 0, 0.25), boxes=[box((-0.5, -4, -0.5), (1, 4, 1), "spike")])]),
        part("cheek_r", pivot=(-2.6, -2.2, -3.6), rot=(0, 0.5, 0.15), boxes=[box((-1, -2, -1), (1, 3.5, 3.5), "hb_shell")]),
        part("mandible_r", pivot=(-1.9, 0.5, -5), rot=(0.25, 0, 0.5),
             boxes=[box((-1.1, 0, -0.5), (2.2, 6.5, 1), "mandible")],
             children=[part("mandible_r_tip", pivot=(0, 6.5, 0), rot=(-0.3, 0, -0.75),
                            boxes=[box((-0.85, 0, -0.5), (1.7, 6, 1), "mandible")], children=[
                                part("mandible_r_point", pivot=(0, 6, 0), rot=(0, 0, -0.4),
                                     boxes=[box((-0.5, 0, -0.5), (1, 2.5, 1), "hook")])])]),
        part("fang_r", pivot=(-0.9, 1.3, -5.4), rot=(0.1, 0, 0.12), boxes=[box((-0.25, 0, -0.25), (0.5, 4.5, 0.5), "mandible")]),
    ])
    head["children"] += [mirror(c) for c in list(head["children"]) if c["name"].endswith("_r")]
    head["children"].append(part("fang_c", pivot=(0, 1.4, -5.6), rot=(0.12, 0, 0),
                                 boxes=[box((-0.25, 0, -0.25), (0.5, 5.5, 0.5), "mandible")]))

    tail_fan = part("tail_fan", pivot=(0, 0, 3), rot=(-0.15, 0, 0), boxes=[box((-1.5, -0.5, 0), (3, 1, 4), "hb_shell")], children=[
        part("tail_fan_r", pivot=(-1.5, 0, 0), rot=(0, 0.45, 0), boxes=[box((-2.5, -0.5, 0), (2.5, 1, 3.5), "hb_shell")]),
        part("tail_fan_l", pivot=(1.5, 0, 0), rot=(0, -0.45, 0), boxes=[box((0, -0.5, 0), (2.5, 1, 3.5), "hb_shell")]),
    ])
    seg4 = part("tail_4", pivot=(0, 0, 3.5), rot=(-0.12, 0, 0), boxes=[box((-2, -1.25, 0), (4, 2.75, 3), "hb_plate")], children=[tail_fan])
    seg3 = part("tail_3", pivot=(0, 0, 3.5), rot=(-0.1, 0, 0), boxes=[box((-2.5, -1.5, 0), (5, 3.25, 3.5), "hb_plate")], children=[seg4])
    seg2 = part("tail_2", pivot=(0, 0, 4), rot=(-0.08, 0, 0), boxes=[box((-3, -1.75, 0), (6, 3.75, 3.5), "hb_plate")], children=[seg3])

    leg = _walking_leg
    # The body lies flat and low: a long carapace with a segmented tail curling slightly down behind it.
    body = part("body", pivot=(0, 15, 0), boxes=[
        box((-4.5, -3, -10), (9, 5.5, 14), "hb_shell"),
        box((-3.5, 2.5, -9), (7, 1, 12), "hb_belly"),
    ], children=[
        # The upright torso rises from the front of the carapace, like a centaur's.
        part("torso", pivot=(0, -2.5, -7), rot=(0.1, 0, 0), boxes=[
            box((-3, -6, -2.5), (6, 6.5, 5), "hb_plate"),
            box((-4.5, -13.5, -3), (9, 7.5, 6), "hb_plate"),
            box((-4, -12.5, -3.6), (8, 5, 1), "hb_shell"),
        ], children=[
            *[part(f"torso_spike_{i}", pivot=(-2 + i * 2, -11 + abs(i - 1) * 0.5, 3), rot=(0.9, 0, 0),
                   boxes=[box((-0.4, -2.5, -0.4), (0.8, 2.5, 0.8), "spike")]) for i in range(3)]]),
        *[part(f"crest_{i}", pivot=(0, -3, -3.5 + i * 3), rot=(-0.25, 0, 0),
               boxes=[box((-0.25, -4 + abs(i - 1), 0), (0.5, 4 - abs(i - 1), 3), "fin")]) for i in range(3)],
        part("tail", pivot=(0, -0.5, 4), rot=(-0.05, 0, 0), boxes=[box((-3.5, -2, 0), (7, 4.25, 4), "hb_plate")], children=[seg2]),
        *[leg(i) for i in range(4)], *[mirror(leg(i)) for i in range(4)],
    ])

    arm = _pincer()
    return {"id": "hive_beast", "tex": [128, 128],
            "parts": [head, part("hat"), body, arm, mirror(arm), part("right_leg"), part("left_leg")]}


def models():
    return [hive_lancer(), hive_beast()]


# ----------------------------------------------------------------------------- materials

def _side_shade(c, side):
    if side == "top":
        return shade(c, 1.15)
    if side == "bottom":
        return shade(c, 0.7)
    if side in ("left", "right"):
        return shade(c, 0.88)
    return c


def _vein(pal, x, y, side, salt):
    """Sculk veins: thin branching lines that glow."""
    if side not in ("front", "back"):
        return None
    # Wandering vertical lines that step sideways every few rows, like roots.
    if (x + (y // 3) * 2 + salt) % 11 == 0:
        return glow(hexc(pal["vein"]))
    return None


def hive_material(mat, side, x, y, w, h, pal, rng):
    """Materials for Hive units. Returns NotImplemented for anything else."""
    if mat in ("hc_plate", "hb_plate"):
        t = y / max(1, h - 1)
        c = mix(hexc(pal["chitin_hi"]), hexc(pal["chitin"]), 0.35 + 0.55 * t)
        if y % 3 == 2 and side != "top":
            c = mix(c, hexc(pal["chitin_dark"]), 0.6)                     # segment ridges
        c = shade(c, rng.uniform(0.93, 1.06))
        v = _vein(pal, x, y, side, 1 if mat == "hc_plate" else 2)
        return v if v else _side_shade(c, side)
    if mat in ("hc_dark", "hb_leg"):
        c = mix(hexc(pal["chitin_dark"]), hexc(pal["chitin"]), rng.uniform(0.1, 0.45))
        if y in (0, h - 1):
            c = shade(c, 0.75)                                            # joints
        return _side_shade(c, side)
    if mat in ("hc_head", "hb_head"):
        c = mix(hexc(pal["chitin_hi"]), hexc(pal["chitin"]), 0.3 + 0.5 * (y / max(1, h - 1)))
        if side == "top":
            c = shade(c, 1.2)
        if mat == "hb_head":
            v = _vein(pal, x, y, side, 3)
            if v:
                return v
        return shade(c, rng.uniform(0.95, 1.05))
    if mat == "hb_belly":
        c = mix(hexc(pal["chitin_dark"]), hexc(pal["chitin"]), 0.4)
        if y % 2 == 0:
            c = mix(c, hexc(pal["chitin_hi"]), 0.35)                      # banded underplates
        if side in ("front", "back") and y % 6 == 1 and (x + y) % 7 == 3:
            return glow(hexc(pal["vein"]))
        return _side_shade(shade(c, rng.uniform(0.94, 1.05)), side)
    if mat == "hb_shell":
        # Domed carapace: lighter on top, a dark rim, mottled, with a few glowing vein cracks.
        c = mix(hexc(pal["chitin_hi"]), hexc(pal["chitin"]), 0.2 + 0.6 * (y / max(1, h - 1)))
        if side == "top":
            c = shade(hexc(pal["chitin_hi"]), 1.08)
        if x in (0, w - 1) or y == h - 1:
            c = mix(c, hexc(pal["chitin_dark"]), 0.55)                    # rim
        if rng.random() < 0.12:
            c = shade(c, 0.85)                                            # mottling
        if side in ("top", "back") and 0 < x < w - 1 and (x + (y // 3) * 2 + 4) % 9 == 0:
            return glow(hexc(pal["vein"]))                                # vein cracks, like roots
        return _side_shade(shade(c, rng.uniform(0.95, 1.05)), side)
    if mat == "hb_claw":
        # Heavy pincer: chitin fading to bone toward the tips, with a toothed edge.
        t = y / max(1, h - 1)
        c = mix(hexc(pal["chitin"]), hexc(pal["bone_dark"]), 0.15 + 0.5 * t)
        if side in ("left", "right") and x % 2 == 0 and y > h // 3:
            c = mix(c, hexc(pal["bone"]), 0.6)                            # teeth
        if side == "front" and y == h // 2 and x == w // 2:
            return glow(hexc(pal["vein"]))
        return _side_shade(shade(c, rng.uniform(0.93, 1.06)), side)
    if mat == "eye":
        facet = (x + y) % 2 == 0
        return glow(hexc(pal["eye"]) if facet else mix(hexc(pal["eye"]), hexc(pal["eye_hi"]), 0.5))
    if mat == "antenna":
        return hexc(pal["chitin_hi"]) if (y // 2) % 2 == 0 else hexc(pal["chitin_dark"])
    if mat == "mandible":
        t = y / max(1, h - 1)
        c = mix(hexc(pal["bone"]), hexc(pal["bone_dark"]), 0.15 + 0.6 * t)
        return _side_shade(shade(c, rng.uniform(0.94, 1.06)), side)
    if mat in ("spike", "hook"):
        t = y / max(1, h - 1)
        if mat == "spike":
            t = 1 - t                                                     # spikes grow upward: y=0 is the tip
        c = mix(hexc(pal["chitin_dark"]), hexc(pal["bone"]), 0.2 + 0.7 * t)
        return shade(c, rng.uniform(0.92, 1.08))
    if mat in ("frill", "fin"):
        # "fin" is the same plate turned edge-on to the front: its broad faces are the left and right sides.
        if side not in (("left", "right") if mat == "fin" else ("front", "back")):
            return hexc(pal["chitin_dark"])
        # A row of pointed teeth along the top edge; solid plate below.
        tooth = abs((x % 3) - 1)
        if y < tooth * 2:
            return None
        c = mix(hexc(pal["chitin_hi"]), hexc(pal["chitin"]), y / max(1, h - 1))
        if x == w // 2:
            c = mix(c, hexc(pal["chitin_dark"]), 0.5)                     # central rib
        if x == w // 2 and y > 3 and y % 2 == 0:
            return glow(hexc(pal["vein"]))
        return shade(c, 0.85 if side == "back" else 1.0)
    if mat in ("wrap", "wrap_rag"):
        if mat == "wrap_rag" and y > h - 3 and (x * 5 + y) % 3 == 0:
            return None                                                   # frayed hem
        c = mix(hexc(pal["wrap"]), hexc(pal["wrap_dark"]), rng.uniform(0, 0.45))
        if (x + y) % 5 == 0:
            c = shade(c, 0.8)                                             # folds
        return _side_shade(c, side)
    if mat == "grass":
        if side not in ("front", "back"):
            return None
        if y > h - 2 - (x * 3) % 3 or x % 3 == 2 and y > h // 2:
            return None                                                   # ragged strands
        c = mix(hexc(pal["straw"]), hexc(pal["straw_dark"]), (x % 3) / 3 + rng.uniform(0, 0.2))
        return c
    if mat == "haft":
        c = mix(hexc(pal["haft"]), hexc("000000"), rng.uniform(0, 0.25))
        if y % 6 == 0:
            c = hexc(pal["wrap"])                                         # grip bindings
        return c
    if mat == "blade":
        c = mix(hexc(pal["blade"]), hexc(pal["blade_edge"]), 0.6 if side in ("front", "back") and z_edge(x, w) else 0.1)
        return shade(c, rng.uniform(0.92, 1.06))
    if mat == "chatkcha":
        if side in ("top", "bottom"):
            # A three-pointed star of blades on the flat faces.
            cx, cy = (w - 1) / 2, (h - 1) / 2
            ang = math.atan2(y - cy, x - cx)
            r = math.hypot(x - cx, y - cy)
            if r > 0.9 + 1.2 * (0.5 + 0.5 * math.cos(3 * ang)):
                return None
            return hexc(pal["blade_edge"]) if r > 1.2 else hexc(pal["haft"])
        return hexc(pal["blade"])
    return NotImplemented


def z_edge(x, w):
    return x in (0, w - 1)
