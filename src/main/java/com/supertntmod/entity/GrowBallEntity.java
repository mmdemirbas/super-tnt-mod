package com.supertntmod.entity;

import com.supertntmod.SuperTntMod;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;

/**
 * Büyütme Topu Entity: yere düşünce sadece atan kişiyi büyütür.
 * Etraftaki diğer canlılara etki etmez.
 */
public class GrowBallEntity extends ThrownEntity {

    public GrowBallEntity(EntityType<? extends ThrownEntity> type, World world) {
        super(type, world);
    }

    public GrowBallEntity(World world, PlayerEntity owner) {
        super(ModEntities.GROW_BALL, owner.getX(), owner.getEyeY() - 0.1, owner.getZ(), world);
        this.setOwner(owner);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        // Ekstra veri gerekmez
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        super.onBlockHit(blockHitResult);
        if (this.getEntityWorld().isClient()) return;

        applyToOwner();
        playGrowSound();
        this.discard();
    }

    private void applyToOwner() {
        if (!(this.getOwner() instanceof LivingEntity owner)) return;

        EntityAttributeInstance scaleAttr = owner.getAttributeInstance(EntityAttributes.SCALE);
        if (scaleAttr == null) return;

        double currentScale = scaleAttr.getValue();
        double newScale = currentScale * 2.0;
        double newModifierValue = newScale - 1.0;

        scaleAttr.removeModifier(SuperTntMod.SCALE_MODIFIER_ID);
        scaleAttr.addPersistentModifier(new EntityAttributeModifier(
                SuperTntMod.SCALE_MODIFIER_ID, newModifierValue,
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE));

        double actualScale = scaleAttr.getValue(); // clamp sonrası gerçek değer
        if (owner instanceof PlayerEntity player) {
            if (Math.abs(actualScale - currentScale) < 0.001) {
                player.sendMessage(
                    Text.literal("En büyük ölçektesin! Sıfırlamak için Temizleyici TNT kullan.").formatted(Formatting.YELLOW),
                    true);
            } else {
                player.sendMessage(
                    Text.literal(String.format("Büyüdün! Ölçek: %.4fx", actualScale)).formatted(Formatting.GREEN),
                    true);
            }
        }
    }

    private void playGrowSound() {
        this.getEntityWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_PUFFER_FISH_BLOW_UP, SoundCategory.PLAYERS, 1.5f, 0.5f);
    }
}
