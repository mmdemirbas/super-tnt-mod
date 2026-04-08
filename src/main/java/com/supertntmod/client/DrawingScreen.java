package com.supertntmod.client;

import com.supertntmod.network.DrawingC2SPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/**
 * 16x16 piksel, 16 katmanlı 3D çizim ekranı.
 * Sol tarafta tuval, sağ tarafta 2x8 renk paleti.
 * Katman geçişi butonlarıyla 3 boyutlu yapılar çizilebilir.
 * Ctrl+Z ile geri al, hover önizleme, seçili renk göstergesi.
 * "Yap" butonu çizimi lego bloklarından inşa eder.
 */
@Environment(EnvType.CLIENT)
public class DrawingScreen extends Screen {

    private static final int CANVAS_SIZE = 16;
    private static final int LAYERS = DrawingC2SPayload.LAYERS;
    private static final int CELL_SIZE = 14;
    private static final int PALETTE_CELL = 18;
    private static final int PALETTE_COLS = 2;
    private static final int PALETTE_ROWS = 8;
    private static final int PALETTE_GAP = 2;
    private static final int MAX_UNDO = 30;

    // Lego tuğla renkleri (Minecraft yün sırası ile eşleşir)
    private static final int[] LEGO_COLORS = {
            0xFFE9ECEC, // white
            0xFFF07613, // orange
            0xFFBD44B3, // magenta
            0xFF3AAFD9, // light_blue
            0xFFF8C527, // yellow
            0xFF70B919, // lime
            0xFFED8DAC, // pink
            0xFF3E4447, // gray
            0xFF8E8E86, // light_gray
            0xFF158991, // cyan
            0xFF792AAC, // purple
            0xFF35399D, // blue
            0xFF724728, // brown
            0xFF546D1B, // green
            0xFFA12722, // red
            0xFF141519  // black
    };

    // 3D piksel dizisi: [layer * 256 + y * 16 + x], 0=boş, 1-16=renk
    private final byte[] pixels = new byte[CANVAS_SIZE * CANVAS_SIZE * LAYERS];
    private int selectedColor = 1;
    private int currentLayer = 0;
    private final float playerYaw;
    private final Deque<byte[]> undoHistory = new ArrayDeque<>();

    private int canvasX, canvasY;
    private int paletteX, paletteY;

    public DrawingScreen(float playerYaw) {
        super(Text.translatable("screen.supertntmod.drawing"));
        this.playerYaw = playerYaw;
    }

