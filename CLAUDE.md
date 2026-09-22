# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**The Bedrock Add-On under `bedrock/` is the product.** The kids play it on
their tablets (`./ctl deploy tablet`); every new feature goes into
`bedrock/build.py`. Nothing is played from the Java side any more — see
"The Add-On" below and `bedrock/PORT-DURUMU.md` (the changelog and the
source of truth for what the pack does).

The repo also contains the original Minecraft **Fabric** mod (Minecraft 1.21.11,
Fabric Loader 0.18.2, Fabric API 0.139.4, Java 21). It is **legacy**: it is not
opened or deployed any more. Do not implement a request there unless the user
explicitly asks for Java. A feature that exists only in `src/` does not exist
for the kids.

## Build, test, deploy

```bash
python3 bedrock/build.py       # the Add-On -> bedrock/out/SuperTNT.mcaddon
bash bedrock/tools/test.sh     # build + check_pack + sim + mutations, one command
./ctl deploy tablet            # test.sh, then send to both tablets over wifi
./ctl deploy android           # the same, to the Android emulator (Minecraft must be installed there)
./ctl status                   # what is connected; AVD and simulator names
```

CI (`.github/workflows/build.yml`) runs `test.sh` on every push; the `.mcaddon`
is its artifact. There is no automated in-game test: `test.sh` proves the
script runs and the pack is consistent, not that an explosion looks right.

## The Add-On: `bedrock/build.py`

`bedrock/build.py` generates the Minecraft Bedrock Add-On: 70 TNTs, blocks,
items, morphs and a ~2500-line script (`SCRIPT_TEMPLATE`). It started as a port
of the Java mod and has since outgrown it; new features are Bedrock-only.
Everything under `bedrock/super_tnt_BP/` and `bedrock/super_tnt_RP/` is
**generated output** — edit `build.py`, not those folders.

Before sending anything to a tablet:

```bash
bash bedrock/tools/test.sh     # build + check_pack + sim + mutations, one command
```

`check_pack.py` enforces the tooltip contract statically; `tools/sim/run.mjs`
runs the script under a fake `@minecraft/server` (extend the fake when a new
API surface is used); the two mutation scripts prove those layers catch
something. A new feature ships with a sim scenario and a mutation. Script API is
`@minecraft/server` 1.14.0 — no custom enchantments, no `beforeEvents.entityHurt`.

See `bedrock/PORT-DURUMU.md` for the per-version design notes and known limits,
`bedrock/README.md` for the dev loop, and `docs/bedrock-port-fizibilite.md` for
what can and cannot be ported.

**Deploying to a tablet:** the Minecraft data folder on Android is read-only, so
each iteration is `build.py` → push to `/sdcard/Download/` → open the file.
Bump `VERSION` in `build.py` every time, otherwise Minecraft treats the import as
a duplicate instead of an update.

```bash
./ctl deploy tablet            # build + send to both tablets, over wifi
./ctl deploy tablet zeynep     # just one child's tablet
```

`./ctl` is the only entry point — build, deploy, wifi setup and status all live
there. Deployment is **wireless**: it discovers the tablets over Bonjour and
connects itself, and de-duplicates when a tablet is reachable on both USB and
wifi. A brand-new tablet needs the cable once (`./ctl wifi kur`). Child code
names live in `~/.config/tablet-adlari`, shared with the bilgebaykus project.
See `docs/tablet-kablosuz-deploy.md`, and `docs/cocuk-paketleri.md` for the
child-facing side.

## Tooltips are a contract

Every TNT's tooltip is what the kids read to learn what it does. A tooltip that
promises something the code does not do — or omits something the code does, such
as starting fires, dealing damage or permanently altering terrain — is treated as
a bug, not a wording nit. The 2026-07-19 audit found 54 such mismatches.

When changing behaviour, update the tooltip in **both** languages in the same commit.

## Legacy: the Java mod (`src/`, not developed)

Kept for reference and for the history of the ideas; nothing below is required
to work on the product. Do not add features here unless the user explicitly asks
for Java.

### Build commands (Java)

```bash
./gradlew build              # Build the mod JAR (output: build/libs/) — runs tests
./gradlew test               # Tests only (6 classes, 48 methods, pure Java)
./gradlew runClient          # Launch Minecraft dev client for manual testing
./gradlew genSources         # Generate Minecraft sources for IDE navigation
```

### Tests (Java)

Tests are **pure Java** — they do not boot Minecraft, so they run in seconds and
are safe to run on every change.

| Class | Covers |
|---|---|
| `ResourceFileTest` | Resource/registration consistency: lang, models, blockstates, recipes, advancements |
| `RegressionTest` | Bug classes found in the 2026-07-19 audit — see `docs/denetim-2026-07-19.md` |
| `TntBehaviorTest` | Pure-logic TNT behaviour |
| `CraftAxeVolumeTest` | Craft Axe fill volume maths |
| `ScaleConsistencyTest` | Shrink/Growth scale factors |
| `DrawingPayloadTest` | Drawing packet size limits |

`RegressionTest` locks in bugs that already happened once. Before deleting or
weakening one, read the comment above it — several of these bugs came back after
being fixed, which is why the test exists.

