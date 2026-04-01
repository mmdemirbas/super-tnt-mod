# Spec: Scale Ball Items, Block Placement Fix, and Translation Verification

## Context
Steps 1–8 from the previous session are complete and the build passes. This spec covers
the three new items added to TASK.md.

## Completed (previous session)
- Step 1: TunneledBlock rendering color fixed (no red tint)
- Step 2: Player can enter drilled holes
- Step 3: Drill size scales with player size
- Step 4: More shrink levels supported (SCALE minimum = 0.0001)
- Step 5: All 22 block item definitions reference unique item models
- Step 6: Shrink TNT texture shows minus sign
- Step 7: Among Us Report item (instakill on right-click)
- Step 8: Scaled block placement via BlockItemMixin

## New Work

### Step 9: Shrink Ball and Grow Ball items
**Requirement (TR):** İki yeni nesne tanımla. Birisi kırmızı, diğer yeşil top olsun. Kırmızı top
küçültme, yeşil top büyütme topu olacak. Tıpkı küçültme ve büyütme TNT'si gibi bunlar da bu
nesneleri eline alıp da atan kişiyi büyütecek veya küçültecek. TNT'lerden farklı olarak etraftaki
diğer canlılara etki etmeyecekler.

**Behavior:**
- Red Ball (shrink): throwable; on landing applies 0.3× scale to the **thrower only** (same
  multiplier as Shrink TNT). Gives tunneling tool if threshold reached.
- Green Ball (grow): throwable; on landing applies 3.0× scale to the **thrower only** (same
  multiplier as Growth TNT).
- Neither affects other nearby entities.
- Consumed on use (not returned). Works in creative mode without consuming.

**Implementation:**
- `ShrinkBallEntity` and `GrowBallEntity` — both extend `ThrownEntity`. In `onBlockHit()`,
  retrieve `getOwner()` as `LivingEntity` and apply scale modifier. No effect on other entities.
- `ShrinkBallItem` and `GrowBallItem` — both extend `Item`, override `use()` to throw entity.
  Pattern: same as `TntFrisbeeItem`.
- Register entities in `ModEntities` using `regThrown`.
- Register items in `ModItems`.
- Add both to creative tab in `SuperTntMod`.
- Renderer: reuse `PortalProjectileEntityRenderer` pattern. Red ball → `RED_CONCRETE`,
  Green ball → `GREEN_CONCRETE` at 0.4× scale.
- Separate renderer classes: `ShrinkBallEntityRenderer` and `GrowBallEntityRenderer`.
- Resources: item definition JSON, item model JSON, 16×16 texture PNG, crafting recipe.
  Shrink ball recipe: 8 red dye + 1 TNT center. Grow ball recipe: 8 lime dye + 1 TNT center.
- Lang entries in both `en_us.json` and `tr_tr.json`.

**Acceptance criteria:**
- Right-clicking while holding shrink ball throws it; it shrinks only the thrower (0.3× current).
- Right-clicking while holding grow ball throws it; it grows only the thrower (3.0× current).
- Other nearby entities are unaffected.
- Items appear in creative tab with correct icons.
- Tooltips in EN and TR.
- Crafting recipe works.

### Step 10: Fix block placement interference for mod blocks
**Requirement (TR):** Küçültme TNT'si ile küçüldüğümüzde artık bazı nesneleri koyamaz hale
geliyoruz. Örneğin büyütme TNT'sini koymaya çalıştığımızda anlık olarak koyuyor gibi görünüyor,
ancak hemen görünmez bir blok oluyor. Kırınca da taşa dönüşerek kırılıyor gibi bir his veriyor.
Yani küçükken tekrar büyümek için büyütme TNT'si kullanamıyoruz. Diğer özel nesnelerimizde de
aynı sorun var. Bunu düzelt.

**Root cause:** `BlockItemMixin.supertntmod$onBlockPlaced()` intercepts ALL block placements for
shrunk players and converts them to a tiny TunneledBlock. This includes all `supertntmod` blocks
(Growth TNT, Shrink TNT, etc.), making them non-functional since TunneledBlock has no TNT behavior.

**Fix:** In `BlockItemMixin`, add an early return if the placed block's namespace is `supertntmod`.
This ensures mod blocks always place at full size regardless of player scale. Only vanilla/external
blocks get the scaled-placement treatment.

**Acceptance criteria:**
- A shrunk player can place Growth TNT, Shrink TNT, and all other mod blocks normally.
- Placed mod blocks function correctly (TNTs explode, doors work, etc.).
- Vanilla blocks placed by a shrunk player still receive the proportional scaling.

### Step 11: Verify all names and translations
**Requirement (TR):** Özel nesnelerimizin hepsinin isimlerinin ve çevirilerinin eksiksiz ve doğru
olduğundan emin ol. Bazıları eksik gibi gördüm.

**Scope:**
- After Step 9, add EN/TR entries for both ball items.
- Check both lang files for any missing block, item, or tooltip key that appears in the codebase
  but is absent from the JSON.

**Acceptance criteria:**
- Every registered block and item has a display name and tooltip in both `en_us.json` and
  `tr_tr.json`.
- No key referenced in Java code (via `Text.translatable(...)`) is absent from the lang files.

## Technical Constraints
- Minecraft 1.21.11, Fabric Loader 0.18.2, Java 21
- No new dependencies
- Source code comments and user-facing strings: Turkish pattern (comments in TR, en_us + tr_tr JSON)
- Manual testing via `./gradlew runClient`
- Must not break existing TNT or tunneling functionality