    @Override
    protected void init() {
        super.init();

        int totalCanvasW = CANVAS_SIZE * CELL_SIZE;
        int totalCanvasH = CANVAS_SIZE * CELL_SIZE;
        int paletteW = PALETTE_COLS * PALETTE_CELL + (PALETTE_COLS - 1) * PALETTE_GAP;
        int gap = 12;

        int totalW = totalCanvasW + gap + paletteW;
        int startX = (width - totalW) / 2;
        canvasX = startX;
        canvasY = (height - totalCanvasH) / 2 - 16;
        paletteX = canvasX + totalCanvasW + gap;
        paletteY = canvasY;

        int btnY = canvasY + totalCanvasH + 6;
        int btnW = 54;
        int btnGap = 4;
        int bx = canvasX;

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.supertntmod.drawing.build"),
                btn -> buildAndClose()
        ).dimensions(bx, btnY, btnW, 20).build());
        bx += btnW + btnGap;

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.supertntmod.drawing.clear"),
                btn -> { pushUndo(); clearCurrentLayer(); }
        ).dimensions(bx, btnY, btnW, 20).build());
        bx += btnW + btnGap;

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.supertntmod.drawing.eraser"),
                btn -> selectedColor = 0
        ).dimensions(bx, btnY, btnW, 20).build());
        bx += btnW + btnGap;

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.supertntmod.drawing.undo"),
                btn -> popUndo()
        ).dimensions(bx, btnY, btnW, 20).build());

        // Katman geçiş butonları
        int layerBtnY = btnY + 24;
        int layerBtnW = 64;

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.supertntmod.drawing.prev_layer"),
                btn -> { if (currentLayer > 0) currentLayer--; }
        ).dimensions(canvasX, layerBtnY, layerBtnW, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.supertntmod.drawing.next_layer"),
                btn -> { if (currentLayer < LAYERS - 1) currentLayer++; }
        ).dimensions(canvasX + layerBtnW + 4, layerBtnY, layerBtnW, 20).build());
    }

    /**
     * renderBackground: sadece panel alanına koyu arka plan çiz.
     * Tam ekran dark overlay çizmeyerek hotbar görünür kalır.
     */
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        int totalCanvasW = CANVAS_SIZE * CELL_SIZE;
        int totalCanvasH = CANVAS_SIZE * CELL_SIZE;
        int paletteW = PALETTE_COLS * PALETTE_CELL + (PALETTE_COLS - 1) * PALETTE_GAP;
        int panelPad = 10;
        int panelX = (width - (totalCanvasW + 12 + paletteW)) / 2 - panelPad;
        int panelY = (height - totalCanvasH) / 2 - 16 - 22;
        int panelW = totalCanvasW + 12 + paletteW + panelPad * 2;
        int panelH = totalCanvasH + 95;
        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xCC222222);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // super.render = renderBackground (panel only) + tüm widget'lar
        super.render(context, mouseX, mouseY, delta);

        int totalCanvasW = CANVAS_SIZE * CELL_SIZE;
        int totalCanvasH = CANVAS_SIZE * CELL_SIZE;

        // Başlık
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, canvasY - 14, 0xFFFFFF);

        // Katman göstergesi
        context.drawTextWithShadow(textRenderer,
                Text.literal("Katman: " + (currentLayer + 1) + "/" + LAYERS),
                canvasX, canvasY + totalCanvasH + 30, 0xFFFFAA);

        // Tuval çerçevesi
        context.fill(canvasX - 2, canvasY - 2,
                canvasX + totalCanvasW + 2, canvasY + totalCanvasH + 2, 0xFF333333);

        // Pikseller (aktif katman)
        int layerOffset = currentLayer * CANVAS_SIZE * CANVAS_SIZE;
        for (int y = 0; y < CANVAS_SIZE; y++) {
            for (int x = 0; x < CANVAS_SIZE; x++) {
                int px = canvasX + x * CELL_SIZE;
                int py = canvasY + y * CELL_SIZE;
                int colorIdx = pixels[layerOffset + y * CANVAS_SIZE + x];
                if (colorIdx > 0 && colorIdx <= 16) {
                    context.fill(px, py, px + CELL_SIZE, py + CELL_SIZE, LEGO_COLORS[colorIdx - 1]);
                } else {
                    context.fill(px, py, px + CELL_SIZE, py + CELL_SIZE, 0xFFFFFFFF);
                    if ((x + y) % 2 == 0) {
                        context.fill(px, py, px + CELL_SIZE, py + CELL_SIZE, 0xFFEEEEEE);
                    }
                }
            }
        }

        // Izgara çizgileri
        for (int i = 0; i <= CANVAS_SIZE; i++) {
            context.fill(canvasX + i * CELL_SIZE, canvasY,
                    canvasX + i * CELL_SIZE + 1, canvasY + totalCanvasH, 0x44000000);
            context.fill(canvasX, canvasY + i * CELL_SIZE,
                    canvasX + totalCanvasW, canvasY + i * CELL_SIZE + 1, 0x44000000);
        }

        // Hover vurgusu
        if (mouseX >= canvasX && mouseX < canvasX + totalCanvasW
                && mouseY >= canvasY && mouseY < canvasY + totalCanvasH) {
            int hx = (mouseX - canvasX) / CELL_SIZE;
            int hy = (mouseY - canvasY) / CELL_SIZE;
            if (hx >= 0 && hx < CANVAS_SIZE && hy >= 0 && hy < CANVAS_SIZE) {
                int hpx = canvasX + hx * CELL_SIZE;
                int hpy = canvasY + hy * CELL_SIZE;
                if (selectedColor > 0 && selectedColor <= 16) {
                    context.fill(hpx, hpy, hpx + CELL_SIZE, hpy + CELL_SIZE,
                            (LEGO_COLORS[selectedColor - 1] & 0x00FFFFFF) | 0x88000000);
                } else {
                    context.fill(hpx, hpy, hpx + CELL_SIZE, hpy + 1, 0xAAFF0000);
                    context.fill(hpx, hpy + CELL_SIZE - 1, hpx + CELL_SIZE, hpy + CELL_SIZE, 0xAAFF0000);
                    context.fill(hpx, hpy, hpx + 1, hpy + CELL_SIZE, 0xAAFF0000);
                    context.fill(hpx + CELL_SIZE - 1, hpy, hpx + CELL_SIZE, hpy + CELL_SIZE, 0xAAFF0000);
                }
            }
        }

        // Renk paleti
        for (int i = 0; i < 16; i++) {
            int col = i / PALETTE_ROWS;
            int row = i % PALETTE_ROWS;
            int px = paletteX + col * (PALETTE_CELL + PALETTE_GAP);
            int py = paletteY + row * (PALETTE_CELL + PALETTE_GAP);
            if (selectedColor == i + 1) {
                context.fill(px - 2, py - 2, px + PALETTE_CELL + 2, py + PALETTE_CELL + 2, 0xFFFFFF00);
            }
            context.fill(px, py, px + PALETTE_CELL, py + PALETTE_CELL, LEGO_COLORS[i]);
        }

        // Seçili renk önizlemesi
        int previewSize = PALETTE_COLS * PALETTE_CELL + (PALETTE_COLS - 1) * PALETTE_GAP;
        int previewY = paletteY + PALETTE_ROWS * (PALETTE_CELL + PALETTE_GAP) + 8;
        context.fill(paletteX - 1, previewY - 1,
                paletteX + previewSize + 1, previewY + previewSize + 1, 0xFFAAAAAA);
        if (selectedColor > 0 && selectedColor <= 16) {
            context.fill(paletteX, previewY,
                    paletteX + previewSize, previewY + previewSize, LEGO_COLORS[selectedColor - 1]);
        } else {
            for (int dy = 0; dy < previewSize; dy += 4) {
                for (int dx = 0; dx < previewSize; dx += 4) {
                    int color = ((dx / 4 + dy / 4) % 2 == 0) ? 0xFFFFFFFF : 0xFFCCCCCC;
                    context.fill(paletteX + dx, previewY + dy,
                            Math.min(paletteX + dx + 4, paletteX + previewSize),
                            Math.min(previewY + dy + 4, previewY + previewSize), color);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        if (super.mouseClicked(click, bl)) return true;
        if (isOnCanvas(click.x(), click.y())) {
            pushUndo();
            tryPaintAt(click.x(), click.y());
            return true;
        }
        if (trySelectColor(click.x(), click.y())) return true;
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (super.mouseDragged(click, deltaX, deltaY)) return true;
        return tryPaintAt(click.x(), click.y());
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.getKeycode() == InputUtil.GLFW_KEY_Z && input.hasCtrlOrCmd()) {
            popUndo();
            return true;
        }
        return super.keyPressed(input);
    }

    private boolean isOnCanvas(double mouseX, double mouseY) {
        int totalCanvasW = CANVAS_SIZE * CELL_SIZE;
        int totalCanvasH = CANVAS_SIZE * CELL_SIZE;
        return mouseX >= canvasX && mouseX < canvasX + totalCanvasW
                && mouseY >= canvasY && mouseY < canvasY + totalCanvasH;
    }

    private boolean tryPaintAt(double mouseX, double mouseY) {
        if (!isOnCanvas(mouseX, mouseY)) return false;
        int cx = (int) ((mouseX - canvasX) / CELL_SIZE);
        int cy = (int) ((mouseY - canvasY) / CELL_SIZE);
        if (cx >= 0 && cx < CANVAS_SIZE && cy >= 0 && cy < CANVAS_SIZE) {
            pixels[currentLayer * CANVAS_SIZE * CANVAS_SIZE + cy * CANVAS_SIZE + cx] = (byte) selectedColor;
            return true;
        }
        return false;
    }

    private boolean trySelectColor(double mouseX, double mouseY) {
        for (int i = 0; i < 16; i++) {
            int col = i / PALETTE_ROWS;
            int row = i % PALETTE_ROWS;
            int px = paletteX + col * (PALETTE_CELL + PALETTE_GAP);
            int py = paletteY + row * (PALETTE_CELL + PALETTE_GAP);
            if (mouseX >= px && mouseX < px + PALETTE_CELL
                    && mouseY >= py && mouseY < py + PALETTE_CELL) {
                selectedColor = i + 1;
                return true;
            }
        }
        return false;
    }

    private void pushUndo() {
        undoHistory.push(pixels.clone());
        while (undoHistory.size() > MAX_UNDO) undoHistory.removeLast();
    }

    private void popUndo() {
        if (!undoHistory.isEmpty()) {
            byte[] prev = undoHistory.pop();
            System.arraycopy(prev, 0, pixels, 0, pixels.length);
        }
    }

    private void buildAndClose() {
        boolean hasContent = false;
        for (byte p : pixels) {
            if (p != 0) { hasContent = true; break; }
        }
        if (!hasContent) { close(); return; }
        ClientPlayNetworking.send(new DrawingC2SPayload(pixels.clone(), playerYaw));
        close();
    }

    private void clearCurrentLayer() {
        int offset = currentLayer * CANVAS_SIZE * CANVAS_SIZE;
        Arrays.fill(pixels, offset, offset + CANVAS_SIZE * CANVAS_SIZE, (byte) 0);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
