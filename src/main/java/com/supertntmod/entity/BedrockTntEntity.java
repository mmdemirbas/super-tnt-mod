package com.supertntmod.entity;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * 🪨 Bedrock TNT
 * Blast resistance hesaba katmadan her bloğu kırar.
 * 25 blok yarıçapında dünyayı yok eder.
 * Performans: İşlem birden fazla tick'e yayılır.
 */
public class BedrockTntEntity extends TntEntity {
    private static final int RADIUS = 25;
    private boolean done = false;

    // Kademeli işleme durumu
    private boolean processing = false;
    private BlockPos center;
    private int idx = 0;
    private static final int MODIFICATIONS_PER_TICK = 1000;

    public BedrockTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public BedrockTntEntity(World world, double x, double y, double z,
                            @Nullable LivingEntity igniter) {
        super(ModEntities.BEDROCK_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    @Override
    public void tick() {
        // Kademeli blok işleme
        if (processing && !this.getEntityWorld().isClient()) {
            World world = getEntityWorld();
            int side = RADIUS * 2 + 1;
            int total = side * side * side;
            int modified = 0;

            while (idx < total && modified < MODIFICATIONS_PER_TICK) {
                int lx = idx % side - RADIUS;
                int ly = (idx / side) % side - RADIUS;
                int lz = (idx / (side * side)) - RADIUS;
                idx++;

                BlockPos pos = center.add(lx, ly, lz);
                if (!pos.isWithinDistance(center, RADIUS)) continue;

                if (!world.getBlockState(pos).isOf(Blocks.AIR)) {
                    world.breakBlock(pos, true);
                    modified++;
                }
            }

            if (idx >= total) {
                processing = false;

                // Devasa toz ve moloz bulutu
                if (world instanceof ServerWorld serverWorld) {
                    serverWorld.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                            center.getX() + 0.5, center.getY() + 5, center.getZ() + 0.5,
                            500, 12.0, 8.0, 12.0, 0.02);
                    serverWorld.spawnParticles(ParticleTypes.ASH,
                            center.getX() + 0.5, center.getY() + 10, center.getZ() + 0.5,
                            500, 15.0, 10.0, 15.0, 0.01);
                    serverWorld.spawnParticles(ParticleTypes.EXPLOSION_EMITTER,
                            center.getX() + 0.5, center.getY() + 2, center.getZ() + 0.5,
                            10, 5.0, 3.0, 5.0, 0.0);
                }

                // Devasa görsel patlama efekti
                world.createExplosion(null, center.getX() + 0.5, center.getY(),
                        center.getZ() + 0.5, 12.0f, false, World.ExplosionSourceType.TNT);
                this.discard();
            }
            return;
        }

        if (!done && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            done = true;
            center = this.getBlockPos();
            processing = true;
            idx = 0;
            // Dünya yıkım sesi hemen çalsın
            World world = getEntityWorld();
            world.playSound(null, center.getX(), center.getY(), center.getZ(),
                    SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.BLOCKS, 3.0f, 0.3f);
            world.playSound(null, center.getX(), center.getY(), center.getZ(),
                    SoundEvents.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.BLOCKS, 2.0f, 0.5f);
            return;
        }
        if (!done) super.tick();
    }
}
