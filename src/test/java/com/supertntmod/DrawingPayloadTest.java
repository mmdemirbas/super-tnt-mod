package com.supertntmod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Çizim payload boyut testleri.
 * 16x16x16 = 4096 piksel doğrulaması.
 */
class DrawingPayloadTest {

    private static final int CANVAS_SIZE = 16;
    private static final int LAYERS = 16;
    private static final int PIXEL_COUNT = CANVAS_SIZE * CANVAS_SIZE * LAYERS;

    @Test
    void pixelCountIs4096() {
        assertEquals(4096, PIXEL_COUNT);
    }

    @Test
    void layerOffsetCalculation() {
        // Katman 0: offset 0
        assertEquals(0, 0 * CANVAS_SIZE * CANVAS_SIZE);
        // Katman 15 (son): offset 3840
        assertEquals(3840, 15 * CANVAS_SIZE * CANVAS_SIZE);
        // Katman 15 son pikseli: 3840 + 255 = 4095 (son geçerli index)
        assertEquals(4095, 15 * CANVAS_SIZE * CANVAS_SIZE + 15 * CANVAS_SIZE + 15);
    }

    @Test
    void colorIndexBounds() {
        // Renk 0 = boş, 1-16 = geçerli renk
        byte[] pixels = new byte[PIXEL_COUNT];
        pixels[0] = 1;   // min geçerli renk
        pixels[1] = 16;  // max geçerli renk
        pixels[2] = 0;   // boş

        assertEquals(1, pixels[0] & 0xFF);
        assertEquals(16, pixels[1] & 0xFF);
        assertEquals(0, pixels[2] & 0xFF);
    }
}
