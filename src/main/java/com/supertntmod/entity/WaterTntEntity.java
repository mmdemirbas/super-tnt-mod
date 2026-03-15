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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Su TNT: 10 blok yarıçapındaki ateşleri söndürür.
 * Yakındaki entity'leri su dalgasıyla iter.
 * Geçici su birikintileri oluşturur.
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

            // Ateş bloklarını söndür
            for (BlockPos pos : BlockPos.iterateOutwards(center, RADIUS, RADIUS, RADIUS)) {
                if (!pos.isWithinDistance(center, RADIUS)) continue;

                BlockState state = world.getBlockState(pos);

                if (state.isOf(Blocks.FIRE) || state.isOf(Blocks.SOUL_FIRE)) {
                    world.setBlockState(pos, Blocks.AIR.getDefaultState());
                } else if (state.isIn(BlockTags.CAMPFIRES) && state.get(CampfireBlock.LIT)) {
                    world.setBlockState(pos, state.with(CampfireBlock.LIT, false));
                }
            }

            // Yakın çevreye su birikintileri (%30 şans, 5 blok yarıçap, sadece yüzeyde)
            for (BlockPos pos : BlockPos.iterateOutwards(center, 5, 5, 5)) {
                if (!pos.isWithinDistance(center, 5)) continue;
                if (world.getBlockState(pos).isOf(Blocks.AIR) &&
                    world.getBlockState(pos.down()).isSolid() &&
                    world.random.nextFloat() < 0.3f) {
                    world.setBlockState(pos, Blocks.WATER.getDefaultState());
                }
            }

            // Yakındaki entity'leri it (su dalgası)
            world.getEntitiesByClass(LivingEntity.class,
                    new net.minecraft.util.math.Box(center).expand(RADIUS),
                    e -> true
            ).forEach(entity -> {
                Vec3d pushDir = entity.getPos().subtract(Vec3d.ofCenter(center)).normalize();
                entity.addVelocity(pushDir.x * 1.5, 0.5, pushDir.z * 1.5);
                entity.velocityModified = true;
                // Ateş söndür
                entity.extinguish();
            });

            // Su sıçrama efekti (güçlendirilmiş)
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.SPLASH,
                        center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5,
                        300, 5.0, 2.0, 5.0, 0.2);
                serverWorld.spawnParticles(ParticleTypes.FALLING_WATER,
                        center.getX() + 0.5, center.getY() + 5, center.getZ() + 0.5,
                        100, 4.0, 1.0, 4.0, 0.0);
                serverWorld.spawnParticles(ParticleTypes.BUBBLE,
                        center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5,
                        50, 3.0, 1.0, 3.0, 0.1);
            }

            // Söndürme sesi + dalga sesi
            world.playSound(null, center.getX(), center.getY(), center.getZ(),
                    SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, SoundCategory.BLOCKS, 2.0f, 1.0f);
            world.playSound(null, center.getX(), center.getY(), center.getZ(),
                    SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.BLOCKS, 2.0f, 0.8f);

            return;
        }
        if (!done) super.tick();
    }
}
