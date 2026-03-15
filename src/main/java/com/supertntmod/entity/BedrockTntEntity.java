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
 * Toz ve moloz partikülleri.
 */
public class BedrockTntEntity extends TntEntity {
    private static final int RADIUS = 7;
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

            // Ağır kırılma sesi
            world.playSound(null, center.getX(), center.getY(), center.getZ(),
                    SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.BLOCKS, 1.5f, 0.5f);

            // Blast resistance'ı tamamen atla, doğrudan kır
            for (BlockPos pos : BlockPos.iterateOutwards(center, RADIUS, RADIUS, RADIUS)) {
                if (pos.isWithinDistance(center, RADIUS)) {
                    if (!world.getBlockState(pos).isOf(Blocks.AIR)) {
                        world.breakBlock(pos, true);
                    }
                }
            }

            // Toz ve moloz partikülleri
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5,
                        150, 4.0, 2.0, 4.0, 0.02);
                serverWorld.spawnParticles(ParticleTypes.ASH,
                        center.getX() + 0.5, center.getY() + 3, center.getZ() + 0.5,
                        200, 5.0, 4.0, 5.0, 0.01);
            }

            // Görsel patlama efekti
            world.createExplosion(null, center.getX() + 0.5, center.getY(),
                    center.getZ() + 0.5, 6.0f, false, World.ExplosionSourceType.TNT);
            return;
        }
        if (!done) super.tick();
    }
}
