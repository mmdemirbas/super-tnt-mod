package com.supertntmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ZehirTntEntity extends TntEntity {
    private static final int RADIUS = 20;
    private boolean done = false;

    public ZehirTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public ZehirTntEntity(World world, double x, double y, double z,
                          @Nullable LivingEntity igniter) {
        super(ModEntities.ZEHIR_TNT, world);
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
                    SoundEvents.ENTITY_WITHER_AMBIENT, SoundCategory.BLOCKS, 2.0f, 0.8f);

            world.getEntitiesByClass(LivingEntity.class,
                    new net.minecraft.util.math.Box(center).expand(RADIUS),
                    e -> true
            ).forEach(entity -> {
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 600, 2));
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 600, 1));
            });

            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.ITEM_SLIME,
                        cx, cy + 1, cz, 150, 6.0, 3.0, 6.0, 0.2);
                serverWorld.spawnParticles(ParticleTypes.MYCELIUM,
                        cx, cy + 1, cz, 100, 6.0, 3.0, 6.0, 0.1);
            }

            world.createExplosion(null, cx, cy, cz, 0.0f, false, World.ExplosionSourceType.NONE);
            return;
        }
        if (!done) super.tick();
    }
}
