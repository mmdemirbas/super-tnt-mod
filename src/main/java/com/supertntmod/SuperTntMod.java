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
                            // Blocker Sandık
                            entries.add(ModBlocks.BLOCKER_CHEST);
                            // Ametist Zırh
                            entries.add(ModItems.AMETHYST_HELMET);
                            entries.add(ModItems.AMETHYST_CHESTPLATE);
                            entries.add(ModItems.AMETHYST_LEGGINGS);
                            entries.add(ModItems.AMETHYST_BOOTS);
                            // Yeni bloklar
                            entries.add(ModBlocks.MIRROR);
                            entries.add(ModBlocks.LIGHT_BOMB);
                            // Yeni item'ler
                            entries.add(ModItems.SPICY_CHIPS);
                            entries.add(ModItems.ENERGY_CRYSTAL);
                            entries.add(ModItems.CRAFT_AXE);
                            // Ender Send
                            entries.add(ModItems.ENDER_SEND_SPAWN_EGG);
                            // Yeni eşyalar
                            entries.add(ModItems.CONTROL_REMOTE);
                            entries.add(ModItems.BLACK_HOLE);
                            entries.add(ModItems.LIGHTNING_SPELL);
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
            com.supertntmod.item.CraftAxeItem.clearAll();
            com.supertntmod.item.AmethystArmorState.clearAll();
            com.supertntmod.block.BlockerChestBlock.clearBans();
        });

        // Yerçekimi TNT: ters yerçekimi zamanlayıcısı
        ServerTickEvents.END_SERVER_TICK.register(server -> GravityTntEntity.tickGravity(server));

        // Yürüyen TNT entity özelliklerini kaydet
        FabricDefaultAttributeRegistry.register(ModEntities.WALKING_TNT, WalkingTntEntity.createAttributes());

        // Ender Send entity özelliklerini kaydet
        FabricDefaultAttributeRegistry.register(ModEntities.ENDER_SEND,
                com.supertntmod.entity.EnderSendEntity.createAttributes());

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
            com.supertntmod.item.AmethystArmorState.onPlayerDisconnect(id);
            com.supertntmod.block.BlockerChestBlock.onPlayerDisconnect(id);
        });

        // Blocker Sandık: sandık yasağı olan oyuncuların herhangi bir sandığı açmasını engelle
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient()) return net.minecraft.util.ActionResult.PASS;
            if (!com.supertntmod.block.BlockerChestBlock.isChestBanned(player.getUuid()))
                return net.minecraft.util.ActionResult.PASS;
            // Sandık benzeri bloklara etkileşimi engelle
            net.minecraft.block.BlockState state = world.getBlockState(hitResult.getBlockPos());
            if (state.getBlock() instanceof net.minecraft.block.ChestBlock
                    || state.getBlock() instanceof net.minecraft.block.EnderChestBlock
                    || state.getBlock() instanceof net.minecraft.block.BarrelBlock
                    || state.getBlock() instanceof EncryptedTntChestBlock
                    || state.getBlock() instanceof com.supertntmod.block.BlockerChestBlock) {
                player.sendMessage(net.minecraft.text.Text.translatable(
                        "message.supertntmod.blocker_chest.banned"), true);
                return net.minecraft.util.ActionResult.FAIL;
            }
            return net.minecraft.util.ActionResult.PASS;
        });

        // Kontrol Kumandası: C2S payload kaydı + server handler
        PayloadTypeRegistry.playC2S().register(
                com.supertntmod.network.ControlRemoteC2SPayload.ID,
                com.supertntmod.network.ControlRemoteC2SPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(
                com.supertntmod.network.ControlRemoteC2SPayload.ID, (payload, context) -> {
                    ServerPlayerEntity p = context.player();
                    context.server().execute(() ->
                            com.supertntmod.item.ControlRemoteItem.activate(p));
                });

        // Çizim eşyası: C2S payload kaydı + server handler
        PayloadTypeRegistry.playC2S().register(DrawingC2SPayload.ID, DrawingC2SPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(DrawingC2SPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> handleDrawingPayload(player, payload));
        });

        LOGGER.info("Super TNT Modu yüklendi! 26 blok + 23 item + 1 mob hazır.");
    }

    /**
     * Çizim verisini lego bloklarına dönüştürür.
     * 16x16x16 piksel (16 katman), oyuncunun baktığı yöne doğru 3D yapı olarak inşa edilir.
     * Katman 0 = oyuncuya en yakın, katman 15 = en uzak.
     * Oyuncu küçükse (scale < 0.5) yapı yarı boyutlu bloklar olarak yerleştirilir.
     */
    private static void handleDrawingPayload(ServerPlayerEntity player, DrawingC2SPayload payload) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) return;

        byte[] pixels = payload.pixels();
        if (pixels.length != DrawingC2SPayload.PIXEL_COUNT) return;

        // Oyuncunun baktığı yönü hesapla (4 ana yön)
        float yaw = payload.yaw() % 360;
        if (yaw < 0) yaw += 360;

        Direction facing;
        if (yaw >= 315 || yaw < 45) facing = Direction.SOUTH;
        else if (yaw < 135) facing = Direction.WEST;
        else if (yaw < 225) facing = Direction.NORTH;
        else facing = Direction.EAST;

        // Yapı 3 blok ileride başlar
        BlockPos origin = player.getBlockPos().offset(facing, 3);

        // Sağ yön (duvarın genişlik ekseni)
        Direction right = facing.rotateYClockwise();

        int size = DrawingC2SPayload.CANVAS_SIZE;
        int layers = DrawingC2SPayload.LAYERS;

        // Oyuncunun ölçeğini kontrol et: küçükse oluşturulan bloklar da küçük olsun
        double playerScale = 1.0;
        net.minecraft.entity.attribute.EntityAttributeInstance scaleAttr =
                player.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.SCALE);
        if (scaleAttr != null) {
            playerScale = scaleAttr.getValue();
        }
        boolean small = playerScale < 0.5;

        for (int layer = 0; layer < layers; layer++) {
            int layerOffset = layer * size * size;
            for (int py = 0; py < size; py++) {
                for (int px = 0; px < size; px++) {
                    int colorIdx = pixels[layerOffset + py * size + px] & 0xFF;
                    if (colorIdx < 1 || colorIdx > 16) continue;

                    // Blok konumu: origin + derinlik (katman) + sağa + yukarı
                    BlockPos blockPos = origin
                            .offset(facing, layer)
                            .offset(right, px - size / 2)
                            .up(size - 1 - py);

                    if (!world.getBlockState(blockPos).isAir()) continue;

                    // LegoBrick rengi 0-15 (colorIdx 1-16 → renk 0-15)
                    int legoColor = colorIdx - 1;
                    net.minecraft.block.BlockState legoState =
                            ModBlocks.LEGO_BRICK.getDefaultState()
                                    .with(com.supertntmod.block.LegoBrickBlock.COLOR, legoColor);

                    if (small) {
                        // Küçük oyuncu: TunneledBlock olarak yerleştir
                        net.minecraft.util.Identifier blockId =
                                net.minecraft.registry.Registries.BLOCK.getId(ModBlocks.LEGO_BRICK);
                        world.setBlockState(blockPos, ModBlocks.TUNNELED_BLOCK.getDefaultState());
                        if (world.getBlockEntity(blockPos) instanceof com.supertntmod.block.TunneledBlockEntity te) {
                            te.setOriginalBlockId(blockId);
                            // 2x2x2 alt-voxel maskesi (scale ≈ 0.5 için 2/4 = yarı boyut)
                            long mask = 0L;
                            for (int z = 0; z < 2; z++)
                                for (int y = 0; y < 2; y++)
                                    for (int x = 0; x < 2; x++)
                                        mask |= (1L << com.supertntmod.block.TunneledBlockEntity.subVoxelIndex(x, y, z));
                            te.setSubVoxelMask(mask);
                        }
                    } else {
                        world.setBlockState(blockPos, legoState);
                    }
                }
            }
        }

        player.sendMessage(
                net.minecraft.text.Text.translatable("message.supertntmod.drawing.built"), true);
    }
}
