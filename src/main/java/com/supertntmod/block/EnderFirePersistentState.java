package com.supertntmod.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Bir boyuttaki Ender Ateşi konumlarını sunucu yeniden başlatmalarında korur.
 *
 * Statik bir harita yetmez: onBlockAdded chunk yüklenmesinde çağrılmaz, bu yüzden
 * yeniden başlatmadan sonra eski ateşler ağdan düşer ve ışınlanma hedefsiz kalır.
 * Boyut ayrımı ücretsiz gelir — durum zaten ServerWorld başına tutuluyor.
 */
public class EnderFirePersistentState extends PersistentState {

    private final Set<BlockPos> fires = new LinkedHashSet<>();

    private static final Codec<EnderFirePersistentState> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BlockPos.CODEC.listOf().fieldOf("fires")
                            .forGetter(state -> new ArrayList<>(state.fires))
            ).apply(instance, list -> {
                EnderFirePersistentState state = new EnderFirePersistentState();
                state.fires.addAll(list);
                return state;
            })
    );

    public static final PersistentStateType<EnderFirePersistentState> TYPE =
            new PersistentStateType<>("supertntmod_ender_fires", EnderFirePersistentState::new, CODEC, null);

    public static EnderFirePersistentState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(TYPE);
    }

    public void add(BlockPos pos) {
        if (fires.add(pos.toImmutable())) markDirty();
    }

    public void remove(BlockPos pos) {
        if (fires.remove(pos.toImmutable())) markDirty();
    }

    /** Yineleme sırasında yetim kayıtları silebilmek için kopya döner. */
    public List<BlockPos> snapshot() {
        return new ArrayList<>(fires);
    }
}
