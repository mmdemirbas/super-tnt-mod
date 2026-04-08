package com.supertntmod.item;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.item.consume.UseAction;
import net.minecraft.world.World;

import java.util.function.Consumer;

/**
 * Acılı Cips: 1.6 saniye yer, hız efekti verir (20 saniye, seviye 3).
 */
public class SpicyChipsItem extends Item {

    public SpicyChipsItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context,
                              TooltipDisplayComponent displayComponent,
                              Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
        textConsumer.accept(Text.translatable("item.supertntmod.spicy_chips.tooltip").formatted(Formatting.GRAY));
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        user.setCurrentHand(hand);
        return ActionResult.CONSUME;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 32; // 1.6 saniye yeme animasyonu
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.EAT;
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!world.isClient()) {
            // Hız efekti: 20 saniye, seviye 3 (III)
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 400, 2));
        }
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 0.5f, 1.2f);

        if (user instanceof PlayerEntity player && !player.isCreative()) {
            stack.decrement(1);
        }
        return stack.isEmpty() ? ItemStack.EMPTY : stack;
    }
}
