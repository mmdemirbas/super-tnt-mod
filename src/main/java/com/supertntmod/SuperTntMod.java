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
import com.supertntmod.network.DrawingC2SPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
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
                            // TNT Zırh
                            entries.add(ModItems.TNT_ARMOR_HELMET);
                            entries.add(ModItems.TNT_ARMOR_CHESTPLATE);
                            entries.add(ModItems.TNT_ARMOR_LEGGINGS);
                            entries.add(ModItems.TNT_ARMOR_BOOTS);
                            // Çizim Eşyası
                            entries.add(ModItems.DRAWING_ITEM);
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

        // Çizim eşyası: C2S payload kaydı + server handler
        PayloadTypeRegistry.playC2S().register(DrawingC2SPayload.ID, DrawingC2SPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(DrawingC2SPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> handleDrawingPayload(player, payload));
        });

        LOGGER.info("Super TNT Modu yüklendi! 25 blok + 16 item hazır.");
    }

    /**
     * Çizim verisini yün bloklarına dönüştürür.
     * 16x16 piksel, oyuncunun baktığı yöne dik duvar olarak 3 blok ileride inşa edilir.
     */
    private static void handleDrawingPayload(ServerPlayerEntity player, DrawingC2SPayload payload) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) return;

        byte[] pixels = payload.pixels();
        if (pixels.length != DrawingC2SPayload.PIXEL_COUNT) return;

        // 16 yün bloğu (renk indeksi 1-16 sırasıyla)
        Block[] WOOL_BLOCKS = {
                Blocks.WHITE_WOOL, Blocks.ORANGE_WOOL, Blocks.MAGENTA_WOOL,
                Blocks.LIGHT_BLUE_WOOL, Blocks.YELLOW_WOOL, Blocks.LIME_WOOL,
                Blocks.PINK_WOOL, Blocks.GRAY_WOOL, Blocks.LIGHT_GRAY_WOOL,
                Blocks.CYAN_WOOL, Blocks.PURPLE_WOOL, Blocks.BLUE_WOOL,
                Blocks.BROWN_WOOL, Blocks.GREEN_WOOL, Blocks.RED_WOOL,
                Blocks.BLACK_WOOL
        };

        // Oyuncunun baktığı yönü hesapla (4 ana yön)
        float yaw = payload.yaw() % 360;
        if (yaw < 0) yaw += 360;

        Direction facing;
        if (yaw >= 315 || yaw < 45) facing = Direction.SOUTH;
        else if (yaw < 135) facing = Direction.WEST;
        else if (yaw < 225) facing = Direction.NORTH;
        else facing = Direction.EAST;

        // Yapı 3 blok ileride, oyuncunun baktığı yöne dik
        BlockPos origin = player.getBlockPos().offset(facing, 3);

        // Sağ yön (duvarın genişlik ekseni)
        Direction right = facing.rotateYClockwise();

        // Çizimi bloklara dönüştür (y ekseni ters: piksel 0,0 = sol üst = en üst blok)
        int size = DrawingC2SPayload.CANVAS_SIZE;
        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                int colorIdx = pixels[py * size + px] & 0xFF;
                if (colorIdx < 1 || colorIdx > 16) continue;

                // Blok konumu: origin + sağa px + yukarı (size-1-py)
                BlockPos blockPos = origin
                        .offset(right, px - size / 2)
                        .up(size - 1 - py);

                // Sadece hava bloklarını değiştir (mevcut yapıları bozmaz)
                if (world.getBlockState(blockPos).isAir()) {
                    world.setBlockState(blockPos, WOOL_BLOCKS[colorIdx - 1].getDefaultState());
                }
            }
        }

        player.sendMessage(
                net.minecraft.text.Text.translatable("message.supertntmod.drawing.built"), true);
    }
}
