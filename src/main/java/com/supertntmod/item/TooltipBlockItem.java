package com.supertntmod.item;

import net.minecraft.block.Block;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.Consumer;

/**
 * Tooltip destekli BlockItem.
 * Bloğun çeviri anahtarına ".tooltip" ve ".tooltip2" ekleyerek
 * otomatik tooltip gösterir.
 */
public class TooltipBlockItem extends BlockItem {

    public TooltipBlockItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context,
                              TooltipDisplayComponent displayComponent,
                              Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);

        // Ana tooltip satırı
        String key = this.getBlock().getTranslationKey() + ".tooltip";
        textConsumer.accept(Text.translatable(key).formatted(Formatting.GRAY));

        // İkinci satır (varsa)
        String key2 = this.getBlock().getTranslationKey() + ".tooltip2";
        net.minecraft.text.MutableText text2 = Text.translatable(key2);
        // Eğer çeviri mevcutsa (anahtar ile aynı değilse) göster
        if (!text2.getString().equals(key2)) {
            textConsumer.accept(text2.formatted(Formatting.RED));
        }
    }
}
