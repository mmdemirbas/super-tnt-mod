package com.supertntmod.block;

import com.supertntmod.SuperTntMod;
import com.supertntmod.item.TooltipBlockItem;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModBlocks {

    // Normal TNT sertliği
    private static AbstractBlock.Settings tntSettings(String name) {
        return AbstractBlock.Settings.copy(Blocks.TNT)
                .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(SuperTntMod.MOD_ID, name)));
    }

    // Güçlü TNT: daha sert, kırılması daha uzun sürer
    private static AbstractBlock.Settings hardTntSettings(String name) {
        return AbstractBlock.Settings.copy(Blocks.TNT)
                .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(SuperTntMod.MOD_ID, name)))
                .hardness(2.0f).resistance(0.0f);
    }

    // Çok güçlü TNT: en sert
    private static AbstractBlock.Settings veryHardTntSettings(String name) {
        return AbstractBlock.Settings.copy(Blocks.TNT)
                .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(SuperTntMod.MOD_ID, name)))
                .hardness(4.0f).resistance(0.0f);
    }

    // 💎 Elmas TNT - Dev patlama
    public static final DiamondTntBlock DIAMOND_TNT = reg("diamond_tnt",
            new DiamondTntBlock(hardTntSettings("diamond_tnt")));

    // 🥇 Altın TNT - 5 küçük patlama dalgası
    public static final GoldTntBlock GOLD_TNT = reg("gold_tnt",
            new GoldTntBlock(tntSettings("gold_tnt")));

    // 🪨 Bedrock TNT - Bedrock dahil her şeyi kırar
    public static final BedrockTntBlock BEDROCK_TNT = reg("bedrock_tnt",
            new BedrockTntBlock(hardTntSettings("bedrock_tnt")));

    // 💚 Zümrüt TNT - Hazine yağmuru
    public static final EmeraldTntBlock EMERALD_TNT = reg("emerald_tnt",
            new EmeraldTntBlock(tntSettings("emerald_tnt")));

    // ⚡ Yıldırım TNT - 20 yıldırım fırtınası
    public static final LightningTntBlock LIGHTNING_TNT = reg("lightning_tnt",
            new LightningTntBlock(hardTntSettings("lightning_tnt")));

    // ☢ Nükleer TNT - Dev patlama + radyasyon
    public static final NuclearTntBlock NUCLEAR_TNT = reg("nuclear_tnt",
            new NuclearTntBlock(veryHardTntSettings("nuclear_tnt")));

    // ❄ Dondurucu TNT - Etrafı buzla kaplar
    public static final FreezeTntBlock FREEZE_TNT = reg("freeze_tnt",
            new FreezeTntBlock(tntSettings("freeze_tnt")));

    // 🌲 Odun TNT - Ağaçları geçici olarak yok eder
    public static final WoodTntBlock WOOD_TNT = reg("wood_tnt",
            new WoodTntBlock(tntSettings("wood_tnt")));

    // 🎮 Komut Bloğu TNT - Seçilen blok türünü patlatır
    public static final CommandTntBlock COMMAND_TNT = reg("command_tnt",
            new CommandTntBlock(hardTntSettings("command_tnt")));

    // 🚪 TNT Kapı - Başkalarını patlatır
    public static final TntDoorBlock TNT_DOOR = regDoor("tnt_door",
            new TntDoorBlock(AbstractBlock.Settings.copy(Blocks.IRON_DOOR)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(SuperTntMod.MOD_ID, "tnt_door")))));

    // 🔒 Şifreli TNT Sandık - Şifreli, başkalarını patlatır
    public static final EncryptedTntChestBlock ENCRYPTED_TNT_CHEST = reg("encrypted_tnt_chest",
            new EncryptedTntChestBlock(AbstractBlock.Settings.copy(Blocks.CHEST)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(SuperTntMod.MOD_ID, "encrypted_tnt_chest")))));

    // 🚶 Yürüyen TNT Blok - Göz göze gelince yaklaşır ve patlar
    public static final WalkingTntBlock WALKING_TNT = reg("walking_tnt",
            new WalkingTntBlock(tntSettings("walking_tnt")));

    // 🧊 Süper TNT (Mob Dondurma) - Tüm düşmanları dondurur
    public static final MobFreezeTntBlock MOB_FREEZE_TNT = reg("mob_freeze_tnt",
            new MobFreezeTntBlock(tntSettings("mob_freeze_tnt")));

    // 💧 Su TNT - Ateşleri söndürür
    public static final WaterTntBlock WATER_TNT = reg("water_tnt",
            new WaterTntBlock(tntSettings("water_tnt")));

    // 🌈 Rainbow Dinamit - Blokları renkli yüne dönüştürür
    public static final RainbowTntBlock RAINBOW_TNT = reg("rainbow_tnt",
            new RainbowTntBlock(tntSettings("rainbow_tnt")));

    // 🧱 Lego TNT - Lego yapıları inşa eder
    public static final LegoTntBlock LEGO_TNT = reg("lego_tnt",
            new LegoTntBlock(tntSettings("lego_tnt")));

    // 🍝 Makarna TNT - Blokları yenilebilen bloklara dönüştürür
    public static final MakarnaTntBlock MAKARNA_TNT = reg("makarna_tnt",
            new MakarnaTntBlock(tntSettings("makarna_tnt")));

    // 🍬 Şeker TNT - Blokları çikolata ve şekere dönüştürür
    public static final SekerTntBlock SEKER_TNT = reg("seker_tnt",
            new SekerTntBlock(tntSettings("seker_tnt")));

    // 🔽 Küçülten TNT - Canlıları küçültür
    public static final ShrinkTntBlock SHRINK_TNT = reg("shrink_tnt",
            new ShrinkTntBlock(tntSettings("shrink_tnt")));

    // 🔼 Büyüten TNT - Canlıları büyütür
    public static final GrowthTntBlock GROWTH_TNT = reg("growth_tnt",
            new GrowthTntBlock(tntSettings("growth_tnt")));

    // ✨ Temizleyici TNT - Efektleri ve boyut değişikliklerini temizler
    public static final CleanseTntBlock CLEANSE_TNT = reg("cleanse_tnt",
            new CleanseTntBlock(tntSettings("cleanse_tnt")));

    // 🧱 Lego Tuğla - Lego TNT tarafından oluşturulan dekoratif blok (16 renk)
    public static final LegoBrickBlock LEGO_BRICK = regBlockOnly("lego_brick",
            new LegoBrickBlock(AbstractBlock.Settings.copy(Blocks.STONE)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(SuperTntMod.MOD_ID, "lego_brick")))));


    // Sadece blok kaydı (item olmadan) - entity tarafından oluşturulan bloklar için
    private static <T extends Block> T regBlockOnly(String name, T block) {
        Identifier id = Identifier.of(SuperTntMod.MOD_ID, name);
        Registry.register(Registries.BLOCK, id, block);
        return block;
    }

    private static <T extends Block> T reg(String name, T block) {
        Identifier id = Identifier.of(SuperTntMod.MOD_ID, name);
        Registry.register(Registries.BLOCK, id, block);
        Registry.register(Registries.ITEM, id,
                new TooltipBlockItem(block, new Item.Settings()
                        .registryKey(RegistryKey.of(RegistryKeys.ITEM, id))));
        return block;
    }

    private static <T extends Block> T regDoor(String name, T block) {
        Identifier id = Identifier.of(SuperTntMod.MOD_ID, name);
        Registry.register(Registries.BLOCK, id, block);
        Registry.register(Registries.ITEM, id,
                new TooltipBlockItem(block, new Item.Settings()
                        .registryKey(RegistryKey.of(RegistryKeys.ITEM, id))));
        return block;
    }

    public static void register() {
        SuperTntMod.LOGGER.info("22 blok kaydedildi.");
    }
}
