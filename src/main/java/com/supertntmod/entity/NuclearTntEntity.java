package com.supertntmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * ☢ Nükleer TNT
 * Oyundaki en büyük patlama + 25 blok yarıçapında
 * radyasyon (zehir + yavaşlama + güçsüzlük) efekti.
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

            // Dev patlama (15x güç)
            world.createExplosion(null, x, y, z, 15.0f, true,
                    World.ExplosionSourceType.TNT);

            // 25 blok yarıçapında radyasyon efekti
            world.getEntitiesByClass(LivingEntity.class,
                    this.getBoundingBox().expand(25),
                    e -> true
            ).forEach(entity -> {
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON,   200, 2));
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 200, 1));
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 200, 1));
            });
            return;
        }
        if (!done) super.tick();
    }
}
