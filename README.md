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

### 2. Raise an army
Use **Recruit Contracts** (paper + emerald + a role item, or paper + 2 War Marks + a role item):

| Role | Job on the battlefield |
|---|---|
| **Shieldbearer** | Front rank. Raises the shield as enemies close in or arrows fly, then lowers it to strike. |
| **Spearman** | Second rank. Extra reach lets them hit over the shield wall. |
| **Swordsman** | Heavy-hitting front-line fighter. |
| **Captain** | Carries your banner. Rallying cry gives nearby allies Strength and morale. Losing one shakes the army. |
| **Archer** | Back ranks. Fires in **synchronized volleys**, and arrows fly through your own troops. |
| **Healer** | Stays behind the lines and tends the most wounded ally nearby. |

Right-click your own soldier with armor or a sword or axe to re-equip them. Empty-handed, it shows
their health, morale and orders.

### 3. Command with the Commander's Baton
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

### 4. Factions and diplomacy
`/faction create <name>`, `invite <player>`, `join <name>`, `leave`, `info`, `list`,
`color <color>`, `war <name>`, `peace <name>`, and `ally <name>` (both leaders must agree).
Soldiers fight for their commander's faction. They attack factions you're at war with, protect
your allies, and never hurt friends.

Two hostile NPC powers roam the world: the **Marauder Horde** (orcs) and the **Black Legion**.
Their warbands **march on you in formation**, and their captains, healers and archers behave
just like yours.

### 5. Tower defense: hold your stronghold
* Place a **War Standard** to found a stronghold. At night, while you're nearby, **sieges** come:
  waves of enemies march from a random direction, and each wave is bigger and better equipped.
  Attackers that reach the standard hack at it, and if its integrity hits zero it falls.
* Build defenses around it:
  * **Arrow Tower**: shoots enemies in range
  * **Arcane Spire**: erupts evoker fangs beneath enemies
  * **Healing Shrine**: heals allies around it
* Repel a wave to earn **War Marks** (diamonds every 5th wave). Spend them on contracts and towers,
  or trade 4 for an emerald.
* Sound a **War Horn** near your standard to call the next wave early.

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
