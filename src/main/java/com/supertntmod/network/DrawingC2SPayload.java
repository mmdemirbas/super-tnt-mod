package com.supertntmod.network;

import com.supertntmod.SuperTntMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Çizim verisi: client -> server.
 * 16x16x16 piksel dizisi (her piksel 0-16: 0 = boş, 1-16 = lego renk indeksi)
 * + oyuncunun baktığı yön (yaw).
 * Katman 0 = oyuncuya en yakın, katman 15 = en uzak.
 */
public record DrawingC2SPayload(byte[] pixels, float yaw) implements CustomPayload {

    public static final int CANVAS_SIZE = 16;
    public static final int LAYERS = 16;
    public static final int PIXEL_COUNT = CANVAS_SIZE * CANVAS_SIZE * LAYERS;

    public static final CustomPayload.Id<DrawingC2SPayload> ID =
            new CustomPayload.Id<>(Identifier.of(SuperTntMod.MOD_ID, "drawing"));

    public static final PacketCodec<RegistryByteBuf, DrawingC2SPayload> CODEC =
            PacketCodec.of(DrawingC2SPayload::write, DrawingC2SPayload::read);

    private void write(RegistryByteBuf buf) {
        buf.writeByteArray(pixels);
        buf.writeFloat(yaw);
    }

    private static DrawingC2SPayload read(RegistryByteBuf buf) {
        byte[] px = buf.readByteArray(PIXEL_COUNT);
        float y = buf.readFloat();
        return new DrawingC2SPayload(px, y);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
