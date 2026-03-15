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
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * ☢ Nükleer TNT
 * Oyundaki en büyük patlama + 25 blok yarıçapında
 * radyasyon (zehir + yavaşlama + güçsüzlük + körlük + bulantı) efekti.
 * Fitil yanarken geiger sayacı sesi çalar.
 */
public class NuclearTntEntity extends TntEntity {
    private boolean done = false;

    public NuclearTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(100); // Biraz daha uzun fitil
    }

    public NuclearTntEntity(World world, double x, double y, double z,
                            @Nullable LivingEntity igniter) {
        super(ModEntities.NUCLEAR_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(100);
    }

    @Override
    public void tick() {
        if (!done && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            done = true;
            double x = getX(), y = getY(), z = getZ();
            World world = getEntityWorld();
            this.discard();

            // Nükleer patlama öncesi wither sesi
            world.playSound(null, x, y, z,
                    SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.BLOCKS, 2.0f, 0.5f);

            // Dev patlama (15x güç)
            world.createExplosion(null, x, y, z, 15.0f, true,
                    World.ExplosionSourceType.TNT);

            // Radyasyon partikülleri (mantar bulutu)
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        x, y + 5, z, 300, 5.0, 8.0, 5.0, 0.02);
                serverWorld.spawnParticles(ParticleTypes.LARGE_SMOKE,
                        x, y + 2, z, 200, 6.0, 8.0, 6.0, 0.02);
                serverWorld.spawnParticles(ParticleTypes.WITCH,
                        x, y + 1, z, 200, 8.0, 3.0, 8.0, 0.05);
            }

            // 25 blok yarıçapında radyasyon efekti (güçlendirilmiş)
            world.getEntitiesByClass(LivingEntity.class,
                    this.getBoundingBox().expand(25),
                    e -> true
            ).forEach(entity -> {
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON,   600, 3));  // 30 sn, seviye 4
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 600, 2));  // 30 sn
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 600, 2));  // 30 sn
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 200, 0)); // 10 sn körlük
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA,   300, 0));  // 15 sn bulantı
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER,   200, 1));  // 10 sn wither
            });
            return;
        }
        // Fitil yanarken tik-tak sesi (her 10 tick'te)
        if (!done && !this.getEntityWorld().isClient() && this.getFuse() % 10 == 0) {
            this.getEntityWorld().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), SoundCategory.BLOCKS, 1.5f, 2.0f);
        }
        if (!done) super.tick();
    }
}
