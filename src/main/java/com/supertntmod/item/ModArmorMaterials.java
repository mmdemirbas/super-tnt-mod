package com.supertntmod.item;

import com.supertntmod.SuperTntMod;
import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.EquipmentAssetKeys;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundEvents;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.util.Identifier;

import java.util.Map;

/**
 * TNT Zırh malzemesi.
 * Demir seviyesinde dayanıklılık, vurana patlama yapma özelliği
 * Java kodunda değil mixin'de uygulanıyor.
 */
public final class ModArmorMaterials {

    public static final RegistryKey<EquipmentAsset> TNT_ARMOR_ASSET_KEY =
            EquipmentAssetKeys.register("tnt");

    public static final ArmorMaterial TNT_ARMOR = new ArmorMaterial(
            20, // durability multiplier (demir = 15, elmas = 33)
            Map.of(
                    EquipmentType.HELMET, 2,
                    EquipmentType.CHESTPLATE, 5,
                    EquipmentType.LEGGINGS, 4,
                    EquipmentType.BOOTS, 2
            ),
            12, // enchantmentValue (demir = 9)
            SoundEvents.ITEM_ARMOR_EQUIP_IRON,
            0.0f, // toughness
            0.0f, // knockbackResistance
            TagKey.of(RegistryKeys.ITEM, Identifier.of(SuperTntMod.MOD_ID, "tnt_armor_repair")),
            TNT_ARMOR_ASSET_KEY
    );

    // Ametist Zırh: Elmas seviyesinde koruma, çıkarılamaz (Enerji Kristali ile gevşetilir)
    public static final RegistryKey<EquipmentAsset> AMETHYST_ARMOR_ASSET_KEY =
            EquipmentAssetKeys.register("amethyst");

    public static final ArmorMaterial AMETHYST_ARMOR = new ArmorMaterial(
            33, // durability multiplier (elmas seviyesi)
            Map.of(
                    EquipmentType.HELMET, 3,
                    EquipmentType.CHESTPLATE, 8,
                    EquipmentType.LEGGINGS, 6,
                    EquipmentType.BOOTS, 3
            ),
            15, // enchantmentValue (elmas = 10)
            SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND,
            2.0f, // toughness (elmas = 2.0)
            0.0f, // knockbackResistance
            TagKey.of(RegistryKeys.ITEM, Identifier.of(SuperTntMod.MOD_ID, "amethyst_armor_repair")),
            AMETHYST_ARMOR_ASSET_KEY
    );

    private ModArmorMaterials() {}
}
