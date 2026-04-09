package com.supertntmod.network;

import com.supertntmod.SuperTntMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Kontrol Kumandası: Client -> Server aktivasyon sinyali.
 */
public record ControlRemoteC2SPayload() implements CustomPayload {

    public static final CustomPayload.Id<ControlRemoteC2SPayload> ID =
            new CustomPayload.Id<>(Identifier.of(SuperTntMod.MOD_ID, "control_remote"));

    public static final PacketCodec<RegistryByteBuf, ControlRemoteC2SPayload> CODEC =
            PacketCodec.of(ControlRemoteC2SPayload::write, ControlRemoteC2SPayload::read);

    private void write(RegistryByteBuf buf) {
        // Boş payload — sadece aktivasyon sinyali
    }

    private static ControlRemoteC2SPayload read(RegistryByteBuf buf) {
        return new ControlRemoteC2SPayload();
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
