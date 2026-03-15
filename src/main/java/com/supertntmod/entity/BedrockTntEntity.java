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
 * Bedrock, obsidian, taş - hepsi gider.
 * 25 blok yarıçapında dünyayı yok eder.
 */
public class BedrockTntEntity extends TntEntity {
    private static final int RADIUS = 25;
    private boolean done = false;

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
        if (!done && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            done = true;
            BlockPos center = this.getBlockPos();
            World world = getEntityWorld();
            this.discard();

            // Dünya yıkım sesi
            world.playSound(null, center.getX(), center.getY(), center.getZ(),
                    SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.BLOCKS, 3.0f, 0.3f);
            world.playSound(null, center.getX(), center.getY(), center.getZ(),
                    SoundEvents.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.BLOCKS, 2.0f, 0.5f);

            // Blast resistance'ı tamamen atla, doğrudan kır (25 blok yarıçap = 50 blok çap!)
            for (BlockPos pos : BlockPos.iterateOutwards(center, RADIUS, RADIUS, RADIUS)) {
                if (pos.isWithinDistance(center, RADIUS)) {
                    if (!world.getBlockState(pos).isOf(Blocks.AIR)) {
                        world.breakBlock(pos, true);
                    }
                }
            }

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
            return;
        }
        if (!done) super.tick();
    }
}
