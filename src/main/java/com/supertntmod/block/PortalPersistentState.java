package com.supertntmod.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Portal pozisyonlarını sunucu yeniden başlatmalarında korur.
 * Her oyuncunun pembe ve yeşil portal pozisyonları Codec ile kaydedilir.
 */
public class PortalPersistentState extends PersistentState {
    final Map<UUID, BlockPos> pinkPortals = new HashMap<>();
    final Map<UUID, BlockPos> greenPortals = new HashMap<>();
    final Map<BlockPos, UUID> portalOwners = new HashMap<>();

    private record PortalEntry(UUID owner, boolean isPink, BlockPos pos) {
        static final Codec<PortalEntry> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Uuids.CODEC.fieldOf("owner").forGetter(PortalEntry::owner),
                        Codec.BOOL.fieldOf("pink").forGetter(PortalEntry::isPink),
                        BlockPos.CODEC.fieldOf("pos").forGetter(PortalEntry::pos)
                ).apply(instance, PortalEntry::new)
        );
    }

    private static final Codec<PortalPersistentState> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    PortalEntry.CODEC.listOf().fieldOf("portals").forGetter(state -> {
                        List<PortalEntry> entries = new ArrayList<>();
                        state.pinkPortals.forEach((uuid, pos) ->
                                entries.add(new PortalEntry(uuid, true, pos)));
                        state.greenPortals.forEach((uuid, pos) ->
                                entries.add(new PortalEntry(uuid, false, pos)));
                        return entries;
                    })
            ).apply(instance, entries -> {
                PortalPersistentState state = new PortalPersistentState();
                for (PortalEntry entry : entries) {
                    if (entry.isPink()) {
                        state.pinkPortals.put(entry.owner(), entry.pos());
                    } else {
                        state.greenPortals.put(entry.owner(), entry.pos());
                    }
                    state.portalOwners.put(entry.pos(), entry.owner());
                }
                return state;
            })
    );

    public static final PersistentStateType<PortalPersistentState> TYPE =
            new PersistentStateType<>("supertntmod_portals", PortalPersistentState::new, CODEC, null);

    public static PortalPersistentState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(TYPE);
    }
}
