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

    private ModArmorMaterials() {}
}
