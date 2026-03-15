package com.supertntmod;

import com.supertntmod.block.EncryptedTntChestBlock;
import com.supertntmod.block.ModBlocks;
import com.supertntmod.entity.ModEntities;
import com.supertntmod.entity.WalkingTntEntity;
import com.supertntmod.item.ModItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SuperTntMod implements ModInitializer {
    public static final String MOD_ID = "supertntmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModEntities.register();
        ModBlocks.register();
        ModItems.register();

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
