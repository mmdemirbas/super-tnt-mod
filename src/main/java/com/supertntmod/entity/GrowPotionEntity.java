package com.supertntmod.entity;

import com.supertntmod.SuperTntMod;
import com.supertntmod.item.ScaleLockItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

/**
 * Büyütme İksiri Entity: çarptığı canlıyı 3.0x boyuta büyütür.
 * Ölçek Kilidi takanlara etki etmez.
 */
public class GrowPotionEntity extends ThrownEntity {

    public GrowPotionEntity(EntityType<? extends ThrownEntity> type, World world) {
        super(type, world);
    }

    public GrowPotionEntity(World world, LivingEntity owner) {
        super(ModEntities.GROW_POTION, owner.getX(), owner.getEyeY() - 0.1, owner.getZ(), world);
        this.setOwner(owner);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        // Ekstra veri gerekmez
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);
        if (this.getEntityWorld().isClient()) return;

        Entity hit = entityHitResult.getEntity();
        if (hit instanceof LivingEntity target) {
            if (!ScaleLockItem.isProtected(target)) {
                applyScale(target);
            }
        }
        playGrowSound();
        this.discard();
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        super.onBlockHit(blockHitResult);
        if (!this.getEntityWorld().isClient()) {
            playGrowSound();
            this.discard();
        }
    }

    private void applyScale(LivingEntity target) {
        EntityAttributeInstance scaleAttr = target.getAttributeInstance(EntityAttributes.SCALE);
        if (scaleAttr == null) return;

        double currentScale = scaleAttr.getValue();
        double newScale = currentScale * 3.0;
        double newModifierValue = newScale - 1.0;

        scaleAttr.removeModifier(SuperTntMod.SCALE_MODIFIER_ID);
        scaleAttr.addPersistentModifier(new EntityAttributeModifier(
                SuperTntMod.SCALE_MODIFIER_ID, newModifierValue,
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    private void playGrowSound() {
        this.getEntityWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_PUFFER_FISH_BLOW_UP, SoundCategory.PLAYERS, 1.5f, 0.5f);
    }
}
