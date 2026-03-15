package com.supertntmod.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Su TNT: 10 blok yarıçapındaki ateşleri söndürür.
 * Bloklara zarar vermez.
 */
public class WaterTntEntity extends TntEntity {
    private static final int RADIUS = 10;
    private boolean done = false;

    public WaterTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public WaterTntEntity(World world, double x, double y, double z,
                          @Nullable LivingEntity igniter) {
        super(ModEntities.WATER_TNT, world);
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

            int extinguished = 0;

            for (BlockPos pos : BlockPos.iterateOutwards(center, RADIUS, RADIUS, RADIUS)) {
                if (!pos.isWithinDistance(center, RADIUS)) continue;

                BlockState state = world.getBlockState(pos);

                // Ateş bloklarını söndür
                if (state.isOf(Blocks.FIRE) || state.isOf(Blocks.SOUL_FIRE)) {
                    world.setBlockState(pos, Blocks.AIR.getDefaultState());
                    extinguished++;
                }
                // Kamp ateşini söndür
                else if (state.isIn(BlockTags.CAMPFIRES) && state.get(CampfireBlock.LIT)) {
                    world.setBlockState(pos, state.with(CampfireBlock.LIT, false));
                    extinguished++;
                }
            }

            // Su sıçrama efekti
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.SPLASH,
                        center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5,
                        100, 3.0, 2.0, 3.0, 0.1);
            }

            // Söndürme sesi
            world.playSound(null, center.getX(), center.getY(), center.getZ(),
                    SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, SoundCategory.BLOCKS, 2.0f, 1.0f);

            return;
        }
        if (!done) super.tick();
    }
}
