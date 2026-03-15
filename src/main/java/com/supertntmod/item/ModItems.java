package com.supertntmod.item;

import com.supertntmod.SuperTntMod;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModItems {

    public static final TntFrisbeeItem TNT_FRISBEE = register("tnt_frisbee",
            new TntFrisbeeItem(new Item.Settings()
                    .maxCount(16)
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(SuperTntMod.MOD_ID, "tnt_frisbee")))));

    private static <T extends Item> T register(String name, T item) {
        Identifier id = Identifier.of(SuperTntMod.MOD_ID, name);
        return Registry.register(Registries.ITEM, id, item);
    }

    public static void register() {
        SuperTntMod.LOGGER.info("TNT Frizbi itemi kaydedildi.");
    }
}
