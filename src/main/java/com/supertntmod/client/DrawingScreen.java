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
 * 16x16 piksel çizim ekranı.
 * Sol tarafta tuval, sağ tarafta 2x8 renk paleti.
 * Ctrl+Z ile geri al, hover önizleme, seçili renk göstergesi.
 * "Yap" butonu çizimi yün bloklarından inşa eder.
 */
@Environment(EnvType.CLIENT)
public class DrawingScreen extends Screen {

    private static final int CANVAS_SIZE = 16;
    private static final int CELL_SIZE = 14;
    private static final int PALETTE_CELL = 18;
    private static final int PALETTE_COLS = 2;
    private static final int PALETTE_ROWS = 8;
    private static final int PALETTE_GAP = 2;
    private static final int MAX_UNDO = 30;

    // Yün renkleri (Minecraft yün sırası): beyaz, turuncu, macenta, açık mavi,
    // sarı, fıstık, pembe, gri, açık gri, camgöbeği, mor, mavi,
    // kahverengi, yeşil, kırmızı, siyah
    private static final int[] WOOL_COLORS = {
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

    // Piksel verileri: 0 = boş, 1-16 = renk indeksi (WOOL_COLORS[i-1])
    private final byte[] pixels = new byte[CANVAS_SIZE * CANVAS_SIZE];
    private int selectedColor = 1; // varsayılan: beyaz (indeks 1)
    private final float playerYaw;
    private final Deque<byte[]> undoHistory = new ArrayDeque<>();

    // Tuval ve palet konumları (init'te hesaplanır)
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

        // Tuvali ve paleti ortala
        int totalW = totalCanvasW + gap + paletteW;
        int startX = (width - totalW) / 2;
        canvasX = startX;
        canvasY = (height - totalCanvasH) / 2 - 10;
        paletteX = canvasX + totalCanvasW + gap;
        paletteY = canvasY;

        // Butonlar
        int btnY = canvasY + totalCanvasH + 6;
        int btnW = 56;
        int btnGap = 4;
        int bx = canvasX;

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.supertntmod.drawing.build"),
                btn -> buildAndClose()
        ).dimensions(bx, btnY, btnW, 20).build());
        bx += btnW + btnGap;

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.supertntmod.drawing.clear"),
                btn -> { pushUndo(); clearCanvas(); }
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
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int totalCanvasW = CANVAS_SIZE * CELL_SIZE;
        int totalCanvasH = CANVAS_SIZE * CELL_SIZE;

        // Başlık
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, canvasY - 14, 0xFFFFFF);

        // Tuval arka planı (koyu gri çerçeve)
        context.fill(canvasX - 2, canvasY - 2,
                canvasX + totalCanvasW + 2, canvasY + totalCanvasH + 2, 0xFF333333);

        // Pikselleri çiz
        for (int y = 0; y < CANVAS_SIZE; y++) {
            for (int x = 0; x < CANVAS_SIZE; x++) {
                int px = canvasX + x * CELL_SIZE;
                int py = canvasY + y * CELL_SIZE;
                int colorIdx = pixels[y * CANVAS_SIZE + x];
                if (colorIdx > 0 && colorIdx <= 16) {
                    context.fill(px, py, px + CELL_SIZE, py + CELL_SIZE,
                            WOOL_COLORS[colorIdx - 1]);
                } else {
                    // Boş piksel: beyaz + açık gri dama deseni
                    context.fill(px, py, px + CELL_SIZE, py + CELL_SIZE, 0xFFFFFFFF);
                    if ((x + y) % 2 == 0) {
                        context.fill(px, py, px + CELL_SIZE, py + CELL_SIZE, 0xFFEEEEEE);
                    }
                }
            }
        }

        // Tuval ızgara çizgileri
        for (int i = 0; i <= CANVAS_SIZE; i++) {
            int lx = canvasX + i * CELL_SIZE;
            int ly = canvasY + i * CELL_SIZE;
            context.fill(lx, canvasY, lx + 1, canvasY + totalCanvasH, 0x44000000);
            context.fill(canvasX, ly, canvasX + totalCanvasW, ly + 1, 0x44000000);
        }

        // Tuval hover vurgusu
        if (mouseX >= canvasX && mouseX < canvasX + totalCanvasW
                && mouseY >= canvasY && mouseY < canvasY + totalCanvasH) {
            int hx = (mouseX - canvasX) / CELL_SIZE;
            int hy = (mouseY - canvasY) / CELL_SIZE;
            if (hx >= 0 && hx < CANVAS_SIZE && hy >= 0 && hy < CANVAS_SIZE) {
                int hpx = canvasX + hx * CELL_SIZE;
                int hpy = canvasY + hy * CELL_SIZE;
                if (selectedColor > 0 && selectedColor <= 16) {
                    // Yarı-saydam seçili renk önizlemesi
                    context.fill(hpx, hpy, hpx + CELL_SIZE, hpy + CELL_SIZE,
                            (WOOL_COLORS[selectedColor - 1] & 0x00FFFFFF) | 0x88000000);
                } else {
                    // Silgi modu: kırmızı çerçeve
                    context.fill(hpx, hpy, hpx + CELL_SIZE, hpy + 1, 0xAAFF0000);
                    context.fill(hpx, hpy + CELL_SIZE - 1, hpx + CELL_SIZE, hpy + CELL_SIZE, 0xAAFF0000);
                    context.fill(hpx, hpy, hpx + 1, hpy + CELL_SIZE, 0xAAFF0000);
                    context.fill(hpx + CELL_SIZE - 1, hpy, hpx + CELL_SIZE, hpy + CELL_SIZE, 0xAAFF0000);
                }
            }
        }

        // Renk paleti (2 sütun x 8 satır)
        for (int i = 0; i < 16; i++) {
            int col = i / PALETTE_ROWS;
            int row = i % PALETTE_ROWS;
            int px = paletteX + col * (PALETTE_CELL + PALETTE_GAP);
            int py = paletteY + row * (PALETTE_CELL + PALETTE_GAP);

            // Seçili renk vurgusu
            if (selectedColor == i + 1) {
                context.fill(px - 2, py - 2, px + PALETTE_CELL + 2, py + PALETTE_CELL + 2,
                        0xFFFFFF00);
            }

            context.fill(px, py, px + PALETTE_CELL, py + PALETTE_CELL, WOOL_COLORS[i]);
        }

        // Seçili renk önizlemesi (paletin altında)
        int previewSize = PALETTE_COLS * PALETTE_CELL + (PALETTE_COLS - 1) * PALETTE_GAP;
        int previewY = paletteY + PALETTE_ROWS * (PALETTE_CELL + PALETTE_GAP) + 8;
        context.fill(paletteX - 1, previewY - 1,
                paletteX + previewSize + 1, previewY + previewSize + 1, 0xFFAAAAAA);
        if (selectedColor > 0 && selectedColor <= 16) {
            context.fill(paletteX, previewY,
                    paletteX + previewSize, previewY + previewSize,
                    WOOL_COLORS[selectedColor - 1]);
        } else {
            // Silgi: dama deseni göstergesi
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

        // Tuval tıklaması: undo kaydı yap ve boya
        if (isOnCanvas(click.x(), click.y())) {
            pushUndo();
            tryPaintAt(click.x(), click.y());
            return true;
        }

        // Palet tıklaması
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
        // Ctrl+Z (veya Cmd+Z): geri al
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
            pixels[cy * CANVAS_SIZE + cx] = (byte) selectedColor;
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
        while (undoHistory.size() > MAX_UNDO) {
            undoHistory.removeLast();
        }
    }

    private void popUndo() {
        if (!undoHistory.isEmpty()) {
            byte[] prev = undoHistory.pop();
            System.arraycopy(prev, 0, pixels, 0, pixels.length);
        }
    }

    private void buildAndClose() {
        // Boş çizim kontrolü
        boolean hasContent = false;
        for (byte p : pixels) {
            if (p != 0) { hasContent = true; break; }
        }
        if (!hasContent) {
            close();
            return;
        }

        // Çizim verisini server'a gönder
        ClientPlayNetworking.send(new DrawingC2SPayload(pixels.clone(), playerYaw));
        close();
    }

    private void clearCanvas() {
        Arrays.fill(pixels, (byte) 0);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
