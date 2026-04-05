package com.supertntmod.entity;

import com.supertntmod.SuperTntMod;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/**
 * Temizleyici TNT: 20 blok yarıçapındaki tüm canlıların
 * iksir efektlerini ve boyut değişikliklerini temizler.
 * Herkesin eski haline dönmesini sağlar.
 */
public class CleanseTntEntity extends TntEntity {
    private static final int RADIUS = 20;
    private boolean done = false;

    public CleanseTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public CleanseTntEntity(World world, double x, double y, double z,
                            @Nullable LivingEntity igniter) {
        super(ModEntities.CLEANSE_TNT, world);
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

            // İyileştirme sesi
            world.playSound(null, center.getX(), center.getY(), center.getZ(),
                    SoundEvents.ITEM_TOTEM_USE, SoundCategory.BLOCKS, 2.0f, 1.2f);

            // Yakındaki tüm canlıları temizle
            world.getEntitiesByClass(LivingEntity.class,
                    new net.minecraft.util.math.Box(center).expand(RADIUS),
                    e -> true
            ).forEach(entity -> {
                // Tüm status efektlerini kaldır
                var effects = new ArrayList<>(entity.getStatusEffects());
                for (StatusEffectInstance effect : effects) {
                    entity.removeStatusEffect(effect.getEffectType());
                }

                // Boyut modifier'ını kaldır (küçültme/büyütme ortak ID)
                EntityAttributeInstance scaleAttr = entity.getAttributeInstance(EntityAttributes.SCALE);
                if (scaleAttr != null) {
                    scaleAttr.removeModifier(SuperTntMod.SCALE_MODIFIER_ID);
                }

                // Donma efektini sıfırla
                entity.setFrozenTicks(0);
            });

            // İyileştirme partikülleri (altın ışıltı)
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING,
                        center.getX() + 0.5, center.getY() + 3, center.getZ() + 0.5,
                        300, 8.0, 5.0, 8.0, 0.5);
                serverWorld.spawnParticles(ParticleTypes.HEART,
                        center.getX() + 0.5, center.getY() + 2, center.getZ() + 0.5,
                        30, 5.0, 3.0, 5.0, 0.1);
                serverWorld.spawnParticles(ParticleTypes.COMPOSTER,
                        center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5,
                        100, 6.0, 3.0, 6.0, 0.1);
            }

            // Görsel patlama
            world.createExplosion(null, center.getX() + 0.5, center.getY(),
                    center.getZ() + 0.5, 0.0f, false, World.ExplosionSourceType.NONE);
            return;
        }
        if (!done) super.tick();
    }
}
