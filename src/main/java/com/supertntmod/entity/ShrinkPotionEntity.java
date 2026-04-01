package com.supertntmod.entity;

import com.supertntmod.item.ModItems;
import com.supertntmod.item.ScaleLockItem;
import com.supertntmod.item.TunnelingItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

/**
 * Küçültme İksiri Entity: çarptığı canlıyı 0.3x boyuta küçültür.
 * Ölçek Kilidi takanlara etki etmez.
 */
public class ShrinkPotionEntity extends ThrownEntity {

    public ShrinkPotionEntity(EntityType<? extends ThrownEntity> type, World world) {
        super(type, world);
    }

    public ShrinkPotionEntity(World world, LivingEntity owner) {
        super(ModEntities.SHRINK_POTION, owner.getX(), owner.getEyeY() - 0.1, owner.getZ(), world);
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
        playShrinkSound();
        this.discard();
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        super.onBlockHit(blockHitResult);
        if (!this.getEntityWorld().isClient()) {
            playShrinkSound();
            this.discard();
        }
    }

    private void applyScale(LivingEntity target) {
        EntityAttributeInstance scaleAttr = target.getAttributeInstance(EntityAttributes.SCALE);
        if (scaleAttr == null) return;

        double currentScale = scaleAttr.getValue();
        double newScale = currentScale * 0.3;
        double newModifierValue = newScale - 1.0;

        scaleAttr.removeModifier(ShrinkTntEntity.SCALE_MODIFIER_ID);
        scaleAttr.addPersistentModifier(new EntityAttributeModifier(
                ShrinkTntEntity.SCALE_MODIFIER_ID, newModifierValue,
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE));

        // 1/12 ölçeğe ulaşan oyunculara tünelleme aleti ver
        if (target instanceof PlayerEntity player && newScale <= TunnelingItem.SCALE_THRESHOLD) {
            if (!player.getInventory().contains(new ItemStack(ModItems.TUNNELING_ITEM))) {
                player.getInventory().insertStack(new ItemStack(ModItems.TUNNELING_ITEM));
                player.sendMessage(Text.translatable("item.supertntmod.tunneling_item.granted"), false);
            }
        }
    }

    private void playShrinkSound() {
        this.getEntityWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_PUFFER_FISH_BLOW_OUT, SoundCategory.PLAYERS, 1.5f, 2.0f);
    }
}
