# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A Minecraft Fabric mod ("Super TNT Mod") that adds 22 custom TNT blocks, 1 decorative block, and 1 throwable item — each with unique explosion or interaction mechanics. Built for Minecraft 1.21.11 with Fabric Loader 0.18.2, Fabric API 0.139.4, and Java 21.

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

### Package Structure

```
com.supertntmod/
├── SuperTntMod.java              # Server entry: registries, tick events, chat listener
├── SuperTntModClient.java        # Client entry: entity renderers
├── block/
│   ├── CustomTntBlock.java       # Abstract base for all TNT blocks (ignition logic)
│   ├── ModBlocks.java            # Block + block item registry
│   ├── LegoBrickBlock.java       # Non-TNT decorative block (16 color variants)
│   └── *TntBlock.java, TntDoorBlock.java, EncryptedTntChestBlock.java, FakeTntBlock.java
├── entity/
│   ├── ModEntities.java          # Entity type registry
│   ├── WalkingTntEntity.java     # Special: PathAwareEntity with AI goals
│   ├── TntFrisbeeEntity.java     # Special: ThrownEntity
│   └── *TntEntity.java           # Standard TNT entities extending TntEntity
├── item/
│   ├── ModItems.java             # Item registry (TNT Frisbee)
│   ├── TntFrisbeeItem.java       # Throwable item with tooltip
│   └── TooltipBlockItem.java     # Block item wrapper that adds tooltips
└── client/
    ├── WalkingTntEntityRenderer.java
    └── TntFrisbeeEntityRenderer.java
```

### Block/Entity Categories

**Standard TNT blocks** (block + entity pair, ignited like vanilla TNT):
Diamond, Gold, Bedrock, Emerald, Lightning, Nuclear, Freeze, Wood, Mob Freeze, Water, Rainbow, Lego, Makarna, Seker, Shrink, Growth, Cleanse

**Interactive TNT blocks** (non-standard ignition or behavior):
- **Command TNT** — configurable target block and radius via item interaction / sneak
- **TNT Door** — owner-based door, explodes on unauthorized second attempt
- **Encrypted TNT Chest** — chat-based password system, damages unauthorized users
- **Walking TNT** — PathAwareEntity with AI, tracks players making eye contact
- **Fake TNT** — cake appearance, explodes when broken (not ignited)

**Non-TNT:**
- **Lego Brick Block** — decorative, 16 color variants via block state, spawned only by Lego TNT
- **TNT Frisbee** — throwable item, cross-shaped ground destruction, returns to owner

### Key Architectural Patterns

- **Multi-tick processing** — Bedrock, Rainbow, Command TNT process block modifications over multiple ticks to avoid lag spikes
- **Owner/UUID tracking** — Walking TNT, TNT Door, Encrypted Chest track `ownerUuid` for access control
- **Server tick events** — Wood TNT uses `ServerTickEvents.END_SERVER_TICK` for delayed tree regrowth
- **Chat message listener** — Encrypted Chest uses `ServerMessageEvents.ALLOW_CHAT_MESSAGE` for password input
- **Attribute modifiers** — Shrink/Growth TNT use `EntityAttributeModifier` on `SCALE` attribute

### Adding a New TNT Type

Standard TNT requires changes in all these places:

1. **Block class** in `block/` — extend `CustomTntBlock`
2. **Entity class** in `entity/` — extend `TntEntity`, override `explode()`
3. **Block registry** — register block + item in `ModBlocks.java`
4. **Entity registry** — register entity type in `ModEntities.java`
5. **Client renderer** — register entity renderer in `SuperTntModClient.java`
6. **Resources** — blockstate JSON, block model JSON, item model JSON in `assets/supertntmod/`
7. **Texture** — block/item textures in `assets/supertntmod/textures/`
8. **Recipe** — shaped crafting JSON in `data/supertntmod/recipe/`
9. **Localization** — entries in both `lang/en_us.json` and `lang/tr_tr.json` (block name + tooltip)
10. **Advancement** (optional) — JSON in `data/supertntmod/advancement/`

### Crafting Pattern

Most TNT variants follow the 8-material + 1-TNT center pattern:
```
M M M
M T M  → 1 Special TNT
M M M
```

Exceptions: TNT Frisbee (feathers + TNT + iron ingot), TNT Door, Encrypted Chest have unique recipes.

### Resources

- **Advancements:** 6 advancements in `data/supertntmod/advancement/`
- **Recipes:** 24 shaped crafting recipes in `data/supertntmod/recipe/`
- **Languages:** `en_us.json` and `tr_tr.json` (~68 entries each: block names, tooltips, advancements)

## Commit Kuralları

- Başka bir göreve geçmeden önce mevcut değişiklikler commit edilir. Yarım kalan iş commit'siz bırakılmaz.
- Değişiklikler anlamsal gruplara ayrılarak commit edilir.

## Branch Topolojisi

İki branch var ve aralarında **strict ancestor** ilişkisi korunur:

- **`main`** — sadece "merge edilmesine karar verilmiş" iş. Tek doğrultulu lineer geçmiş.
- **`zeynep`** — develop branch. Tüm geliştirme burada yapılır. Her zaman `main` üzerinde rebase edilmiş durumda kalır (`main` her zaman `zeynep`'in atasıdır).

**Kurallar:**

- `zeynep` üzerindeki zeynep-özel commit'ler tip'te tutulur, **ayrı ayrı** (her mantıksal özellik için bir commit). Tek squash commit kullanma — gelecekte main'e seçici cherry-pick gerektiğinde history divergence olur.
- Yeni paylaşılan iş geldiğinde:
  1. Önce `main`'e (ya direkt ya da ayrı bir feature branch'ten merge ile) eklenir.
  2. `zeynep` `main` üstüne rebase edilir (`git rebase main`). Zeynep-özel commit'ler tip'te kalır.
- Yeni zeynep-özel iş geldiğinde:
  1. `zeynep` üstünde commit edilir (tip'e eklenir).
  2. `main`'e dokunma.
- Her iki branch'e gidecek bir geliştirme `main`'e gider; sonra `zeynep` rebase edilir. Çift commit asla yapılmaz.

**Doğrulama:**

```bash
git merge-base --is-ancestor main zeynep && echo "OK"   # main, zeynep'in atası mı?
git log main..zeynep --oneline                          # zeynep-özel commit'ler
git diff main..zeynep --stat                            # zeynep-özel içerik özeti
```

## Language

Source code comments and documentation (README.md, TODO.md) are in Turkish.
