package com.supertntmod.block;

import com.supertntmod.SuperTntMod;
import com.supertntmod.item.TooltipBlockItem;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
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

    // 🟡 Zıplatan TNT - Yakındaki her şeyi havaya uçurur
    public static final BounceTntBlock BOUNCE_TNT = reg("bounce_tnt",
            new BounceTntBlock(tntSettings("bounce_tnt")));

    // 💣 Yakınlık Mayını - Biri yaklaşınca patlar
    public static final ProximityMineBlock PROXIMITY_MINE = reg("proximity_mine",
            new ProximityMineBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)
                    .hardness(0.5f)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(SuperTntMod.MOD_ID, "proximity_mine")))));

    // 🧲 Mıknatıs TNT - 3 sn çeker sonra patlar
    public static final MagnetTntBlock MAGNET_TNT = reg("magnet_tnt",
            new MagnetTntBlock(hardTntSettings("magnet_tnt")));

    // 🌌 Yerçekimi TNT - Yakındaki canlıların yerçekimini 10 sn ters çevirir
    public static final GravityTntBlock GRAVITY_TNT = reg("gravity_tnt",
            new GravityTntBlock(tntSettings("gravity_tnt")));

    // 🪨 Görünmez TNT - Taş gibi görünür, ateşlenince patlar
    public static final InvisibleTntBlock INVISIBLE_TNT = reg("invisible_tnt",
            new InvisibleTntBlock(tntSettings("invisible_tnt")));

    // 🔀 Takas TNT - Yakındaki canlıların konumlarını karıştırır
    public static final SwapTntBlock SWAP_TNT = reg("swap_tnt",
            new SwapTntBlock(tntSettings("swap_tnt")));

    // 🎂 Sahte TNT - Pasta görünümlü, yemeye çalışan patlar!
    public static final FakeTntBlock FAKE_TNT = reg("fake_tnt",
            new FakeTntBlock(AbstractBlock.Settings.copy(Blocks.CAKE)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(SuperTntMod.MOD_ID, "fake_tnt")))));

    // 🔒 Blocker Sandık - Sadece sahibi açabilir, başkası ölür
    public static final BlockerChestBlock BLOCKER_CHEST = reg("blocker_chest",
            new BlockerChestBlock(AbstractBlock.Settings.copy(Blocks.CHEST)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(SuperTntMod.MOD_ID, "blocker_chest")))));

    // 🪞 Ayna - Koyulduğunda parlar, aynaya bakan enderman'lar kendine saldırır
    public static final MirrorBlock MIRROR = reg("mirror",
            new MirrorBlock(AbstractBlock.Settings.copy(Blocks.GLASS)
                    .luminance(state -> 15)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(SuperTntMod.MOD_ID, "mirror")))));

    // 💡 Işık Bloğu - Parlar ve 100 blok yarıçapındaki tüm düşman mobları yok eder
    public static final LightBombBlock LIGHT_BOMB = reg("light_bomb",
            new LightBombBlock(AbstractBlock.Settings.copy(Blocks.GLOWSTONE)
                    .luminance(state -> 15)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(SuperTntMod.MOD_ID, "light_bomb")))));

    // 👁 Herobrine Çağırıcı - Yerleştirilince Herobrine doğurur ve blok kaybolur
    public static final HerobrineSpawnerBlock HEROBRINE_SPAWNER = reg("herobrine_spawner",
            new HerobrineSpawnerBlock(AbstractBlock.Settings.copy(Blocks.OBSIDIAN)
                    .hardness(50.0f)
                    .resistance(1200.0f)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(SuperTntMod.MOD_ID, "herobrine_spawner")))));

    // 🧱 Lego Tuğla - Lego TNT tarafından oluşturulan dekoratif blok (16 renk)
    public static final LegoBrickBlock LEGO_BRICK = regBlockOnly("lego_brick",
            new LegoBrickBlock(AbstractBlock.Settings.copy(Blocks.STONE)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(SuperTntMod.MOD_ID, "lego_brick")))));

    // 🕳 Tünellenmiş Blok - TunnelingItem ile oluşturulan kısmen kazılmış blok
    public static final TunneledBlock TUNNELED_BLOCK = regBlockOnly("tunneled_block",
            new TunneledBlock(AbstractBlock.Settings.copy(Blocks.STONE)
                    .nonOpaque()
                    .dynamicBounds()
                    .suffocates((state, world, pos) -> false)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(SuperTntMod.MOD_ID, "tunneled_block")))));

    // 🌀 Portal Bloğu - Portal silahı tarafından oluşturulan ışınlanma portalı
    public static final PortalBlock PORTAL_BLOCK = regBlockOnly("portal_block",
            new PortalBlock(AbstractBlock.Settings.copy(Blocks.GLASS)
                    .nonOpaque()
                    .noCollision()
                    .luminance(state -> 11)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(SuperTntMod.MOD_ID, "portal_block")))));

    // 🚪 End Kapısı - Yerleştirilebilir, içinden geçince End'e ışınlar
    public static final EndGateBlock END_GATE = reg("end_gate",
            new EndGateBlock(AbstractBlock.Settings.copy(Blocks.OBSIDIAN)
                    .nonOpaque()
                    .noCollision()
                    .luminance(state -> 12)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(SuperTntMod.MOD_ID, "end_gate")))));

    // 👻 Hayalet Blok - İçinden geçilebilir, çim bloğu gibi görünür
    public static final GhostBlock GHOST_BLOCK = reg("ghost_block",
            new GhostBlock(AbstractBlock.Settings.copy(Blocks.GRASS_BLOCK)
                    .nonOpaque()
                    .noCollision()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(SuperTntMod.MOD_ID, "ghost_block")))));

    // 🪵 Tahta Hayalet Blok - İçinden geçilebilir, tahta görünümlü
    public static final GhostBlock WOODEN_GHOST_BLOCK = reg("wooden_ghost_block",
            new GhostBlock(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS)
                    .nonOpaque()
                    .noCollision()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(SuperTntMod.MOD_ID, "wooden_ghost_block")))));

    // ⚠ Yanlış Altın Plaka - Üstüne basan ölür, kırılamaz
    public static final WrongGoldenPlateBlock WRONG_GOLDEN_PLATE = reg("wrong_golden_plate",
            new WrongGoldenPlateBlock(AbstractBlock.Settings.copy(Blocks.GOLD_BLOCK)
                    .hardness(-1.0f).resistance(3600000.0f)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(SuperTntMod.MOD_ID, "wrong_golden_plate")))));

    // ✅ Doğru Altın Plaka - Üstüne basılabilir, kırılamaz
    public static final RightGoldenPlateBlock RIGHT_GOLDEN_PLATE = reg("right_golden_plate",
            new RightGoldenPlateBlock(AbstractBlock.Settings.copy(Blocks.GOLD_BLOCK)
                    .hardness(-1.0f).resistance(3600000.0f)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(SuperTntMod.MOD_ID, "right_golden_plate")))));

    // 🌋 Nether TNT - Havada netherrack adası + 15 nether portalı
    public static final NetherTntBlock NETHER_TNT = reg("nether_tnt",
            new NetherTntBlock(hardTntSettings("nether_tnt")));

    // 🌌 End TNT - Havada end taşı adası + açık end portalları
    public static final EndTntBlock END_TNT = reg("end_tnt",
            new EndTntBlock(hardTntSettings("end_tnt")));

    // 🪟 Cam TNT - Tüm cam bloklarını kırar
    public static final CamTntBlock CAM_TNT = reg("cam_tnt",
            new CamTntBlock(tntSettings("cam_tnt")));

    // 👁 Gizli TNT - Görünmez, 25 blok yarıçapındaki herkesi öldürür
    public static final GizliTntBlock GIZLI_TNT = reg("gizli_tnt",
            new GizliTntBlock(tntSettings("gizli_tnt")));

    // 🐇 Üreyen TNT - Bedrock çıkana kadar çoğalır
    public static final UreyenTntBlock UREYEN_TNT = reg("ureyen_tnt",
            new UreyenTntBlock(tntSettings("ureyen_tnt")));

    // 📦 Küp TNT - Yerin içine 25 blok küp şeklinde patlatır
    public static final KupTntBlock KUP_TNT = reg("kup_tnt",
            new KupTntBlock(hardTntSettings("kup_tnt")));

    // ❤ Kalp TNT - Sağlık verir, efektleri temizler, boyutu sıfırlar
    public static final KalpTntBlock KALP_TNT = reg("kalp_tnt",
            new KalpTntBlock(tntSettings("kalp_tnt")));

    // ☀ Güneş TNT - End kristalleri yaratır, 5 sn sonra 1 kalır
    public static final GunesTntBlock GUNES_TNT = reg("gunes_tnt",
            new GunesTntBlock(tntSettings("gunes_tnt")));

    // ☁ Bulut TNT - Yağmur başlatır
    public static final BulutTntBlock BULUT_TNT = reg("bulut_tnt",
            new BulutTntBlock(tntSettings("bulut_tnt")));

    // ⛈ Şimşek Yağmur TNT - Fırtına başlatır ve yıldırım yağdırır
    public static final SimsekYagmurTntBlock SIMSEK_YAGMUR_TNT = reg("simsek_yagmur_tnt",
            new SimsekYagmurTntBlock(hardTntSettings("simsek_yagmur_tnt")));

    // 🟢 Zehir TNT - Yavaşlatma ve zehir verir
    public static final ZehirTntBlock ZEHIR_TNT = reg("zehir_tnt",
            new ZehirTntBlock(tntSettings("zehir_tnt")));

    // 💧 Ölümcül Su TNT - Su yaratır ve hasar verir
    public static final OlumculSuTntBlock OLUMCUL_SU_TNT = reg("olumcul_su_tnt",
            new OlumculSuTntBlock(tntSettings("olumcul_su_tnt")));

    // 🟠 Zeynep Komut TNT - Tüm güçlü efektleri ve eşyaları verir
    public static final ZeynepKomutTntBlock ZEYNEP_KOMUT_TNT = reg("zeynep_komut_tnt",
            new ZeynepKomutTntBlock(veryHardTntSettings("zeynep_komut_tnt")));

    // 💎 Elmas Zırh TNT
    public static final ElmasZirhTntBlock ELMAS_ZIRH_TNT = reg("elmas_zirh_tnt",
            new ElmasZirhTntBlock(veryHardTntSettings("elmas_zirh_tnt")));

    // 🔤 Harf TNT - Kağıt fırlatır, can yeniler
    public static final HarfTntBlock HARF_TNT = reg("harf_tnt",
            new HarfTntBlock(tntSettings("harf_tnt")));

    // 💙 Zeynep TNT - Lacivert, etrafa boş kağıt saçar
    public static final ZeynepTntBlock ZEYNEP_TNT = reg("zeynep_tnt",
            new ZeynepTntBlock(tntSettings("zeynep_tnt")));

    // 🦓 Zebra TNT - Etrafa kağıt saçar + zebra/at sesi çıkarır
    public static final ZebraTntBlock ZEBRA_TNT = reg("zebra_tnt",
            new ZebraTntBlock(tntSettings("zebra_tnt")));

    // 🔴 Redstone TNT - Yüksek güç + hız/güç verir
    public static final RedstoneTntBlock REDSTONE_TNT = reg("redstone_tnt",
            new RedstoneTntBlock(hardTntSettings("redstone_tnt")));

    // ⛏ Maden TNT - Her madenden 10 tane verir
    public static final MadenTntBlock MADEN_TNT = reg("maden_tnt",
            new MadenTntBlock(tntSettings("maden_tnt")));

    // 🪚 Crafting Table TNT - Altın kask, netherite kılıç, demir balta saçar
    public static final CraftingTableTntBlock CRAFTING_TABLE_TNT = reg("crafting_table_tnt",
            new CraftingTableTntBlock(tntSettings("crafting_table_tnt")));

    // ❓ Soru Bloğu - kırılınca rastgele savaş eşyası / lav / saldırgan mob
    public static final SoruBloguBlock SORU_BLOGU = reg("soru_blogu",
            new SoruBloguBlock(AbstractBlock.Settings.copy(Blocks.GRASS_BLOCK)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(SuperTntMod.MOD_ID, "soru_blogu")))));

    // 🦠 Zehir Toprağı - sadece Koku Bombası ile yerleşir; değen ölür; 60 sn sonra yok olur
    public static final ZehirToprakBlock ZEHIR_TOPRAK = regBlockOnly("zehir_toprak",
            new ZehirToprakBlock(AbstractBlock.Settings.copy(Blocks.MOSS_BLOCK)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(SuperTntMod.MOD_ID, "zehir_toprak")))
                    .strength(0.5f)));

    // TunneledBlock için BlockEntity tipi
    public static final BlockEntityType<TunneledBlockEntity> TUNNELED_BLOCK_ENTITY_TYPE =
            Registry.register(Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(SuperTntMod.MOD_ID, "tunneled_block"),
                    FabricBlockEntityTypeBuilder.create(TunneledBlockEntity::new, TUNNELED_BLOCK).build());


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
        SuperTntMod.LOGGER.info("63 blok kaydedildi.");
    }
}
