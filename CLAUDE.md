# Warfront: Age of Banners

A NeoForge 1.21.1 modpack built around **Warfront**, a custom mod (in `mods/warfront/`) with playable races,
factions, armies in formation, mana as base power, and tower defense. The owner directs the design; Claude builds
it. The owner reads crypto/stock-style briefings: keep replies broad, short, decisive and in plain words.

## Branches: the default branch holds the latest work

`claude/vibrant-tesla-g5jis8` is the repo's default branch and always holds the latest pushed work; new sessions
start from it. The owner has given standing permission to push there.
- At the start of a session, if you were assigned a different branch, bring it up to date first:
  `git fetch origin claude/vibrant-tesla-g5jis8 && git merge --ff-only origin/claude/vibrant-tesla-g5jis8`
  (or a normal merge if your branch has its own commits). Never rewrite or force-push the default branch.
- After every push of finished work, and on "save progress", also push to the default branch:
  `git push origin HEAD:claude/vibrant-tesla-g5jis8` (a fast-forward; if it is rejected, merge the default branch
  in and push again).

## Start of every session: lead with a decision

The owner starts sessions with something like **"pick up where we left off"**, or just "continue". That (or any
opening without its own request) means: run this routine. Before anything else:

1. Read `tools/codex/needs.json` (open questions, most important first: `high`, then `medium`, then `low`) and
   skim the top of `tools/codex/progress.json` (what was done last).
2. In two or three lines, say where things stand: what was built last and whether CI was green.
3. Ask the most important open question with the AskUserQuestion tool: 2 to 4 concrete options, your
   recommendation first and marked "(Recommended)", each with a one-line consequence.
4. Act on the answer, then offer the next open question.

If the owner opens with their own request, do that instead.

**Build queue first.** `tools/codex/backlog.json` holds decided work in order. If it has `ready` items, step 3 is
replaced by: say what's queued in a line, then build the ready items top to bottom without re-asking (they are
decided), pushing and checking CI after each. Ask the owner only about `blocked` items, or when a spec is unclear.
When an item is done, remove it from the backlog and log it in `progress.json`.

## Brainstorm sessions

When the owner says the session is for **brainstorming** (or "just ideas"), nothing gets built: no code, no models,
no renders. Everything else is recorded as usual. Help widen and sharpen ideas (ask questions, offer options with a
recommendation, point out how an idea interacts with existing systems), and when the owner decides something,
record it right away: move it to `settled` in `needs.json`, add any new open questions, and queue the work in
`tools/codex/backlog.json` with a spec a build session can follow (`ready` if decided, `blocked` if it waits on
something). Ideas the owner floats but doesn't decide go in `tools/codex/ideas.json` in their words. Push after each
decision, and republish the two pages when they change.

## The two pages the owner uses

Both are generated from the repo and published as claude.ai artifacts. Republish to the **same URLs** after any
change they cover (a new session must pass `url` and read the artifact first, or it creates a duplicate).

| Page | URL | Build |
|---|---|---|
| Warfront Codex: progress, confirmed designs, models, systems, tests | https://claude.ai/artifact/8mbSQF2ZteBXw5VcU7Aef1 | `python3 mods/warfront/tools/units.py && python3 tools/codex/build_codex.py` → `build/codex/warfront-codex.html` |
| Design Needs: every race and role by name, open questions, settled decisions | https://claude.ai/artifact/WLp7LPGzYr36T3nPoU5fBR | `python3 tools/codex/build_needs.py` → `build/codex/design-needs.html` |

Data behind them, edit these instead of the HTML:
- `tools/codex/progress.json`: build log (newest first, current state only: fold superseded entries) and game
  test descriptions.
- `tools/codex/designs.json`: the Confirmed designs page (status `approved`, `review`, or `planned`).
- `tools/codex/needs.json`: open questions, settled decisions, per race/role design status.
- The Codex is page-based (`PAGES` in `build_codex.py`); keep it under 16 MB (repeated images are deduplicated).

