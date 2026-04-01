package com.supertntmod.item;

import com.supertntmod.entity.GrowPotionEntity;
import net.minecraft.component.type.TooltipDisplayComponent;
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
import net.minecraft.world.World;

import java.util.function.Consumer;

/**
 * Büyütme İksiri: Atıldığında çarptığı canlıyı büyütür.
 * Ölçek Kilidi takanları etkilemez.
 */
public class GrowPotionItem extends Item {

    public GrowPotionItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context,
                              TooltipDisplayComponent displayComponent,
                              Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
        textConsumer.accept(Text.translatable("item.supertntmod.grow_potion.tooltip").formatted(Formatting.GRAY));
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient()) {
            GrowPotionEntity potion = new GrowPotionEntity(world, user);
            potion.setVelocity(user, user.getPitch(), user.getYaw(), 0.0f, 1.5f, 0.0f);
            world.spawnEntity(potion);
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENTITY_SPLASH_POTION_THROW, SoundCategory.PLAYERS, 1.0f, 0.8f);

            if (!user.isCreative()) {
                stack.decrement(1);
            }
        }

        return ActionResult.SUCCESS;
    }
}
