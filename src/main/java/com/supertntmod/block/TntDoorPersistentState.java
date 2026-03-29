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
 * TNT kapı sahiplik bilgilerini sunucu yeniden başlatmalarında korur.
 */
public class TntDoorPersistentState extends PersistentState {
    final Map<BlockPos, UUID> owners = new HashMap<>();

    private record DoorEntry(BlockPos pos, UUID owner) {
        static final Codec<DoorEntry> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        BlockPos.CODEC.fieldOf("pos").forGetter(DoorEntry::pos),
                        Uuids.CODEC.fieldOf("owner").forGetter(DoorEntry::owner)
                ).apply(instance, DoorEntry::new)
        );
    }

    private static final Codec<TntDoorPersistentState> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    DoorEntry.CODEC.listOf().fieldOf("doors").forGetter(state -> {
                        List<DoorEntry> entries = new ArrayList<>();
                        state.owners.forEach((pos, uuid) -> entries.add(new DoorEntry(pos, uuid)));
                        return entries;
                    })
            ).apply(instance, entries -> {
                TntDoorPersistentState state = new TntDoorPersistentState();
                for (DoorEntry entry : entries) {
                    state.owners.put(entry.pos(), entry.owner());
                }
                return state;
            })
    );

    public static final PersistentStateType<TntDoorPersistentState> TYPE =
            new PersistentStateType<>("supertntmod_tnt_doors", TntDoorPersistentState::new, CODEC, null);

    public static TntDoorPersistentState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(TYPE);
    }
}
