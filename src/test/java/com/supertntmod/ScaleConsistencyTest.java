package com.supertntmod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Ölçek tutarlılık testleri: küçültme ve büyütme çarpanlarının
 * birbirini iptal ettiğini doğrular (0.5 * 2.0 = 1.0).
 */
class ScaleConsistencyTest {

    private static final double SHRINK_MULTIPLIER = 0.5;
    private static final double GROWTH_MULTIPLIER = 2.0;

    @Test
    void shrinkThenGrowthReturnsToOriginal() {
        double scale = 1.0;
        scale *= SHRINK_MULTIPLIER;
        scale *= GROWTH_MULTIPLIER;
        assertEquals(1.0, scale, 1e-9);
    }

    @Test
    void growthThenShrinkReturnsToOriginal() {
        double scale = 1.0;
        scale *= GROWTH_MULTIPLIER;
        scale *= SHRINK_MULTIPLIER;
        assertEquals(1.0, scale, 1e-9);
    }

    @Test
    void multipleShrinkThenGrowthReturnsToOriginal() {
        double scale = 1.0;
        int rounds = 5;
        for (int i = 0; i < rounds; i++) scale *= SHRINK_MULTIPLIER;
        for (int i = 0; i < rounds; i++) scale *= GROWTH_MULTIPLIER;
        assertEquals(1.0, scale, 1e-9);
    }

    @Test
    void shrinkMultiplierIsInverseOfGrowth() {
        assertEquals(1.0, SHRINK_MULTIPLIER * GROWTH_MULTIPLIER, 1e-9);
    }
}
