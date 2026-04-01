package com.supertntmod.item;

import com.supertntmod.entity.GrapplingHookEntity;
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
 * Kanca: atıldığında bir bloğa saplanır ve sahibini o noktaya fırlatır.
 * Tekrar kullanılabilir (tüketilmez).
 */
public class GrapplingHookItem extends Item {

    public GrapplingHookItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context,
                              TooltipDisplayComponent displayComponent,
                              Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
        textConsumer.accept(Text.translatable("item.supertntmod.grappling_hook.tooltip").formatted(Formatting.GRAY));
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient()) {
            GrapplingHookEntity hook = new GrapplingHookEntity(world, user);
            hook.setVelocity(user, user.getPitch(), user.getYaw(), 0.0f, 2.5f, 0.0f);
            world.spawnEntity(hook);
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENTITY_FISHING_BOBBER_THROW, SoundCategory.PLAYERS, 0.8f, 1.4f);
        }
        // Tüketilmez — item sayısı azalmaz
        return ActionResult.SUCCESS;
    }
}
