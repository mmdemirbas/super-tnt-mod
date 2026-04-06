package com.supertntmod.item;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.function.Consumer;

/**
 * Çizim Eşyası: Sağ tıklayınca 16x16 çizim ekranı açılır.
 * Çizilen şekil yün bloklarından gerçek boyutta inşa edilir.
 */
public class DrawingItem extends Item {

    public DrawingItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context,
                              TooltipDisplayComponent displayComponent,
                              Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
        textConsumer.accept(Text.translatable("item.supertntmod.drawing_item.tooltip")
                .formatted(Formatting.GRAY));
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        // Ekran açma işlemi client tarafında SuperTntModClient'ta ele alınır.
        // Server tarafında bir şey yapmaya gerek yok.
        return ActionResult.SUCCESS;
    }
}
