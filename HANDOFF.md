# Handoff

## Status
COMPLETE

## Round
4

## From → To
coder → done

---

## Message
All items from TASK.md implemented and committed.

**Steps 1–8** (previous session) — confirmed complete, build passing.

**Step 9 — Shrink Ball and Grow Ball:**
- `ShrinkBallEntity` / `GrowBallEntity` — ThrownEntity subclasses; `onBlockHit()` applies
  scale modifier to owner only using the shared `ShrinkTntEntity.SCALE_MODIFIER_ID`.
- `ShrinkBallItem` / `GrowBallItem` — throwable item pattern matching TntFrisbeeItem.
- Registered in ModEntities, ModItems, SuperTntMod creative tab, SuperTntModClient renderers.
- Full resource set: item definitions, models, 16×16 PNG textures, recipes (8 dye + 1 TNT = 4 balls).
- EN/TR lang entries added.

**Step 10 — Block placement fix:**
- `BlockItemMixin.supertntmod$onBlockPlaced()` now skips blocks in the `supertntmod` namespace.
- Shrunk players can place Growth TNT, Shrink TNT, and all other mod blocks at full size again.

**Step 11 — Translations:**
- All existing items had complete EN/TR entries.
- Shrink Ball and Grow Ball entries added in this session.

## Commits
- `c4852b8` fix: skip scaled-placement conversion for supertntmod blocks
- `abdb3d9` feat: add Shrink Ball and Grow Ball self-only scale items
