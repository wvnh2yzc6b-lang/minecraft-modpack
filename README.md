# Warfront: Age of Banners

A **magic and adventure modpack for NeoForge 1.21.1**, built around **Warfront**, a custom mod
that turns Minecraft into a fantasy world of warring states. You get playable races, player
factions with diplomacy, armies that fight in real formations, and tower defense against marauder
sieges.

## Repository layout

| Path | What it is |
|---|---|
| `mods/warfront/` | Source of the custom **Warfront** mod (Java 21, NeoForge ModDevGradle) |
| `modlist.txt` | Curated list of third-party mods (Modrinth slugs) |
| `pack/` | [packwiz](https://packwiz.infra.link/) pack definition (MC + NeoForge versions) |
| `scripts/build-pack.sh` | Resolves the mod list, bundles Warfront, exports a `.mrpack` |
| `.github/workflows/build.yml` | CI: compiles the mod and builds the modpack on every push |

## Installing the pack

1. Open the **Actions** tab, choose the latest green run of *Build mod and modpack*, and download
   the `warfront-modpack` artifact.
2. Import `Warfront-Age-of-Banners.mrpack` into **Prism Launcher**, the **Modrinth App** or
   **ATLauncher** (Add Instance → Import).
3. Give the instance at least **6 GB of RAM**.

To build it yourself, you need Java 21 and Go:

```bash
cd mods/warfront && ./gradlew build && cd ../..
go install github.com/packwiz/packwiz@latest
./scripts/build-pack.sh          # writes build/Warfront-Age-of-Banners.mrpack
```

## The Warfront mod: how to play

### 1. Choose your race
When you first join, click a race in chat (or run `/warfront race <race>`). The choice is
permanent, and it shapes both you and every soldier you recruit.

| Race | Traits |
|---|---|
| **Human** | Balanced. Morale recovers fastest. Human healers heal more. |
| **Elf** | +10% speed, slightly taller, deadly accurate archers. Less health. |
| **Dwarf** | +4 health, +2 armor, short and slower. **Dwarven soldiers never rout.** |
| **Orc** | +1.5 damage, +2 health, towering. Morale breaks easily. |
| **Demon** | Immune to fire and lava. Demon soldiers set their foes ablaze. Recruits cost 15% more mana. |
| **Angel** | No fall damage, slow regeneration, +50% damage against undead and demons. Never rout. Recruits cost 20% more mana. |
| **Hive** | A sculk-dark cave race. Small, fast swarmers with less health. Never rout, hit harder in packs, cost **40% less** mana, and grow stronger underground (below Y=40) but weaker in sunlight. New Hive players start in a cave. |

### 2. Mana: power for your base
Mana is base power, not a personal bar. You grow and mine it, store it in wells, and spend it on
towers and summoning.
* **Manabloom** is a glowing crop that grows **Mana Shards** (+10 mana each). Its seeds drop from
  grass (about 5%), or you can craft them from a shard and wheat seeds.
* **Mana Ore** generates underground, in stone and deepslate up to Y=80. It needs an iron pickaxe and
  drops **Mana Crystals** (+50 mana each), which are also what wells, pylons and altars are built from.
* A **Mana Well** stores up to 2,000 mana. Right-click it with shards or crystals (sneak for the
  whole stack), or feed it with a hopper.
* Wells and **Mana Pylons** each reach 16 blocks and chain together. Anything in reach of the chain
  draws from every well on it.
* Towers spend mana on every shot or heal, and sputter without it.

### 3. Raise an army at a Summoning Altar
Lay a 3×3 floor of any brick or stone-brick block, put the **Summoning Altar** in the middle, and a
**Mana Brazier** two blocks out on each diagonal corner. Keep a filled well in reach, then
right-click the altar to choose a unit. Its cost comes from the wells, scaled by your race:

| Role | Mana | Job on the battlefield |
|---|---|---|
| **Shieldbearer** | 15 | Front rank. Raises the shield as enemies close in or arrows fly, then lowers it to strike. |
| **Spearman** | 15 | Second rank. Extra reach lets them hit over the shield wall. |
| **Swordsman** | 20 | Heavy-hitting front-line fighter. |
| **Captain** | 50 | Carries your banner. Rallying cry gives nearby allies Strength and morale. Losing one shakes the army. |
| **Archer** | 20 | Back ranks. Fires in **synchronized volleys**, and arrows fly through your own troops. |
| **Healer** | 30 | Stays behind the lines and tends the most wounded ally nearby. |
| **Champion** | 60 | The race's elite, with a signature weapon and ability. |
| **War Beast** | 150 | A huge beast, for races that have one (the Hive's Deepmaw so far). Up to 10 per commander. |

The starter kit includes an altar, four braziers, a well and 24 shards.

Right-click your own soldier with armor or a sword or axe to re-equip them. Empty-handed, it shows
their health, morale and orders.

### 4. Workers and guards
Three roles keep your base running instead of marching with the army. They take up a **post** where
you summon them and ignore the Commander's Baton. **Sneak + right-click** one with an empty hand to
have it follow you, and again to post it where it stands.

| Role | Mana | What it does |
|---|---|---|
| **Farmer** | 10 | Harvests ripe crops (and melons and pumpkins) within 8 blocks of its post, replants from the seeds it carries, tills dirt and grass near water, and carries the harvest to the nearest chest or barrel. Starts with 8 wheat seeds. |
| **Builder** | 15 | Surveys the buildings around its post (21×21 blocks, 3 down and 12 up) and puts back anything enemies break or explosions destroy, lowest blocks first. Takes blocks from a nearby chest. What you and your allies build or break yourselves updates its plan instead. |
| **Guard** | 20 | Holds its post, walks a short patrol around it, and fights anything hostile within 14 blocks. When it spots an enemy it rings an alarm that sends nearby allied troops (16 blocks) into the fight. |

Hand a worker seeds or blocks by right-clicking it with them. Farmers and builders never fight: they
run from enemies. Each race has its own workers (Farmhand, Grovetender, Stonemason, Gatekeeper,
Sentry Drone...), in plain work clothes in their race's colors.

### 5. Command with the Commander's Baton
* **Right-click** to cycle orders. **Follow** marches in formation behind you. **Hold the line**
  forms up where you stand, facing where you look, and holds. **Charge!** breaks ranks and attacks
  the spot you're looking at.
* **Sneak + right-click** to cycle formations:
  * **Battle Line**: ranks of eight, melee in front, archers behind
  * **Shield Wall**: tight and wide. Shieldbearers in front, spears behind, no chasing.
  * **Wedge**: an arrowhead to break lines
  * **Square**: melee ring facing out, archers and healers protected inside
  * **Skirmish**: loose spacing against arrows and area attacks
* Soldiers keep discipline. When holding, they only engage enemies close to their post.
* **Morale:** taking damage, or seeing comrades (especially captains) fall, lowers morale. Badly
  hurt soldiers with broken morale **rout** and flee, then regroup. Captains nearby speed recovery.
* `/army` shows your army's composition, average health and morale.

### 6. Factions and diplomacy
`/faction create <name>`, `invite <player>`, `join <name>`, `leave`, `info`, `list`,
`color <color>`, `war <name>`, `peace <name>`, and `ally <name>` (both leaders must agree).
Soldiers fight for their commander's faction. They attack factions you're at war with, protect
your allies, and never hurt friends.

Seven hostile NPC powers roam the world, and each favors its own homeland:

| Faction | Race | Homeland |
|---|---|---|
| Marauder Horde | Orc | Plains, savanna |
| Black Legion | Human | Plains, taiga |
| Burning Horde | Demon | Badlands, desert, **the Nether** |
| The Swarm | Hive | Jungle, swamp, caves (comes in big numbers) |
| Silverwood Reavers | Elf | Forests |
| Ironbeard Clan | Dwarf | Mountains, snow |
| Fallen Host | Angel | Mountain peaks, the End |

Their warbands **march on you in formation**, and their captains, healers and archers behave just
like yours. They are also at war with each other. Raiders who can't reach you **hack through
walls, doors and fences**. Harder blocks take longer, and obsidian and anything harder than
`maxBreakHardness` (default 10) stops them. This needs the `mobGriefing` game rule.

### 7. Tower defense: hold your stronghold
* Place a **War Standard** to found a stronghold. At night, while you're nearby, **sieges** come:
  waves of enemies march from a random direction, and each wave is bigger and better equipped.
  Attackers that reach the standard hack at it, and if its integrity hits zero it falls.
* Build defenses around it (each draws mana from your wells):
  * **Arrow Tower**: shoots enemies in range
  * **Arcane Spire**: erupts evoker fangs beneath enemies
  * **Healing Shrine**: heals allies around it
* Repel a wave to earn **War Marks** (diamonds every 5th wave). Spend them on towers and Mana Shards,
  or trade 4 for an emerald.
* Sound a **War Horn** near your standard to begin a **wave campaign**. These are endless,
  escalating waves with a **boss bar** that shows the wave number, enemies remaining and the
  standard's integrity, plus a 30-second break between waves.
  * From wave 4, attackers come from two directions. From wave 8, they come from three.
  * Every 5th wave is led by a **Warlord**: a towering boss with triple health that hits the
    standard three times as hard.
  * Sneak and sound the horn to stand down after the current wave.

All numbers (army cap, siege frequency, warband chance, tower range, friendly fire) are in
`config/warfront-common.toml`.

## Bundled mods

Performance (Sodium, Iris, Lithium, FerriteCore, ModernFix, EntityCulling), magic (Iron's Spells
'n Spellbooks, Ars Nouveau, Occultism, Apothic Enchanting, Artifacts), combat (Better Combat,
Simply Swords, Epic Knights), adventure (When Dungeons Arise, the YUNG's suite, Towns and Towers,
Repurposed Structures, Explorify, Dungeons and Taverns, The Aether, The Twilight Forest), world
generation (Terralith, Tectonic), and QoL (JEI, Jade, Xaero's maps, Waystones, Sophisticated
Backpacks, Corpse). See `modlist.txt` for the exact list. Each CI run's `pack-report.md` lists
anything that failed to resolve.
