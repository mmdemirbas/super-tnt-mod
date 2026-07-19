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
            trail(player.getUuid()).addLast(new TrailBlock(world, below.toImmutable()));
            trimTrail(player.getUuid());
        }
    }

    /**
     * İz uzunluğu sınırlı. Sınır yokken botla dolaşmak araziyi kalıcı olarak
     * yünle kaplıyordu; birkaç dakika yürümek dünyayı bozuyordu.
     */
    private static final int TRAIL_LIMIT = 64;

    /** Dünya da saklanır; çıkışta/kapanışta izi süpürebilmek için gerekli. */
    private record TrailBlock(ServerWorld world, BlockPos pos) {}

    private static final Map<UUID, Deque<TrailBlock>> TRAILS = new ConcurrentHashMap<>();

    private static Deque<TrailBlock> trail(UUID uuid) {
        return TRAILS.computeIfAbsent(uuid, u -> new ConcurrentLinkedDeque<>());
    }

    private static void trimTrail(UUID uuid) {
        Deque<TrailBlock> t = trail(uuid);
        while (t.size() > TRAIL_LIMIT) {
            TrailBlock old = t.pollFirst();
            if (old == null) break;
            remove(old);
        }
    }

    /** Oyuncu üzerine başka blok koymuş olabilir — sadece kendi yünümüzü al. */
    private static void remove(TrailBlock b) {
        try {
            if (b.world().getBlockState(b.pos()).isOf(Blocks.WHITE_WOOL)) {
                b.world().setBlockState(b.pos(), Blocks.AIR.getDefaultState());
            }
        } catch (RuntimeException ignored) {
            // kapanış sırasında dünya erişimi bozulabilir
        }
    }

    /**
     * Cikista izi SIL demek, yunu dunyada birakmak demekti: 64 blok yuru,
     * cik, gir, tekrar — sinir tamamen atlaniyordu. Once temizle, sonra unut.
     */
    public static void onPlayerDisconnect(UUID uuid) {
        sweep(TRAILS.remove(uuid));
    }

    public static void clearAll() {
        for (Deque<TrailBlock> t : TRAILS.values()) sweep(t);
        TRAILS.clear();
    }

    private static void sweep(Deque<TrailBlock> t) {
        if (t == null) return;
        for (TrailBlock b : t) remove(b);
        t.clear();
    }
}
