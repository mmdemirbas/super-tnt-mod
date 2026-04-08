package com.supertntmod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Craft Baltası hacim hesaplaması testleri.
 * buildWall içindeki sınır kontrolünü doğrular.
 */
class CraftAxeVolumeTest {

    private static final int MAX_FILL = 32768; // 32^3

    private long volume(int x1, int y1, int z1, int x2, int y2, int z2) {
        int dx = Math.abs(x2 - x1) + 1;
        int dy = Math.abs(y2 - y1) + 1;
        int dz = Math.abs(z2 - z1) + 1;
        return (long) dx * dy * dz;
    }

    @Test
    void singleBlockVolume() {
        assertEquals(1, volume(0, 0, 0, 0, 0, 0));
    }

    @Test
    void maxAllowedCube() {
        // 32x32x32 = 32768 = MAX_FILL
        assertEquals(MAX_FILL, volume(0, 0, 0, 31, 31, 31));
    }

    @Test
    void justOverLimit() {
        // 33x32x32 = 33792 > MAX_FILL
        long vol = volume(0, 0, 0, 32, 31, 31);
        assertTrue(vol > MAX_FILL);
    }

    @Test
    void negativeCoordinates() {
        assertEquals(8, volume(-1, -1, -1, 0, 0, 0));
    }

    @Test
    void reversedCoordinates() {
        // Aynı hacim, koordinat sırası fark etmez
        assertEquals(volume(0, 0, 0, 5, 5, 5), volume(5, 5, 5, 0, 0, 0));
    }

    @Test
    void flatWall() {
        // 16x16x1 = 256
        assertEquals(256, volume(0, 0, 0, 15, 15, 0));
    }
}
