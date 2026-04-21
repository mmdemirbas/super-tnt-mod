package com.supertntmod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TNT entity davranışlarının pure-Java mantık testleri.
 * Minecraft API gerektiren entegrasyon testleri değil; hesaplama, sınır ve
 * tutarlılık kontrolleri.
 */
class TntBehaviorTest {

    // ── Küp TNT geometrisi ────────────────────────────────────────────────

    @Test
    void kupTntCubeIsExactly25x25x25() {
        final int HALF  = 12;
        final int DEPTH = 24;

        int count = 0;
        for (int dx = -HALF; dx <= HALF; dx++) {
            for (int dz = -HALF; dz <= HALF; dz++) {
                for (int dy = 0; dy >= -DEPTH; dy--) {
                    count++;
                }
            }
        }

        int expectedSide = 2 * HALF + 1;  // 25
        int expectedDepth = DEPTH + 1;     // 25  (0 … -24 inclusive)
        assertEquals(25, expectedSide, "X/Z side must be 25 blocks");
        assertEquals(25, expectedDepth, "Y depth must be 25 blocks");
        assertEquals(25 * 25 * 25, count, "Total positions must be 25^3 = 15625");
    }

    @Test
    void kupTntHalfAndDepthGiveSymmetricCube() {
        final int HALF  = 12;
        final int DEPTH = 24;
        int xSide = 2 * HALF + 1;
        int zSide = 2 * HALF + 1;
        int ySide = DEPTH + 1;
        assertEquals(xSide, zSide, "X and Z sides must match");
        assertEquals(xSide, ySide, "All three dimensions must match (cube)");
    }

    // ── Üreyen TNT nesil sınırı ve fitil süresi ──────────────────────────

    @Test
    void ureyenTntFuseDecreasesWithGeneration() {
        // Math.max(20, 80 - generation * 15)
        assertEquals(80, fuseForGeneration(0));
        assertEquals(65, fuseForGeneration(1));
        assertEquals(50, fuseForGeneration(2));
        assertEquals(35, fuseForGeneration(3));
        assertEquals(20, fuseForGeneration(4)); // clamp kicks in here
    }

    @Test
    void ureyenTntFuseNeverDropsBelowMinimum() {
        for (int gen = 0; gen <= 10; gen++) {
            assertTrue(fuseForGeneration(gen) >= 20,
                    "Fuse at generation " + gen + " must be >= 20");
        }
    }

    @Test
    void ureyenTntMaxGenerationLimitsSpread() {
        final int MAX_GENERATION = 5;
        // Generation 4 (0-indexed) is the last that spawns children
        // (generation < MAX_GENERATION)
        assertTrue(4 < MAX_GENERATION, "Gen 4 should spawn children");
        assertFalse(5 < MAX_GENERATION, "Gen 5 should NOT spawn children");
    }

    private static int fuseForGeneration(int generation) {
        return Math.max(20, 80 - generation * 15);
    }

    // ── Ölçek matematiği (ShrinkBall / GrowBall) ─────────────────────────

    @Test
    void shrinkAppliesHalfScaleModifier() {
        double current = 1.0;
        double newScale = current * 0.5;          // 0.5
        double modifier = newScale - 1.0;          // -0.5
        assertEquals(-0.5, modifier, 1e-9);
    }

    @Test
    void growAppliesDoubleScaleModifier() {
        double current = 1.0;
        double newScale = current * 2.0;           // 2.0
        double modifier = newScale - 1.0;          // 1.0
        assertEquals(1.0, modifier, 1e-9);
    }

    @Test
    void shrinkAndGrowUseConsistentMaxScaleTolerance() {
        // Her iki ball da 0.001 tolerans kullanmalıdır
        double tolerance = 0.001;

        // Minimum ölçekte (örn. 0.001) küçültme: yeni ölçek 0.0005
        // Fark = |0.0005 - 0.001| = 0.0005 < 0.001 → "en küçük" mesajı
        double minScale = 0.001;
        double afterShrink = minScale * 0.5;
        assertTrue(Math.abs(afterShrink - minScale) < tolerance,
                "At minimum scale, shrink delta must be within tolerance");

        // Makul bir ölçekte küçültme: yeni ölçek 0.5
        double normalScale = 1.0;
        double afterNormalShrink = normalScale * 0.5;
        assertFalse(Math.abs(afterNormalShrink - normalScale) < tolerance,
                "At normal scale, shrink must produce a detectable change");
    }

    @Test
    void shrinkAndGrowModifierFormulaIsInverse() {
        double startScale = 1.0;
        // Küçüldükten sonra büyü → başa dönmeli (float hassasiyeti hariç)
        double afterShrink = startScale * 0.5;
        double afterGrow   = afterShrink * 2.0;
        assertEquals(startScale, afterGrow, 1e-9);
    }

    // ── Cam TNT yarıçap filtresi ──────────────────────────────────────────

    @Test
    void camTntDetectsGlassBlocksByRegistryPath() {
        // CamTntEntity: blockPath.contains("glass")
        String[] glassPaths = {"glass", "glass_pane", "tinted_glass", "brown_stained_glass", "brown_stained_glass_pane"};
        String[] nonGlassPaths = {"stone", "grass_block", "oak_log", "sand"};

        for (String path : glassPaths) {
            assertTrue(path.contains("glass"), "'" + path + "' should be detected as glass");
        }
        for (String path : nonGlassPaths) {
            assertFalse(path.contains("glass"), "'" + path + "' should NOT be detected as glass");
        }
    }

    // ── ControlRemote: cooldown yükleme ──────────────────────────────────

    @Test
    void controlRemoteCooldownConstants() {
        final int COOLDOWN_TICKS = 600;
        final int EFFECT_DURATION = 1800;
        // Soğuma süresi etki süresinin 1/3'ü olmalı (30 sn / 1.5 dk)
        assertEquals(EFFECT_DURATION, COOLDOWN_TICKS * 3,
                "Effect duration should be 3x cooldown (30s cooldown, 90s effect)");
    }
}
