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
import java.util.Optional;
import java.util.UUID;

/**
 * Şifreli TNT sandık verilerini (sahip, şifre, envanter) sunucu
 * yeniden başlatmalarında korur.
 */
public class ChestPersistentState extends PersistentState {
    final Map<BlockPos, UUID> owners = new HashMap<>();
    final Map<BlockPos, String> passwords = new HashMap<>();
    final Map<BlockPos, SimpleInventory> inventories = new HashMap<>();

    private record ChestEntry(BlockPos pos, UUID owner, Optional<String> password,
                              List<ItemStack> items) {
        static final Codec<ChestEntry> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        BlockPos.CODEC.fieldOf("pos").forGetter(ChestEntry::pos),
                        Uuids.CODEC.fieldOf("owner").forGetter(ChestEntry::owner),
                        Codec.STRING.optionalFieldOf("password").forGetter(ChestEntry::password),
                        ItemStack.OPTIONAL_CODEC.listOf().fieldOf("items").forGetter(ChestEntry::items)
                ).apply(instance, ChestEntry::new)
        );
    }

    private static final Codec<ChestPersistentState> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ChestEntry.CODEC.listOf().fieldOf("chests").forGetter(state -> {
                        List<ChestEntry> entries = new ArrayList<>();
                        state.owners.forEach((pos, uuid) -> {
                            String pw = state.passwords.get(pos);
                            SimpleInventory inv = state.inventories.get(pos);
                            List<ItemStack> items = new ArrayList<>();
                            if (inv != null) {
                                for (int i = 0; i < inv.size(); i++) {
                                    items.add(inv.getStack(i));
                                }
                            }
                            entries.add(new ChestEntry(pos, uuid, Optional.ofNullable(pw), items));
                        });
                        return entries;
                    })
            ).apply(instance, entries -> {
                ChestPersistentState state = new ChestPersistentState();
                for (ChestEntry entry : entries) {
                    state.owners.put(entry.pos(), entry.owner());
                    entry.password().ifPresent(pw -> state.passwords.put(entry.pos(), pw));
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

    public static final PersistentStateType<ChestPersistentState> TYPE =
            new PersistentStateType<>("supertntmod_encrypted_chests", ChestPersistentState::new, CODEC, null);

    public static ChestPersistentState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(TYPE);
    }
}
