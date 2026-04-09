package com.supertntmod.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
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
 * Blocker Sandık verilerini (sahip, envanter) kalıcı olarak saklar.
 */
public class BlockerChestPersistentState extends PersistentState {
    final Map<BlockPos, UUID> owners = new HashMap<>();
    final Map<BlockPos, SimpleInventory> inventories = new HashMap<>();

    private record Entry(BlockPos pos, UUID owner, List<ItemStack> items) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        BlockPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
                        Uuids.CODEC.fieldOf("owner").forGetter(Entry::owner),
                        ItemStack.OPTIONAL_CODEC.listOf().fieldOf("items").forGetter(Entry::items)
                ).apply(instance, Entry::new)
        );
    }

    private static final Codec<BlockerChestPersistentState> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Entry.CODEC.listOf().fieldOf("chests").forGetter(state -> {
                        List<Entry> entries = new ArrayList<>();
                        state.owners.forEach((pos, uuid) -> {
                            SimpleInventory inv = state.inventories.get(pos);
                            List<ItemStack> items = new ArrayList<>();
                            if (inv != null) {
                                for (int i = 0; i < inv.size(); i++) {
                                    items.add(inv.getStack(i));
                                }
                            }
                            entries.add(new Entry(pos, uuid, items));
                        });
                        return entries;
                    })
            ).apply(instance, entries -> {
                BlockerChestPersistentState state = new BlockerChestPersistentState();
                for (Entry entry : entries) {
                    state.owners.put(entry.pos(), entry.owner());
                    SimpleInventory inv = new SimpleInventory(27);
                    List<ItemStack> items = entry.items();
                    for (int i = 0; i < Math.min(items.size(), 27); i++) {
                        inv.setStack(i, items.get(i));
                    }
                    inv.addListener(sender -> state.markDirty());
                    state.inventories.put(entry.pos(), inv);
                }
                return state;
            })
    );

    public static final PersistentStateType<BlockerChestPersistentState> TYPE =
            new PersistentStateType<>("supertntmod_blocker_chests", BlockerChestPersistentState::new, CODEC, null);

    public static BlockerChestPersistentState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(TYPE);
    }
}
