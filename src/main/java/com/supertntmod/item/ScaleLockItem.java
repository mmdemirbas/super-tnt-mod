package com.supertntmod.item;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.Consumer;

/**
 * Ölçek Kilidi: tüm slot'larda taşındığında ölçek değişikliklerine karşı
 * bağışıklık sağlar (Küçülten TNT, Büyüten TNT, iksirler).
 */
public class ScaleLockItem extends Item {

    public ScaleLockItem(Settings settings) {
        super(settings);
    }

    /**
     * Canlının herhangi bir ekipman slot'unda Ölçek Kilidi olup olmadığını kontrol eder.
     * Bu kontrol ölçek uygulayan tüm mekanizmalar tarafından kullanılır.
     */
    public static boolean isProtected(LivingEntity entity) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (entity.getEquippedStack(slot).getItem() instanceof ScaleLockItem) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context,
                              TooltipDisplayComponent displayComponent,
                              Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
        textConsumer.accept(Text.translatable("item.supertntmod.scale_lock.tooltip").formatted(Formatting.GRAY));
    }
}
