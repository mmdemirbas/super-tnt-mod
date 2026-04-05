package com.supertntmod;

import com.supertntmod.block.EncryptedTntChestBlock;
import com.supertntmod.block.ModBlocks;
import com.supertntmod.block.PortalBlock;
import com.supertntmod.block.TntDoorBlock;
import com.supertntmod.entity.GravityTntEntity;
import com.supertntmod.entity.ModEntities;
import com.supertntmod.entity.WalkingTntEntity;
import com.supertntmod.entity.WoodTntEntity;
import com.supertntmod.item.ModItems;
import com.supertntmod.item.PortalGunItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
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
    public static final net.minecraft.util.Identifier SCALE_MODIFIER_ID =
            net.minecraft.util.Identifier.of(MOD_ID, "scale");

    // Özel item grubu
    public static final RegistryKey<ItemGroup> SUPER_TNT_GROUP = RegistryKey.of(
            RegistryKeys.ITEM_GROUP, Identifier.of(MOD_ID, "super_tnt_group"));

    @Override
    public void onInitialize() {
        // SCALE attribute minimum değerini düşür (varsayılan 0.0625 = 1/16)
        // Bu sayede 5-6 kez küçülme desteklenir
        var scaleAttr = (net.minecraft.entity.attribute.ClampedEntityAttribute)
                net.minecraft.entity.attribute.EntityAttributes.SCALE.value();
        ((com.supertntmod.mixin.ClampedEntityAttributeAccessor) scaleAttr)
                .supertntmod$setMinValue(0.0001);

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
                            entries.add(ModBlocks.BOUNCE_TNT);
                            entries.add(ModBlocks.PROXIMITY_MINE);
                            entries.add(ModBlocks.MAGNET_TNT);
                            entries.add(ModBlocks.GRAVITY_TNT);
                            entries.add(ModBlocks.INVISIBLE_TNT);
                            entries.add(ModBlocks.SWAP_TNT);
                            // Item'ler
                            entries.add(ModItems.TNT_FRISBEE);
                            entries.add(ModItems.TUNNELING_ITEM);
                            entries.add(ModItems.PORTAL_GUN);
                            entries.add(ModItems.PINK_LEGO_BRICK);
                            entries.add(ModItems.GREEN_LEGO_BRICK);
                            entries.add(ModItems.AMONG_US_REPORT);
                            entries.add(ModItems.GRAPPLING_HOOK);
                            entries.add(ModItems.SHRINK_BALL);
                            entries.add(ModItems.GROW_BALL);
                            entries.add(ModItems.SHRINK_POTION);
                            entries.add(ModItems.GROW_POTION);
                            entries.add(ModItems.SCALE_LOCK);
                        })
                        .build());

        // Odun TNT geri yükleme zamanlayıcısı
        ServerTickEvents.END_SERVER_TICK.register(server -> WoodTntEntity.tickRestores());

        // Sunucu kapanınca stale static state'i temizle
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            WoodTntEntity.clearAll();
            GravityTntEntity.clearAll(server);
            PortalBlock.clearCooldowns();
        });

        // Yerçekimi TNT: ters yerçekimi zamanlayıcısı
        ServerTickEvents.END_SERVER_TICK.register(server -> GravityTntEntity.tickGravity(server));

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

        // Oyuncu ayrılınca ephemeral per-player state'i temizle
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            java.util.UUID id = handler.player.getUuid();
            EncryptedTntChestBlock.onPlayerDisconnect(id);
            TntDoorBlock.onPlayerDisconnect(id);
            PortalGunItem.onPlayerDisconnect(id);
        });

        LOGGER.info("Super TNT Modu yüklendi! 25 blok + 5 item hazır.");
    }
}
