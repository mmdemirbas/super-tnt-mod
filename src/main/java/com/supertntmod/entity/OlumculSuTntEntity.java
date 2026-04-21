package com.supertntmod.entity;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class OlumculSuTntEntity extends TntEntity {
    private static final int RADIUS = 15;
    private boolean done = false;

    public OlumculSuTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public OlumculSuTntEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        super(ModEntities.OLUMCUL_SU_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    @Override
    public void tick() {
        if (!done && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            done = true;
            BlockPos center = this.getBlockPos();
            double cx = center.getX() + 0.5, cy = center.getY(), cz = center.getZ() + 0.5;
            World world = getEntityWorld();
            this.discard();

            world.playSound(null, cx, cy, cz,
                    SoundEvents.BLOCK_WATER_AMBIENT, SoundCategory.BLOCKS, 3.0f, 0.8f);

            for (BlockPos pos : BlockPos.iterateOutwards(center, RADIUS, RADIUS, RADIUS)) {
                if (!pos.isWithinDistance(center, RADIUS)) continue;
                if (world.getBlockState(pos).isAir()) {
                    world.setBlockState(pos, Blocks.WATER.getDefaultState());
                }
            }

            if (world instanceof ServerWorld sw2) {
                DamageSource src = world.getDamageSources().drown();
                world.getEntitiesByClass(LivingEntity.class,
                        new net.minecraft.util.math.Box(center).expand(RADIUS),
                        e -> true
                ).forEach(entity -> entity.damage(sw2, src, 10.0f));
            }

            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.SPLASH,
                        cx, cy + 1, cz, 300, 6.0, 3.0, 6.0, 0.3);
                serverWorld.spawnParticles(ParticleTypes.BUBBLE,
                        cx, cy + 1, cz, 100, 5.0, 2.0, 5.0, 0.2);
            }

            world.createExplosion(null, cx, cy, cz, 0.0f, false, World.ExplosionSourceType.NONE);
            return;
        }
        if (!done) super.tick();
    }
}
