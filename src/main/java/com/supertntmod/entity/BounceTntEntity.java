package com.supertntmod.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Zıplatan TNT entity: patlayınca 12 blok yarıçapındaki tüm entity'leri
 * yukarı fırlatır. Blok hasarı yoktur.
 */
public class BounceTntEntity extends TntEntity {

    private static final int RADIUS = 12;
    private static final double LAUNCH_FORCE = 1.5;
    private boolean done = false;

    public BounceTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public BounceTntEntity(World world, double x, double y, double z,
                           @Nullable LivingEntity igniter) {
        super(ModEntities.BOUNCE_TNT, world);
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

            // Zıplama sesi
            world.playSound(null, center.getX(), center.getY(), center.getZ(),
                    SoundEvents.ENTITY_SLIME_JUMP, SoundCategory.BLOCKS, 2.0f, 0.5f);
            world.playSound(null, center.getX(), center.getY(), center.getZ(),
                    SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 1.5f, 1.5f);

            // Tüm entity'leri yukarı fırlat
            world.getEntitiesByClass(Entity.class,
                    new Box(center).expand(RADIUS),
                    e -> e.isAlive() && !(e instanceof TntEntity)
            ).forEach(entity -> {
                entity.addVelocity(0, LAUNCH_FORCE, 0);
                entity.velocityDirty = true;
            });

            // Yukarı doğru partiküller
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING,
                        center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5,
                        150, 4.0, 1.0, 4.0, 0.8);
                serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                        center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5,
                        40, 5.0, 1.0, 5.0, 0.3);
            }

            // Görsel patlama (blok hasarsız)
            world.createExplosion(null,
                    center.getX() + 0.5, center.getY(), center.getZ() + 0.5,
                    0.0f, false, World.ExplosionSourceType.NONE);
            return;
        }
        if (!done) super.tick();
    }
}
