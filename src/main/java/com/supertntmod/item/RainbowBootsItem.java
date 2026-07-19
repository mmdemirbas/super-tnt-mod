package com.supertntmod.item;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Gökkuşağı Botları: giyildiğinde adım attığın yerde beyaz yün blok çıkar.
 * Boşluğa düşmek imkânsız.
 */
public class RainbowBootsItem extends Item {

    public RainbowBootsItem(Settings settings) {
        super(settings);
    }

    /** SuperTntMod tick event'inden çağrılır. */
    public static void onTick(ServerPlayerEntity player) {
        ItemStack boots = player.getEquippedStack(EquipmentSlot.FEET);
        if (!boots.isOf(ModItems.RAINBOW_BOOTS)) return;
        ServerWorld world = (ServerWorld) player.getEntityWorld();

        // Yalnızca ayağın hemen altını doldur. feetPos'a yün koymak oyuncuyu
        // bloğun içinde bırakıp çarpışma fiziği ile yukarı itiyordu — zıplama
        // kırılıyor ve dar tavanlı koridorda kid kapana kısılıyordu.
        BlockPos below = player.getBlockPos().down();
        if (world.getBlockState(below).isAir()) {
            world.setBlockState(below, Blocks.WHITE_WOOL.getDefaultState());
            trail(player.getUuid()).addLast(below.toImmutable());
            trimTrail(world, player.getUuid());
        }
    }

    /**
     * İz uzunluğu sınırlı. Sınır yokken botla dolaşmak araziyi kalıcı olarak
     * yünle kaplıyordu; birkaç dakika yürümek dünyayı bozuyordu.
     */
    private static final int TRAIL_LIMIT = 64;
    private static final Map<UUID, Deque<BlockPos>> TRAILS = new ConcurrentHashMap<>();

    private static Deque<BlockPos> trail(UUID uuid) {
        return TRAILS.computeIfAbsent(uuid, u -> new ConcurrentLinkedDeque<>());
    }

    private static void trimTrail(ServerWorld world, UUID uuid) {
        Deque<BlockPos> t = trail(uuid);
        while (t.size() > TRAIL_LIMIT) {
            BlockPos old = t.pollFirst();
            if (old == null) break;
            // Oyuncu üzerine başka blok koymuş olabilir — sadece kendi yünümüzü al.
            if (world.getBlockState(old).isOf(Blocks.WHITE_WOOL)) {
                world.setBlockState(old, Blocks.AIR.getDefaultState());
            }
        }
    }

    public static void onPlayerDisconnect(UUID uuid) {
        TRAILS.remove(uuid);
    }

    public static void clearAll() {
        TRAILS.clear();
    }
}
