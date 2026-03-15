package com.supertntmod.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Odun TNT: Patlatınca 10 blok yakınındaki ağaçlar yok olur.
 * Önce odunlar düşer, sonra patlayıp ağaçları yok eder.
 * 1 dakika (1200 tick) sonra ağaçlar geri gelir.
 */
public class WoodTntEntity extends TntEntity {
    private static final int RADIUS = 10;
    private static final int RESTORE_DELAY = 1200; // 1 dakika = 1200 tick
    private boolean done = false;

    public WoodTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public WoodTntEntity(World world, double x, double y, double z,
                         @Nullable LivingEntity igniter) {
        super(ModEntities.WOOD_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    @Override
    public void tick() {
        if (!done && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            done = true;
            BlockPos center = this.getBlockPos();
            World world = getEntityWorld();
            this.discard();

            // Ağaç bloklarını bul ve kaydet
            Map<BlockPos, BlockState> savedBlocks = new HashMap<>();

            for (BlockPos pos : BlockPos.iterateOutwards(center, RADIUS, RADIUS, RADIUS)) {
                if (!pos.isWithinDistance(center, RADIUS)) continue;

                BlockState state = world.getBlockState(pos);

                // Ağaç gövdeleri (log) ve yaprakları tespit et
                if (state.isIn(BlockTags.LOGS) || state.isIn(BlockTags.LEAVES)) {
                    savedBlocks.put(pos.toImmutable(), state);
                }
            }

            // Önce odun item'lerini etrafa saç (animasyon efekti)
            int logCount = 0;
            for (Map.Entry<BlockPos, BlockState> entry : savedBlocks.entrySet()) {
                if (entry.getValue().isIn(BlockTags.LOGS) && logCount < 10) {
                    logCount++;
                    double ix = entry.getKey().getX() + 0.5;
                    double iy = entry.getKey().getY() + 0.5;
                    double iz = entry.getKey().getZ() + 0.5;
                    ItemEntity itemEntity = new ItemEntity(world, ix, iy, iz,
                            new ItemStack(Items.OAK_LOG));
                    itemEntity.setVelocity(
                            (world.random.nextDouble() - 0.5) * 0.5,
                            world.random.nextDouble() * 0.5 + 0.3,
                            (world.random.nextDouble() - 0.5) * 0.5);
                    // Kısa ömürlü - sadece görsel efekt
                    itemEntity.setDespawnImmediately();
                    world.spawnEntity(itemEntity);
                }
            }

            // Ağaç bloklarını kaldır
            for (BlockPos pos : savedBlocks.keySet()) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState());
            }

            // Küçük patlama efekti
            world.createExplosion(null, center.getX() + 0.5, center.getY(),
                    center.getZ() + 0.5, 2.0f, false, World.ExplosionSourceType.NONE);

            // 1 dakika sonra ağaçları geri getir
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.getServer().execute(() -> {
                    // Delayed task - schedule for later
                    scheduleRestore(serverWorld, savedBlocks, RESTORE_DELAY);
                });
            }
            return;
        }
        if (!done) super.tick();
    }

    private static void scheduleRestore(ServerWorld world, Map<BlockPos, BlockState> savedBlocks, int ticksRemaining) {
        // ServerTickEvents kullanarak geri yükleme zamanlayıcısı
        // Basit yaklaşım: tick sayacı ile
        final int[] counter = {ticksRemaining};

        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (counter[0] > 0) {
                counter[0]--;
                return;
            }
            if (counter[0] == 0) {
                counter[0] = -1; // Bir kez çalıştır
                for (Map.Entry<BlockPos, BlockState> entry : savedBlocks.entrySet()) {
                    BlockPos pos = entry.getKey();
                    BlockState state = entry.getValue();
                    // Sadece hava olan yerlere geri koy (oyuncu başka blok koymuşsa dokunma)
                    if (world.getBlockState(pos).isOf(Blocks.AIR)) {
                        world.setBlockState(pos, state);
                    }
                }
            }
        });
    }
}
