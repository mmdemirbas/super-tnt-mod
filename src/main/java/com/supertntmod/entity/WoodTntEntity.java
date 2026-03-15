package com.supertntmod.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

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

            // Yaprak partikülleri ve odun kırılma sesi
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.CHERRY_LEAVES,
                        center.getX() + 0.5, center.getY() + 5, center.getZ() + 0.5,
                        200, 5.0, 4.0, 5.0, 0.05);
                serverWorld.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        center.getX() + 0.5, center.getY() + 2, center.getZ() + 0.5,
                        50, 3.0, 2.0, 3.0, 0.01);
            }
            world.playSound(null, center.getX(), center.getY(), center.getZ(),
                    SoundEvents.BLOCK_WOOD_BREAK, SoundCategory.BLOCKS, 2.0f, 0.8f);

            // Küçük patlama efekti
            world.createExplosion(null, center.getX() + 0.5, center.getY(),
                    center.getZ() + 0.5, 2.0f, false, World.ExplosionSourceType.NONE);

            // 1 dakika sonra ağaçları geri getir
            if (world instanceof ServerWorld serverWorld) {
                pendingRestores.add(new PendingRestore(serverWorld, savedBlocks, RESTORE_DELAY));
            }
            return;
        }
        if (!done) super.tick();
    }

    // Bekleyen geri yükleme görevleri (thread-safe)
    private static final List<PendingRestore> pendingRestores = new CopyOnWriteArrayList<>();

    private record PendingRestore(ServerWorld world, Map<BlockPos, BlockState> savedBlocks, int ticksRemaining) {
        PendingRestore tick() {
            return new PendingRestore(world, savedBlocks, ticksRemaining - 1);
        }
    }

    /**
     * SuperTntMod.onInitialize() içinde ServerTickEvents.END_SERVER_TICK ile
     * bir kez kaydedilmeli. Her tick'te bekleyen geri yüklemeleri işler.
     */
    public static void tickRestores() {
        if (pendingRestores.isEmpty()) return;

        List<PendingRestore> completed = new ArrayList<>();
        List<PendingRestore> updated = new ArrayList<>();

        for (PendingRestore restore : pendingRestores) {
            if (restore.ticksRemaining() <= 0) {
                completed.add(restore);
            } else {
                updated.add(restore.tick());
            }
        }

        for (PendingRestore restore : completed) {
            for (Map.Entry<BlockPos, BlockState> entry : restore.savedBlocks().entrySet()) {
                BlockPos pos = entry.getKey();
                BlockState state = entry.getValue();
                // Sadece hava olan yerlere geri koy (oyuncu başka blok koymuşsa dokunma)
                if (restore.world().getBlockState(pos).isOf(Blocks.AIR)) {
                    restore.world().setBlockState(pos, state);
                }
            }
        }

        pendingRestores.clear();
        pendingRestores.addAll(updated);
    }
}
