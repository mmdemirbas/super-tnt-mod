package com.supertntmod.entity;

import com.supertntmod.SuperTntMod;
import com.supertntmod.item.ScaleLockItem;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Büyüten TNT: 15 blok yarıçapındaki tüm canlıları büyütür.
 * Scale attribute modifier kullanarak 3x boyuta çıkarır.
 */
public class GrowthTntEntity extends TntEntity {
    private static final int RADIUS = 15;

    private boolean done = false;

    public GrowthTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public GrowthTntEntity(World world, double x, double y, double z,
                           @Nullable LivingEntity igniter) {
        super(ModEntities.GROWTH_TNT, world);
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

            // Büyüme sesi
            world.playSound(null, center.getX(), center.getY(), center.getZ(),
                    SoundEvents.ENTITY_PUFFER_FISH_BLOW_UP, SoundCategory.BLOCKS, 2.0f, 0.5f);
            world.playSound(null, center.getX(), center.getY(), center.getZ(),
                    SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.BLOCKS, 1.0f, 0.8f);

            // Yakındaki tüm canlıları büyüt (Ölçek Kilidi takanlar hariç)
            world.getEntitiesByClass(LivingEntity.class,
                    new net.minecraft.util.math.Box(center).expand(RADIUS),
                    e -> true
            ).forEach(entity -> {
                if (ScaleLockItem.isProtected(entity)) return;
                EntityAttributeInstance scaleAttr = entity.getAttributeInstance(EntityAttributes.SCALE);
                if (scaleAttr != null) {
                    // Mevcut ölçeği oku ve 2.0 ile çarp (kümülatif büyütme)
                    double currentScale = scaleAttr.getValue();
                    double newScale = currentScale * 2.0;
                    double newModifierValue = newScale - 1.0;

                    scaleAttr.removeModifier(SuperTntMod.SCALE_MODIFIER_ID);
                    scaleAttr.addPersistentModifier(new EntityAttributeModifier(
                            SuperTntMod.SCALE_MODIFIER_ID, newModifierValue,
                            EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                }
            });

            // Büyüme partikülleri (dışa doğru yayılan efekt)
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.EXPLOSION_EMITTER,
                        center.getX() + 0.5, center.getY() + 2, center.getZ() + 0.5,
                        5, 2.0, 2.0, 2.0, 0.0);
                serverWorld.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING,
                        center.getX() + 0.5, center.getY() + 3, center.getZ() + 0.5,
                        200, 5.0, 4.0, 5.0, 0.5);
            }

            // Görsel patlama
            world.createExplosion(null, center.getX() + 0.5, center.getY(),
                    center.getZ() + 0.5, 0.0f, false, World.ExplosionSourceType.NONE);
            return;
        }
        if (!done) super.tick();
    }
}
