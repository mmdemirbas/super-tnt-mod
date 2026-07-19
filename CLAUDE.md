# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A Minecraft **Fabric** mod ("Super TNT Mod") built for Minecraft 1.21.11 with
Fabric Loader 0.18.2, Fabric API 0.139.4, and Java 21.

There is also a **Bedrock Add-On** port under `bedrock/` for the kids' tablets —
see "Bedrock port" below.

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

## Build Commands

```bash
./gradlew build              # Build the mod JAR (output: build/libs/) — runs tests
./gradlew test               # Tests only (6 classes, 48 methods, pure Java)
./gradlew runClient          # Launch Minecraft dev client for manual testing
./gradlew genSources         # Generate Minecraft sources for IDE navigation
python3 bedrock/build.py     # Build the Bedrock Add-On -> bedrock/out/SuperTNT.mcaddon
```

## Tests

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

## Architecture

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

### Key architectural patterns

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

### Block/Entity categories

**Standard TNT** (block + entity pair, ignited like vanilla TNT) — the large
majority of the 71.

**Interactive** — Command TNT (configurable target/radius), TNT Door (owner-based),
Encrypted TNT Chest (chat password), Blocker Chest (owner set on **placement**),
Walking TNT (`PathAwareEntity` with AI), Fake TNT (cake disguise), Proximity Mine
(detects, then gives a real escape window before detonating).

**Non-TNT** — Lego Brick Block (16 colours), plus tools, armour and spells in `item/`.

## Adding a new TNT type

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

### Crafting pattern

Most TNT variants use 8 material + 1 TNT centre:

```
M M M
M T M  → 1 Special TNT
M M M
```

## Tooltips are a contract

Every TNT's tooltip is what the kids read to learn what it does. A tooltip that
promises something the code does not do — or omits something the code does, such
as starting fires, dealing damage or permanently altering terrain — is treated as
a bug, not a wording nit. The 2026-07-19 audit found 54 such mismatches.

When changing behaviour, update the tooltip in **both** languages in the same commit.

## Bedrock port

`bedrock/build.py` generates a Minecraft Bedrock Add-On from a subset of the Java
TNTs (12 of 71 so far). Everything under `bedrock/super_tnt_BP/` and
`bedrock/super_tnt_RP/` is **generated output** — edit `build.py`, not those folders.

Behaviour numbers and tooltip text are copied from the Java source so the two
versions stay consistent. See `bedrock/README.md` for known differences and
`docs/bedrock-port-fizibilite.md` for what can and cannot be ported.

**Deploying to a tablet:** the Minecraft data folder on Android is read-only, so
each iteration is `build.py` → `adb push` to `/sdcard/Download/` → open the file.
Bump `VERSION` in `build.py` every time, otherwise Minecraft treats the import as
a duplicate instead of an update. See `docs/cocuk-paketleri.md`.

## Commit Kuralları

- Başka bir göreve geçmeden önce mevcut değişiklikler commit edilir. Yarım kalan iş commit'siz bırakılmaz.
- Değişiklikler anlamsal gruplara ayrılarak commit edilir.

## Language

Source code comments and documentation (README.md, TODO.md, docs/) are in Turkish.
