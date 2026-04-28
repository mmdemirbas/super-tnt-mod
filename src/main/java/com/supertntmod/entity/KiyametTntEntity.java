package com.supertntmod.entity;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Kıyamet TNT: Tüm oyuncuları öldürür + 35 blok yarıçapında her şeyi yok eder.
 * Dünya sıfırlanmaz — oyuncular doğal olarak yeniden doğar.
 * Kademeli işlem: tick başına 2000 blok.
 */
public class KiyametTntEntity extends TntEntity {
    private static final int RADIUS = 35;
    private static final int MOD_PER_TICK = 2000;

    private boolean done = false;
    private boolean processing = false;
    private BlockPos center;
    private int idx = 0;

    public KiyametTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public KiyametTntEntity(World world, double x, double y, double z,
                            @Nullable LivingEntity igniter) {
        super(ModEntities.KIYAMET_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    @Override
    public void tick() {
        if (processing && !this.getEntityWorld().isClient()) {
            World world = getEntityWorld();
            int side = RADIUS * 2 + 1;
            int total = side * side * side;
            int modified = 0;

            while (idx < total && modified < MOD_PER_TICK) {
                int lz = idx % side - RADIUS;
                int lx = (idx / side) % side - RADIUS;
                int ly = (idx / (side * side)) - RADIUS;
                idx++;

                BlockPos pos = center.add(lx, ly, lz);
                if (!pos.isWithinDistance(center, RADIUS)) continue;
                if (!world.getBlockState(pos).isOf(Blocks.AIR)
                        && !world.getBlockState(pos).isOf(Blocks.BEDROCK)) {
                    world.breakBlock(pos, false);
                    modified++;
                }
            }

            if (idx >= total) {
                processing = false;
                double cx = center.getX() + 0.5, cy = center.getY(), cz = center.getZ() + 0.5;

                if (world instanceof ServerWorld sw) {
                    sw.spawnParticles(ParticleTypes.EXPLOSION_EMITTER,
                            cx, cy + 5, cz, 30, 15.0, 10.0, 15.0, 0.0);
                    sw.spawnParticles(ParticleTypes.ASH,
                            cx, cy + 10, cz, 800, 20.0, 15.0, 20.0, 0.02);
                    sw.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                            cx, cy + 8, cz, 400, 18.0, 10.0, 18.0, 0.03);
                }

                world.createExplosion(null, cx, cy, cz, 15.0f, true, World.ExplosionSourceType.TNT);
                this.discard();
            }
            return;
        }

        if (!done && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            done = true;
            World world = getEntityWorld();
            center = this.getBlockPos();
            double cx = center.getX() + 0.5, cy = center.getY(), cz = center.getZ() + 0.5;

            // Tüm oyuncuları öldür
            if (world instanceof ServerWorld serverWorld) {
                for (ServerPlayerEntity player : serverWorld.getPlayers()) {
                    player.kill(serverWorld);
                }
            }

            world.playSound(null, cx, cy, cz,
                    SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.BLOCKS, 4.0f, 0.3f);
            world.playSound(null, cx, cy, cz,
                    SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.BLOCKS, 3.0f, 0.5f);

            processing = true;
            idx = 0;
            return;
        }
        if (!done) super.tick();
    }
}
