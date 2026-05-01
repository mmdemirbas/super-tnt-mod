package com.supertntmod.item;

import com.supertntmod.entity.KokuBombasiEntity;
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
 * 🥚 Koku Bombası — atıldığında zehir toprağı saçar, değen anında ölür.
 */
public class KokuBombasiItem extends Item {

    public KokuBombasiItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context,
                              TooltipDisplayComponent displayComponent,
                              Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
        textConsumer.accept(Text.translatable("item.supertntmod.koku_bombasi.tooltip").formatted(Formatting.GRAY));
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient()) {
            KokuBombasiEntity bomb = new KokuBombasiEntity(world, user);
            bomb.setVelocity(user, user.getPitch(), user.getYaw(), 0.0f, 1.4f, 0.0f);
            world.spawnEntity(bomb);
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENTITY_EGG_THROW, SoundCategory.PLAYERS, 1.0f, 0.6f);

            if (!user.isCreative()) {
                stack.decrement(1);
            }
        }

        return ActionResult.SUCCESS;
    }
}
