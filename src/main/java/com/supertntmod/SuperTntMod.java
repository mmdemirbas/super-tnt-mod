package com.supertntmod;

import com.supertntmod.block.EncryptedTntChestBlock;
import com.supertntmod.block.ModBlocks;
import com.supertntmod.entity.ModEntities;
import com.supertntmod.entity.WalkingTntEntity;
import com.supertntmod.entity.WoodTntEntity;
import com.supertntmod.item.ModItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SuperTntMod implements ModInitializer {
    public static final String MOD_ID = "supertntmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Özel item grubu
    public static final RegistryKey<ItemGroup> SUPER_TNT_GROUP = RegistryKey.of(
            RegistryKeys.ITEM_GROUP, Identifier.of(MOD_ID, "super_tnt_group"));

    @Override
    public void onInitialize() {
        ModEntities.register();
        ModBlocks.register();
        ModItems.register();

        // Özel creative tab oluştur
        Registry.register(Registries.ITEM_GROUP, SUPER_TNT_GROUP,
                FabricItemGroup.builder()
                        .icon(() -> new ItemStack(ModBlocks.DIAMOND_TNT))
                        .displayName(Text.literal("Super TNT"))
                        .entries((context, entries) -> {
                            // Orijinal TNT'ler
                            entries.add(ModBlocks.DIAMOND_TNT);
                            entries.add(ModBlocks.GOLD_TNT);
                            entries.add(ModBlocks.BEDROCK_TNT);
                            entries.add(ModBlocks.EMERALD_TNT);
                            entries.add(ModBlocks.LIGHTNING_TNT);
                            entries.add(ModBlocks.NUCLEAR_TNT);
                            entries.add(ModBlocks.FREEZE_TNT);
                            // Yeni TNT'ler
                            entries.add(ModBlocks.WOOD_TNT);
                            entries.add(ModBlocks.COMMAND_TNT);
                            entries.add(ModBlocks.TNT_DOOR);
                            entries.add(ModBlocks.ENCRYPTED_TNT_CHEST);
                            entries.add(ModBlocks.WALKING_TNT);
                            entries.add(ModBlocks.MOB_FREEZE_TNT);
                            entries.add(ModBlocks.WATER_TNT);
                            entries.add(ModBlocks.RAINBOW_TNT);
                            entries.add(ModBlocks.LEGO_TNT);
                            entries.add(ModBlocks.MAKARNA_TNT);
                            entries.add(ModBlocks.SEKER_TNT);
                            entries.add(ModBlocks.SHRINK_TNT);
                            entries.add(ModBlocks.GROWTH_TNT);
                            entries.add(ModBlocks.CLEANSE_TNT);
                            entries.add(ModBlocks.FAKE_TNT);
                            // Item'ler
                            entries.add(ModItems.TNT_FRISBEE);
                            entries.add(ModItems.TUNNELING_ITEM);
                        })
                        .build());

        // Odun TNT geri yükleme zamanlayıcısı (tek bir listener)
        ServerTickEvents.END_SERVER_TICK.register(server -> WoodTntEntity.tickRestores());

        // Yürüyen TNT entity özelliklerini kaydet
        FabricDefaultAttributeRegistry.register(ModEntities.WALKING_TNT, WalkingTntEntity.createAttributes());

        // Şifreli TNT sandık için chat mesajı dinleyicisi
        // ALLOW_CHAT_MESSAGE: şifre mesajını iptal edip diğer oyunculara göstermez
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            String text = message.getContent().getString();
            if (sender instanceof ServerPlayerEntity player) {
                boolean handled = EncryptedTntChestBlock.handleChatMessage(player, text);
                if (handled) return false; // Şifre mesajını iptal et, kimse görmesin
            }
            return true; // Normal mesaj, devam et
        });

        LOGGER.info("Super TNT Modu yüklendi! 23 blok + 1 item hazır.");
    }
}