When the owner decides something: move it from `questions` to `settled` in `needs.json`, record art direction in
`docs/art-reference/NOTES.md`, and add or update the design in `designs.json`.

## Standing decisions (owner direction)

- **Compatibility first.** Use vanilla systems other mods understand: attributes, mob effects, item/block tags,
  NeoForge capabilities. Integrations (Iron's Spells, Ars Nouveau...) are optional; Warfront must run without them.
  Player spell mana should come from Iron's Spells, not a third mana bar.
- **Mana is base power**, not a player stat: wells store it, pylons extend reach (16 blocks), towers and the
  Summoning Altar spend it. Manabloom grows shards; Mana Ore gives crystals (fuel and building material).
- **Imps are one demon species**, used only for the Impaler (spearman) and Firecaster (archer).
- **Workers look plain and humanoid** in every race: no horns, antennae, wings or crests. Orc workers are goblins (long ears, hooked nose).
- **No separate guard unit**: battle units take guard or patrol duty (sneak + right-click to cycle). The Guard role is retired but kept so old saves load.
- **The Hive is a sculk/Warden-like cave race**: dark teal chitin, glowing cyan veins; the Swarm is black with
  acid green. Stronger underground, weaker in sunlight; Swarm raids tunnel up; new Hive players start in a cave.
- **War beasts** are rare altar units, up to 10 per commander (`beastLimit`), meant to grow with base level once
  bases have levels. Hive: Deepmaw (built). Demon: Bone Stalker (reference only, not built).
- **Player flight per race**: humans have the Mana Glider (built); orc jetpack/tech, angel wings, elf flight magic not built;
  demons already fly. Player models come from the owner's references, Hive first.
- **Creatures** (later): a few fantastical creatures per race, mostly neutral.
- Champions, captains and most soldiers have **no design yet**; they wait on the owner's references.

## How to work in this repo

- **References:** every image the owner sends goes to `docs/art-reference/` (crop out book text and stat blocks)
  with design notes in `NOTES.md`, written from the picture only. Build models from them, render, and compare
  side by side before calling a model done.
- **Models** are generated: `mods/warfront/tools/units.py` (imps, demon player, Hive units via `hive_units.py`,
  worker gear via `worker_gear.py`, all in `mods/warfront/tools/`) writes `UnitGeometry.java` and textures. Blocks, items, recipes, loot and race
  skins come from `mods/warfront/tools/generate_assets.py`. Edit the generators, then run them; don't hand-edit their output.
  `generate_assets.py` also rewrites `mana_ore.png`, `deepslate_mana_ore.png` and `platform.nbt` with no real
  change: `git checkout` those three afterwards.
- **Rendering previews:** Chromium + Playwright (node, at `/opt/node22/lib/node_modules/playwright`) with three.js
  installed from npm into the scratchpad, using `tools/codex/mcmodel.js` (the game's own cube/UV rules).
- **Building:** the NeoForge maven is blocked from the cloud container, so Gradle can't build here. **CI is the
  compiler and test runner**: every push runs `./gradlew build` and `runGameTestServer`
  (`.github/workflows/build.yml`). Push, then check the run (GitHub MCP tools) and fix failures before moving on.
- **Game tests** (`WarfrontGameTests.java`) run in parallel 9×9 arenas: archers and mobs from a neighbouring arena
  can interfere, so assert on the thing under test (counters, state), not on a mob's health. Block entities join
  the mana network on their first tick: wait a couple of ticks before using it. Spawn test mobs at y=2 (as
  `archerShootsEnemy` does): at y=1 they sit in the arena floor and towers have no line of sight. When a test
  fails for an unclear reason, put the state in the failure message (as `towersFireOnlyWithMana` does) so the
  next CI run says what went wrong.
- **Git:** `git rm` stages immediately, so a deletion can ride along in an unrelated commit and break CI; stage
  with explicit paths. Commit messages describe the change in plain words.
- Work on the session's designated branch, push there, and keep the default branch up to date (see Branches). Don't open PRs unless asked.
