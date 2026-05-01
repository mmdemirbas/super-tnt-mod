package com.supertntmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Redstone TNT: Çok güçlü patlama + yakındaki oyunculara hız ve güç verir.
 */
public class RedstoneTntEntity extends TntEntity {
    private boolean done = false;
    private static final int BUFF_RADIUS = 20;

    public RedstoneTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public RedstoneTntEntity(World world, double x, double y, double z,
                             @Nullable LivingEntity igniter) {
        super(ModEntities.REDSTONE_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    @Override
    public void tick() {
        if (!done && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            done = true;
            World world = getEntityWorld();
            double x = getX(), y = getY(), z = getZ();
            this.discard();

            world.playSound(null, x, y, z, SoundEvents.ENTITY_GENERIC_EXPLODE,
                    SoundCategory.BLOCKS, 4.0f, 0.6f);
            world.playSound(null, x, y, z, SoundEvents.BLOCK_REDSTONE_TORCH_BURNOUT,
                    SoundCategory.BLOCKS, 3.0f, 0.5f);

            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.LARGE_SMOKE, x, y, z,
                        200, 6.0, 4.0, 6.0, 0.2);
                serverWorld.spawnParticles(ParticleTypes.FLAME, x, y, z,
                        100, 5.0, 3.0, 5.0, 0.1);
            }

            // Vanilla TNT 4.0; bu çok daha güçlü
            world.createExplosion(null, x, y, z, 8.0f, true, World.ExplosionSourceType.TNT);

            // Yakındaki oyunculara hız ve güç (1 dakika)
            world.getEntitiesByClass(PlayerEntity.class,
                    new Box(x - BUFF_RADIUS, y - BUFF_RADIUS, z - BUFF_RADIUS,
                            x + BUFF_RADIUS, y + BUFF_RADIUS, z + BUFF_RADIUS),
                    e -> true).forEach(p -> {
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 1200, 2));
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 1200, 1));
            });
            return;
        }
        if (!done) super.tick();
    }
}
