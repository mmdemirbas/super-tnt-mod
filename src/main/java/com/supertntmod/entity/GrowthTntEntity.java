package com.supertntmod.entity;

import com.supertntmod.SuperTntMod;
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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Büyüten TNT: 15 blok yarıçapındaki tüm canlıları büyütür.
 * Scale attribute modifier kullanarak 3x boyuta çıkarır.
 */
public class GrowthTntEntity extends TntEntity {
    private static final int RADIUS = 15;
    public static final Identifier GROWTH_MODIFIER_ID = Identifier.of(SuperTntMod.MOD_ID, "growth");
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

            // Yakındaki tüm canlıları büyüt
            world.getEntitiesByClass(LivingEntity.class,
                    new net.minecraft.util.math.Box(center).expand(RADIUS),
                    e -> true
            ).forEach(entity -> {
                EntityAttributeInstance scaleAttr = entity.getAttributeInstance(EntityAttributes.SCALE);
                if (scaleAttr != null) {
                    // Önceki büyütme/küçültme modifier'ını kaldır
                    scaleAttr.removeModifier(GROWTH_MODIFIER_ID);
                    scaleAttr.removeModifier(ShrinkTntEntity.SHRINK_MODIFIER_ID);
                    // Büyütme uygula: base * 2.0 = 3x boyut
                    scaleAttr.addPersistentModifier(new EntityAttributeModifier(
                            GROWTH_MODIFIER_ID, 2.0,
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
