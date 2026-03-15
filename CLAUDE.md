# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A Minecraft Fabric mod ("Super TNT Mod") that adds 7 custom TNT variants with unique explosion mechanics. Built for Minecraft 1.21.11 with Fabric Loader 0.18.2 and Java 21.

## Build Commands

```bash
./gradlew build              # Build the mod JAR (output: build/libs/)
./gradlew runClient          # Launch Minecraft dev client for testing
./gradlew genSources         # Generate Minecraft sources for IDE navigation
```

No unit tests exist — testing is done manually via `./gradlew runClient`.

## Architecture

- **Mod ID:** `supertntmod`
- **Package:** `com.supertntmod`
- **Entry points:** `SuperTntMod` (server) and `SuperTntModClient` (client), declared in `fabric.mod.json`

### Adding a new TNT type requires changes in all these places:

1. **Block class** in `block/` — extend `CustomTntBlock` (handles ignition via flint & steel, fire charge, redstone, chain reaction)
2. **Entity class** in `entity/` — extend `TntEntity`, override `explode()` with custom behavior
3. **Block registry** — register block + item in `block/ModBlocks.java`
4. **Entity registry** — register entity type in `entity/ModEntities.java`
5. **Client renderer** — register entity renderer in `SuperTntModClient.java`
6. **Resources** — add blockstate, block model, item model JSONs in `assets/supertntmod/`
7. **Recipe** — add shaped crafting JSON in `data/supertntmod/recipe/` (pattern: 8 material + 1 TNT center)
8. **Localization** — add entries in both `lang/en_us.json` and `lang/tr_tr.json`

### Crafting pattern (all variants follow this):
```
M M M
M T M  → 1 Special TNT
M M M
```

## Language

Source code comments and documentation (README.md, TODO.md) are in Turkish.