Manual in-game testing still matters: tests cannot catch "does this feel right".

### Architecture (Java)

- **Mod ID:** `supertntmod`
- **Package:** `com.supertntmod`
- **Entry points:** `SuperTntMod` (server) and `SuperTntModClient` (client), declared in `fabric.mod.json`

```
com.supertntmod/
├── SuperTntMod.java              # Server entry: registries, tick events, chat listener,
│                                 #   SERVER_STOPPING cleanup, player-disconnect cleanup
├── SuperTntModClient.java        # Client entry: entity renderers, screens
├── block/                        # 95 classes — CustomTntBlock base + all TNT blocks,
│                                 #   chests, doors, decorative blocks
├── entity/                       # 84 classes — ModEntities registry + one entity per TNT
├── item/                         # 37 classes — ModItems registry, tools, armor, spells
├── mixin/                        # 5 mixins — armor slot locking, TNT armor retaliation
└── client/                       # 16 classes — renderers, screens, drawing UI
```

#### Key architectural patterns

- **Multi-tick processing** — TNTs that modify thousands of blocks spread the work
  across ticks (`processing` / `idx` / `center` fields + a per-tick budget).
  **Any entity with a `processing` field MUST implement `readData`/`writeData`.**
  Without it, a save-and-quit mid-processing reloads with the fuse frozen at 0 and
  re-runs the whole destruction on every world load. `RegressionTest` enforces this.
- **Igniter exclusion** — `getOwner()` is **never set** on TNT entities (`setOwner`
  is only called on projectiles). To exclude whoever lit the TNT, store your own
  `igniterUuid` field and persist it — see `GizliTntEntity`. `RegressionTest`
  enforces that no TNT relies on `getOwner()`.
- **Temporary block placement** — anything that places blocks into the world
  (water, ice, wool) must schedule removal via
  `WaterTntEntity.scheduleRemoval(world, pos, lifetimeTicks, expectedBlock)`.
  That queue is swept on an interval, budgeted per sweep, and flushed on
  `SERVER_STOPPING` so nothing is left behind.
- **Position-keyed static maps** — must include the dimension
  (`record DimPos(RegistryKey<World>, BlockPos)`). A bare `BlockPos` key makes the
  same coordinates in the Nether and the Overworld collide. `RegressionTest`
  enforces this.
- **Static state lifecycle** — every static collection is concurrent, cleared on
  `SERVER_STOPPING`, and per-player entries cleared on disconnect. Both hooks live
  in `SuperTntMod`.
- **Explosion flags** — `createFire` must be `false` unless the tooltip says the
  TNT starts fires. `ExplosionSourceType.NONE` for TNTs whose tooltip promises
  decoration or loot rather than craters. `RegressionTest` enforces the fire rule.

#### Block/Entity categories

**Standard TNT** (block + entity pair, ignited like vanilla TNT) — the large
majority of the 71.

**Interactive** — Command TNT (configurable target/radius), TNT Door (owner-based),
Encrypted TNT Chest (chat password), Blocker Chest (owner set on **placement**),
Walking TNT (`PathAwareEntity` with AI), Fake TNT (cake disguise), Proximity Mine
(detects, then gives a real escape window before detonating).

**Non-TNT** — Lego Brick Block (16 colours), plus tools, armour and spells in `item/`.

### Adding a new TNT type

1. **Block class** in `block/` — extend `CustomTntBlock`
2. **Entity class** in `entity/` — extend `TntEntity`, override `tick()`
3. **Registries** — `ModBlocks.java` and `ModEntities.java`
4. **Client renderer** — `SuperTntModClient.java`
5. **Resources** — blockstate, block model, item model, `items/*.json`
   (**required in 1.21.11 or the item is invisible in-game**)
6. **Texture** — `assets/supertntmod/textures/`
7. **Recipe** — `data/supertntmod/recipe/` — `key` values are **plain strings**,
   not `{"item": "..."}` objects; `result` uses `id`, not `item`
8. **Localization** — both `lang/en_us.json` and `lang/tr_tr.json` (name + tooltip)
9. **Advancements** — add to **both** `collect_all.json` and `first_craft.json`
10. **Run `./gradlew test`** — steps 5, 7, 8 and 9 are all enforced by tests

#### Crafting pattern

Most TNT variants use 8 material + 1 TNT centre:

```
M M M
M T M  → 1 Special TNT
M M M
```

### Size of the Java mod

Current size (verify with a script before quoting these — they grow):

| | Count |
|---|---:|
| Registered blocks | 88 (71 TNT + 17 other) |
| Registered items | 43 |
| Entity classes | 84 |
| Block classes | 95 |
| Item classes | 37 |
| Mixins | 5 |
| Recipes | 124 |
| Advancements | 25 |
| Lang keys (per language) | 355 |
| Textures | 165 |
| Java source lines | ~18,000 |

## Commit rules

- Commit the current changes before moving to another task. No unfinished
  work left uncommitted.
- Split the changes into semantic groups, one commit each.

## Language

Source code comments and documentation (README.md, TODO.md, docs/) are in Turkish.
