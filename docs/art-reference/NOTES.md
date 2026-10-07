# Unit art direction

Reference images supplied by the pack owner, with the design notes taken from each.
These drive the soldier models (3D parts), skins and unit sizes.

## General direction
- Models should be heavily textured and creative, with extra 3D parts: tails, wings and extremities
  (claws, talons, horns, ears) matter.
- Units differ in size by what they are: low ranks and small creatures are smaller, elites and
  brutes are larger.
- Every race needs full rosters of roles, not only soldiers: civilians like builders and farmers,
  plus guards and other ranks.

## Demon: Imp (basic low-rank soldier) - `demon-imp.png`
- **Size:** small and lean, well below player height (about 0.7×). A wiry, gaunt build with thin limbs.
- **Skin:** deep crimson-red with darker spotted/mottled patches across the chest, shoulders and thighs.
  Lighter highlights on the muscles.
- **Head:** a crown of small swept-back spiky horns, large pointed ears, glowing yellow eyes, a heavy
  brow, a snarling mouth and a thin goatee.
- **Wings:** large bat-like membrane wings, much taller than the body. They have bony finger struts,
  a hooked claw at the wing tip, and translucent red membranes with tattered, ragged trailing edges.
- **Tail:** long, thick and segmented like a scorpion's, tan or bone colored, curling up behind the
  body. It ends in a small barb with a tuft.
- **Hands:** long fingers with long black talons.
- **Legs:** digitigrade (bent backwards, beast-like), with black clawed feet and dark shins fading
  from the red skin.

## Imp role variants (owner direction)
- Imps are one species within the demon faction, not the whole faction. They only fill two roles:
  the Impaler (spearman) and the Firecaster (archer). Other demon ranks and workers are full-sized.
- Each role of a unit should look a little different, and this applies to every race.
- Impaler: rugged and sharper, with some armor. Firecaster: a magical look. Imps are low tier, so
  keep it modest.

## Player characters and flight (owner direction)
- Most races get their own player model, from references the owner will supply. Hive comes first.
- Most races should have some form of flight:
  - Orc: a jetpack or other tech. No wings.
  - Angel: angel wings.
  - Elf: flight magic.
  - Human: undecided.
  - Demon: done (wings, elytra-style flight).
- Not started. Wait for the references.

## Hive (owner direction)
- The Hive should be more Warden and sculk-like: an underground race that builds its keeps in caves.

## Workers (owner direction)
- Builders, farmers and guards should not be extravagant. They look like plain humanoid working
  folk: simple hats, smocks, aprons and helms in the race's colors, with no horns, antennae, wings
  or crests.
- Armor and clothes need real texture (grain, stitching, rivets, wear), not flat pixels.

## Demon player character (owner reference painting, not stored as a file)
- **Size:** same as a normal player.
- **Head:** a black, armored, helm-like horned visage. Two long horns sweep up and curve inward like a
  lyre, with a crown of smaller spikes and thorns around them. The face is a dark beaked mask.
- **Body:** pale grey, heavily muscled bare torso with defined abs and pecs.
- **Arms:** dark, thorny bramble-like growths wrap the forearms and shoulders. The hands are long and
  black with hooked talons.
- **Wings:** huge and dark, wider than the body, with ragged black edges. The inner membranes glow
  ember red. Each wing has a hooked claw at the top joint.
- **Lower body:** a long tattered black robe or loincloth from the waist to the feet, with a flowing
  black cloak behind, frayed into strips.
- **Feet:** black clawed feet.

## Working agreement
- Whenever a visual change is made, render the model and show it right away, without being asked.

## Demon player head (revision)
- Model the head on Oryx, the Taken King from Destiny: a bone-chitin skull, a tall crescent crest
  sweeping up and back, crown blades along the sides, a jutting brow plate, three deep-set glowing
  eyes, flared cheek plates and a mandibled jaw. The eyes stay demon red to match the race.

## Demon player wings: folding

- Each wing hinges at an elbow (`wing_r` → `wing_r_outer`). The inner panel carries the deep membrane;
  the outer panel carries the pointed tip membrane, two finger struts and the tip claw.
- **Tucked** (default, on the ground): the wrist folds up over the shoulder and the membrane hangs flat
  down the back like a cape; the outer panel folds back onto the inner. A slight idle sway.
- **Half-open** when jumping or falling (quick balancing flaps); **full spread** in flight, with the
  elbow straightened and the tip flexing behind each beat. The layer eases between states over about
  half a second. See `demon-player-wing-fold.png`.


## Working agreement: animation

Animations should look and feel fluid and real, not like a metronome. Rules used so far (see
`WingAnimator`):

- Drive cycles from an accumulated phase, never `sin(age * speed)` with a changing speed (that jumps).
- Ease every blend exponentially by elapsed time, so motion is the same at any frame rate.
- Asymmetric strokes: fast power stroke, slow recovery. Outer joints lag inner ones (follow-through),
  and secondary parts (cloak, tail) ride the main motion a beat late.
- Effort follows context: hard beats on takeoff and climbs, a swept-back glide in dives.
- Multi-joint transitions are staggered: shoulder then elbow when opening, the reverse when closing.
- Technique follows procedural creature animation used by mods such as Citadel / Ice and Fire
  (phase-offset chain waves), implemented in-house to avoid a dependency.
- Every animation change ships with a short preview GIF (e.g. `demon-flight.gif`).

## Working agreement: the Codex

The Warfront Codex page (https://claude.ai/artifact/8mbSQF2ZteBXw5VcU7Aef1) shows everything built so
far and is updated as work lands. To update it: add or change an entry in `tools/codex/progress.json`,
run `python3 mods/warfront/tools/units.py` then `python3 tools/codex/build_codex.py`, and republish
`build/codex/warfront-codex.html` to the same link.
