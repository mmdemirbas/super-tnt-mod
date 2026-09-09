package com.supertntmod.item;

import com.supertntmod.SuperTntMod;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

/**
 * Modun büyüleri. Büyünün kendisi veri tabanlı
 * ({@code data/supertntmod/enchantment/tukenmezlik.json}); burada yalnızca
 * koddan tanınması için anahtar ve yardımcılar var.
 *
 * <p><b>Tükenmezlik'in hangi eşyalara basılabileceğinin tek yetkilisi
 * {@link com.supertntmod.mixin.UnendingEnchantmentTargetMixin}'dir.</b> JSON'daki
 * {@code supported_items} bilerek boş bir etikete bakar: "her eşya" diyen bir
 * vanilla etiketi yok, elle sayılan bir liste de her sürümde eskir.
 */
public final class ModEnchantments {

    private ModEnchantments() {}

    /** JSON'daki {@code description.translate} ile birebir aynı olmalı. */
    public static final String TUKENMEZLIK_DESCRIPTION_KEY = "enchantment.supertntmod.tukenmezlik";

    public static final RegistryKey<Enchantment> TUKENMEZLIK = RegistryKey.of(
            RegistryKeys.ENCHANTMENT, Identifier.of(SuperTntMod.MOD_ID, "tukenmezlik"));

    /** Yığın Tükenmezlik ile büyülenmişse true — yani hiç eksilmemeli. */
    public static boolean isUnending(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ItemEnchantmentsComponent enchantments = EnchantmentHelper.getEnchantments(stack);
        if (enchantments.isEmpty()) return false;
        for (RegistryEntry<Enchantment> entry : enchantments.getEnchantments()) {
            if (entry.matchesKey(TUKENMEZLIK)) return true;
        }
        return false;
    }
}
