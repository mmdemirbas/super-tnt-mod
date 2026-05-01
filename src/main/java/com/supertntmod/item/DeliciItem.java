package com.supertntmod.item;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Delici Aleti (trol): kullanılınca her tick oyuncunun bakış yönünde
 * üç bloklu bir tünel açmaya başlar. Bir kez başlatınca durmaz —
 * itemi atana veya öldüğüne kadar devam eder. Saf trol.
 */
public class DeliciItem extends Item {
    private static final int DURATION_TICKS = 200; // 10 saniye
    private static final double STEP = 0.6; // her tick bu kadar ilerler
    private static final Map<UUID, Integer> ACTIVE = new ConcurrentHashMap<>();

    public DeliciItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient()) return ActionResult.SUCCESS;
        // 10 saniye boyunca aktif
        ACTIVE.put(user.getUuid(), DURATION_TICKS);
        user.sendMessage(Text.translatable("item.supertntmod.delici.activated").formatted(Formatting.RED), true);
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.BLOCK_NETHERITE_BLOCK_BREAK, SoundCategory.PLAYERS, 1.0f, 0.7f);
        return ActionResult.SUCCESS;
    }

    /** Sunucu kapanırken aktif kullanıcıları temizle. */
    public static void clearAll() {
        ACTIVE.clear();
    }

    public static void onPlayerDisconnect(UUID uuid) {
        ACTIVE.remove(uuid);
    }

    /**
     * Server tick'inde çağrılır. Aktif kullanıcıların önündeki blokları siler.
     */
    public static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) return;
        Iterator<Map.Entry<UUID, Integer>> iter = ACTIVE.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<UUID, Integer> entry = iter.next();
            int remaining = entry.getValue();
            if (remaining <= 0) { iter.remove(); continue; }

            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player == null || !player.isAlive()) { iter.remove(); continue; }
            if (!(player.getEntityWorld() instanceof ServerWorld world)) { iter.remove(); continue; }

            drillForward(world, player);
            entry.setValue(remaining - 1);
        }
    }

    private static void drillForward(ServerWorld world, ServerPlayerEntity player) {
        Vec3d look = player.getRotationVec(1.0f);
        Vec3d eye = player.getEyePos();
        Set<BlockPos> toBreak = new HashSet<>();
        // Bakış yönünde 0..3 metre arası 1x2 tünel (oyuncu boyu) deli
        for (double dist = 0.5; dist <= 3.0; dist += 0.5) {
            Vec3d p = eye.add(look.multiply(dist));
            BlockPos head = BlockPos.ofFloored(p);
            BlockPos feet = head.down();
            toBreak.add(head);
            toBreak.add(feet);
        }
        for (BlockPos pos : toBreak) {
            var state = world.getBlockState(pos);
            if (state.isAir()) continue;
            if (state.getHardness(world, pos) < 0) continue; // bedrock vb.
            world.breakBlock(pos, false);
        }
        // Görsel
        world.spawnParticles(ParticleTypes.SMOKE,
                eye.x + look.x * 1.5, eye.y + look.y * 1.5, eye.z + look.z * 1.5,
                10, 0.3, 0.3, 0.3, 0.05);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context,
                              TooltipDisplayComponent displayComponent,
                              Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
        textConsumer.accept(Text.translatable("item.supertntmod.delici.tooltip").formatted(Formatting.DARK_PURPLE));
        textConsumer.accept(Text.translatable("item.supertntmod.delici.tooltip2").formatted(Formatting.RED));
    }
}
