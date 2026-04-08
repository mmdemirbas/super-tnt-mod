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

    public static final Item PINK_LEGO_BRICK = register("pink_lego_brick",
            new Item(new Item.Settings()
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(SuperTntMod.MOD_ID, "pink_lego_brick")))));

    public static final Item GREEN_LEGO_BRICK = register("green_lego_brick",
            new Item(new Item.Settings()
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(SuperTntMod.MOD_ID, "green_lego_brick")))));

    public static final PortalGunItem PORTAL_GUN = register("portal_gun",
            new PortalGunItem(new Item.Settings()
                    .maxCount(1)
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(SuperTntMod.MOD_ID, "portal_gun")))));

    public static final TunnelingItem TUNNELING_ITEM = register("tunneling_item",
            new TunnelingItem(new Item.Settings()
                    .maxCount(1)
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(SuperTntMod.MOD_ID, "tunneling_item")))));

    public static final AmongUsReportItem AMONG_US_REPORT = register("among_us_report",
            new AmongUsReportItem(new Item.Settings()
                    .maxCount(16)
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(SuperTntMod.MOD_ID, "among_us_report")))));

    public static final ShrinkBallItem SHRINK_BALL = register("shrink_ball",
            new ShrinkBallItem(new Item.Settings()
                    .maxCount(16)
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(SuperTntMod.MOD_ID, "shrink_ball")))));

    public static final GrapplingHookItem GRAPPLING_HOOK = register("grappling_hook",
            new GrapplingHookItem(new Item.Settings()
                    .maxCount(1)
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(SuperTntMod.MOD_ID, "grappling_hook")))));

    public static final GrowBallItem GROW_BALL = register("grow_ball",
            new GrowBallItem(new Item.Settings()
                    .maxCount(16)
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(SuperTntMod.MOD_ID, "grow_ball")))));

    public static final ShrinkPotionItem SHRINK_POTION = register("shrink_potion",
            new ShrinkPotionItem(new Item.Settings()
                    .maxCount(16)
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(SuperTntMod.MOD_ID, "shrink_potion")))));

    public static final GrowPotionItem GROW_POTION = register("grow_potion",
            new GrowPotionItem(new Item.Settings()
                    .maxCount(16)
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(SuperTntMod.MOD_ID, "grow_potion")))));

    public static final ScaleLockItem SCALE_LOCK = register("scale_lock",
            new ScaleLockItem(new Item.Settings()
                    .maxCount(1)
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(SuperTntMod.MOD_ID, "scale_lock")))));

    // TNT Zırh seti
    public static final Item TNT_ARMOR_HELMET = register("tnt_armor_helmet",
            new TooltipItem(new Item.Settings()
                    .armor(ModArmorMaterials.TNT_ARMOR, net.minecraft.item.equipment.EquipmentType.HELMET)
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(SuperTntMod.MOD_ID, "tnt_armor_helmet")))));

    public static final Item TNT_ARMOR_CHESTPLATE = register("tnt_armor_chestplate",
            new TooltipItem(new Item.Settings()
                    .armor(ModArmorMaterials.TNT_ARMOR, net.minecraft.item.equipment.EquipmentType.CHESTPLATE)
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(SuperTntMod.MOD_ID, "tnt_armor_chestplate")))));

    public static final Item TNT_ARMOR_LEGGINGS = register("tnt_armor_leggings",
            new TooltipItem(new Item.Settings()
                    .armor(ModArmorMaterials.TNT_ARMOR, net.minecraft.item.equipment.EquipmentType.LEGGINGS)
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(SuperTntMod.MOD_ID, "tnt_armor_leggings")))));

    public static final Item TNT_ARMOR_BOOTS = register("tnt_armor_boots",
            new TooltipItem(new Item.Settings()
                    .armor(ModArmorMaterials.TNT_ARMOR, net.minecraft.item.equipment.EquipmentType.BOOTS)
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(SuperTntMod.MOD_ID, "tnt_armor_boots")))));

    // Acılı Cips - yenildiğinde hız efekti verir
    public static final SpicyChipsItem SPICY_CHIPS = register("spicy_chips",
            new SpicyChipsItem(new Item.Settings()
                    .maxCount(16)
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(SuperTntMod.MOD_ID, "spicy_chips")))));

    // Enerji Kristali - bedrock kırar, altından yeni bedrock çıkar
    public static final EnergyCrystalItem ENERGY_CRYSTAL = register("energy_crystal",
            new EnergyCrystalItem(new Item.Settings()
                    .maxCount(16)
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(SuperTntMod.MOD_ID, "energy_crystal")))));

    // Craft Baltası - iki blok arasına duvar inşa eder
    public static final CraftAxeItem CRAFT_AXE = register("craft_axe",
            new CraftAxeItem(new Item.Settings()
                    .maxCount(1)
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(SuperTntMod.MOD_ID, "craft_axe")))));

    // Çizim Eşyası
    public static final DrawingItem DRAWING_ITEM = register("drawing_item",
            new DrawingItem(new Item.Settings()
                    .maxCount(1)
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(SuperTntMod.MOD_ID, "drawing_item")))));

    private static <T extends Item> T register(String name, T item) {
        Identifier id = Identifier.of(SuperTntMod.MOD_ID, name);
        return Registry.register(Registries.ITEM, id, item);
    }

    public static void register() {
        SuperTntMod.LOGGER.info("16 item kaydedildi.");
    }
}
