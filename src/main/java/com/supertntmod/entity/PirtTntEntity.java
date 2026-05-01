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
 * Pırt TNT: Patlatıldığında etrafa pırt kokusu (yeşil bulut) ve sesi saçar.
 * Hasar yok — sadece komik. Yakındaki oyunculara kısa NAUSEA verir.
 */
public class PirtTntEntity extends TntEntity {
    private boolean done = false;
    private static final int RADIUS = 10;

    public PirtTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public PirtTntEntity(World world, double x, double y, double z,
                         @Nullable LivingEntity igniter) {
        super(ModEntities.PIRT_TNT, world);
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

            // Komik pırt sesi — keçi+balık kombinasyonu
            world.playSound(null, x, y, z, SoundEvents.ENTITY_GOAT_SCREAMING_AMBIENT,
                    SoundCategory.BLOCKS, 2.0f, 0.5f);
            world.playSound(null, x, y, z, SoundEvents.ENTITY_PUFFER_FISH_BLOW_OUT,
                    SoundCategory.BLOCKS, 3.0f, 0.4f);

            if (world instanceof ServerWorld serverWorld) {
                // Yeşil pırt bulutu
                serverWorld.spawnParticles(ParticleTypes.SNEEZE,
                        x, y + 1, z, 200, RADIUS, 3.0, RADIUS, 0.05);
                serverWorld.spawnParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                        x, y + 1, z, 100, RADIUS, 3.0, RADIUS, 0.02);
                serverWorld.spawnParticles(ParticleTypes.SMOKE,
                        x, y + 0.5, z, 80, 4.0, 1.0, 4.0, 0.03);
            }

            // Yakındaki oyunculara nausea
            world.getEntitiesByClass(PlayerEntity.class,
                    new Box(x - RADIUS, y - RADIUS, z - RADIUS,
                            x + RADIUS, y + RADIUS, z + RADIUS),
                    e -> true).forEach(p -> {
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 200, 0));
            });

            // Görsel patlama (hasarsız)
            world.createExplosion(null, x, y, z, 0.5f, false, World.ExplosionSourceType.NONE);
            return;
        }
        if (!done) super.tick();
    }
}
