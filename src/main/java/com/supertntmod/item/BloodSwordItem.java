package com.supertntmod.item;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.Consumer;

/**
 * Kanlı Kılıç: vurulan canlının üzerinden kırmızı sıvı (kan) partikülü
 * çıkar. Görsel etki için DUST partikülü kırmızı renkle kullanılır.
 */
public class BloodSwordItem extends Item {

    public BloodSwordItem(Settings settings) {
        super(settings);
    }

    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.postHit(stack, target, attacker);
        if (target.getEntityWorld() instanceof ServerWorld sw) {
            spawnBlood(sw, target);
        }
    }

    private void spawnBlood(ServerWorld world, Entity target) {
        double x = target.getX();
        double y = target.getY() + target.getHeight() * 0.6;
        double z = target.getZ();
        // Kırmızı dust partikülü (RGB: kıpkırmızı)
        DustParticleEffect blood = new DustParticleEffect(0xCC0000, 1.5f);
        world.spawnParticles(blood, x, y, z, 60, 0.4, 0.5, 0.4, 0.05);
        // Damla efekti
        world.spawnParticles(ParticleTypes.FALLING_OBSIDIAN_TEAR,
                x, y, z, 30, 0.3, 0.3, 0.3, 0.0);
        // Splash sesi (bloody)
        world.playSound(null, x, y, z,
                SoundEvents.ENTITY_GENERIC_SPLASH,
                SoundCategory.PLAYERS, 0.6f, 0.7f);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context,
                              TooltipDisplayComponent displayComponent,
                              Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
        textConsumer.accept(Text.translatable("item.supertntmod.blood_sword.tooltip").formatted(Formatting.DARK_RED));
    }
}
