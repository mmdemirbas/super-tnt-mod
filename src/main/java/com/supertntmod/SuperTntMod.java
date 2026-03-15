package com.supertntmod;

import com.supertntmod.block.EncryptedTntChestBlock;
import com.supertntmod.block.ModBlocks;
import com.supertntmod.entity.ModEntities;
import com.supertntmod.entity.WalkingTntEntity;
import com.supertntmod.item.ModItems;
import net.fabricmc.api.ModInitializer;
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
                            entries.add(ModBlocks.RAINBOW_TNT);
                            // Item'ler
                            entries.add(ModItems.TNT_FRISBEE);
                        })
                        .build());

        // Yürüyen TNT entity özelliklerini kaydet
        FabricDefaultAttributeRegistry.register(ModEntities.WALKING_TNT, WalkingTntEntity.createAttributes());

        // Şifreli TNT sandık için chat mesajı dinleyicisi
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            String text = message.getContent().getString();
            if (sender instanceof ServerPlayerEntity player) {
                EncryptedTntChestBlock.handleChatMessage(player, text);
            }
        });

        LOGGER.info("Super TNT Modu yüklendi! 15 yeni TNT/blok türü hazır.");
    }
}
