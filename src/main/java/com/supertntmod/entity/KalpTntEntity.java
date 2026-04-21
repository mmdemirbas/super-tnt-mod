package com.supertntmod.entity;

import com.supertntmod.SuperTntMod;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class KalpTntEntity extends TntEntity {
    private static final int RADIUS = 20;
    private boolean done = false;

    public KalpTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public KalpTntEntity(World world, double x, double y, double z,
                         @Nullable LivingEntity igniter) {
        super(ModEntities.KALP_TNT, world);
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

            world.playSound(null, center.getX(), center.getY(), center.getZ(),
                    SoundEvents.ITEM_TOTEM_USE, SoundCategory.BLOCKS, 2.0f, 1.0f);

            world.getEntitiesByClass(LivingEntity.class,
                    new net.minecraft.util.math.Box(center).expand(RADIUS),
                    e -> true
            ).forEach(entity -> {
                var effects = new ArrayList<>(entity.getStatusEffects());
                for (StatusEffectInstance effect : effects) {
                    entity.removeStatusEffect(effect.getEffectType());
                }
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 600, 2));
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 600, 4));

                EntityAttributeInstance scaleAttr = entity.getAttributeInstance(EntityAttributes.SCALE);
                if (scaleAttr != null) {
                    scaleAttr.removeModifier(SuperTntMod.SCALE_MODIFIER_ID);
                }
                entity.setFrozenTicks(0);
            });

            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.HEART,
                        center.getX() + 0.5, center.getY() + 2, center.getZ() + 0.5,
                        60, 6.0, 4.0, 6.0, 0.2);
                serverWorld.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING,
                        center.getX() + 0.5, center.getY() + 2, center.getZ() + 0.5,
                        150, 5.0, 4.0, 5.0, 0.4);
            }

            world.createExplosion(null, center.getX() + 0.5, center.getY(),
                    center.getZ() + 0.5, 0.0f, false, World.ExplosionSourceType.NONE);
            return;
        }
        if (!done) super.tick();
    }
}
