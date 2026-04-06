package com.supertntmod.item;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.Consumer;

/**
 * Tooltip destekli Item.
 * Çeviri anahtarına ".tooltip" ekleyerek otomatik tooltip gösterir.
 */
public class TooltipItem extends Item {

    public TooltipItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context,
                              TooltipDisplayComponent displayComponent,
                              Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
        String key = this.getTranslationKey() + ".tooltip";
        textConsumer.accept(Text.translatable(key).formatted(Formatting.GRAY));
    }
}
