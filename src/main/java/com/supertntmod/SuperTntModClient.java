package com.supertntmod;

import com.supertntmod.block.ModBlocks;
import com.supertntmod.client.DrawingScreen;
import com.supertntmod.client.EnderSendEntityRenderer;
import com.supertntmod.client.GrowBallEntityRenderer;
import com.supertntmod.client.GrowPotionEntityRenderer;
import com.supertntmod.client.PortalProjectileEntityRenderer;
import com.supertntmod.client.ShrinkPotionEntityRenderer;
import com.supertntmod.client.ShrinkBallEntityRenderer;
import com.supertntmod.client.TntFrisbeeEntityRenderer;
import com.supertntmod.client.TunneledBlockEntityRenderer;
import com.supertntmod.client.WalkingTntEntityRenderer;
import com.supertntmod.entity.ModEntities;
import com.supertntmod.item.ModItems;
import com.supertntmod.network.ControlRemoteC2SPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.entity.TntEntityRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.ActionResult;
import org.lwjgl.glfw.GLFW;

public class SuperTntModClient implements ClientModInitializer {

    // Kontrol Kumandası aktivasyon tuşu (T)
    private static final KeyBinding CONTROL_REMOTE_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.supertntmod.control_remote",
                    InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_T,
                    KeyBinding.Category.GAMEPLAY));

    @Override
    public void onInitializeClient() {
        // Orijinal TNT entity renderer'lar
        EntityRendererRegistry.register(ModEntities.DIAMOND_TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.GOLD_TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.BEDROCK_TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.EMERALD_TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.LIGHTNING_TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.NUCLEAR_TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.FREEZE_TNT, TntEntityRenderer::new);

        // Yeni TNT entity renderer'lar
        EntityRendererRegistry.register(ModEntities.WOOD_TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.COMMAND_TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MOB_FREEZE_TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.RAINBOW_TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.WATER_TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.LEGO_TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MAKARNA_TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.SEKER_TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.SHRINK_TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.GROWTH_TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.CLEANSE_TNT, TntEntityRenderer::new);

        EntityRendererRegistry.register(ModEntities.MAGNET_TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.GRAVITY_TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.INVISIBLE_TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.SWAP_TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.BOUNCE_TNT, TntEntityRenderer::new);

        // Yürüyen TNT - özel renderer (TNT bloğu görünümü)
        EntityRendererRegistry.register(ModEntities.WALKING_TNT, WalkingTntEntityRenderer::new);

        // TNT Frizbi - özel renderer (küçük TNT)
        EntityRendererRegistry.register(ModEntities.TNT_FRISBEE, TntFrisbeeEntityRenderer::new);

        // Portal mermisi - özel renderer
        EntityRendererRegistry.register(ModEntities.PORTAL_PROJECTILE, PortalProjectileEntityRenderer::new);

        // Kanca entity — portal projectile renderer'ını yeniden kullan (küçük gri küp)
        EntityRendererRegistry.register(ModEntities.GRAPPLING_HOOK, ctx ->
                new com.supertntmod.client.GrapplingHookEntityRenderer(ctx));

        // Top entity'leri - özel renderer (renkli beton küp)
        EntityRendererRegistry.register(ModEntities.SHRINK_BALL, ShrinkBallEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.GROW_BALL, GrowBallEntityRenderer::new);

        // İksir entity'leri - özel renderer (renkli beton küp)
        EntityRendererRegistry.register(ModEntities.SHRINK_POTION, ShrinkPotionEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.GROW_POTION, GrowPotionEntityRenderer::new);

        // Ender Send - blok tabanlı dev mob renderer
        EntityRendererRegistry.register(ModEntities.ENDER_SEND, EnderSendEntityRenderer::new);

        // Herobrine - blok tabanlı insansı renderer
        EntityRendererRegistry.register(ModEntities.HEROBRINE,
                com.supertntmod.client.HerobrineEntityRenderer::new);

        // Yeni TNT ve projectile renderer'ları
        EntityRendererRegistry.register(ModEntities.NETHER_TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.END_TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.END_PEARL,
                com.supertntmod.client.EndPearlEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.NETHER_PEARL,
                com.supertntmod.client.NetherPearlEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.CAM_TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.GIZLI_TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.UREYEN_TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.KUP_TNT, TntEntityRenderer::new);

        // Yeni TNT renderer'ları
        EntityRendererRegistry.register(ModEntities.KALP_TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.GUNES_TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.BULUT_TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.SIMSEK_YAGMUR_TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.ZEHIR_TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.OLUMCUL_SU_TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.ZEYNEP_KOMUT_TNT, TntEntityRenderer::new);

        // Yeni paylaşılan TNT renderer'ları
        EntityRendererRegistry.register(ModEntities.REDSTONE_TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MADEN_TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.CRAFTING_TABLE_TNT, TntEntityRenderer::new);

        // Paylaşılan: Koku Bombası — küçük yeşil küp
        EntityRendererRegistry.register(ModEntities.KOKU_BOMBASI,
                com.supertntmod.client.KokuBombasiEntityRenderer::new);


        // Portal bloğu render katmanı
        BlockRenderLayerMap.putBlock(ModBlocks.PORTAL_BLOCK, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.END_GATE, BlockRenderLayer.TRANSLUCENT);
        BlockRenderLayerMap.putBlock(ModBlocks.GIZLI_TNT, BlockRenderLayer.TRANSLUCENT);
        BlockRenderLayerMap.putBlock(ModBlocks.GHOST_BLOCK, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.WOODEN_GHOST_BLOCK, BlockRenderLayer.CUTOUT);

        // Çim Hayalet Bloğu — vanilla çim bloğu gibi biyom tonlamalı yeşil
        net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.BLOCK.register(
                (state, world, pos, tintIndex) -> {
                    if (tintIndex != 0) return -1;
                    if (world == null || pos == null) return 0x91BD59;
                    return net.minecraft.client.color.world.BiomeColors.getGrassColor(world, pos);
                }, ModBlocks.GHOST_BLOCK);

        // Tünellenmiş blok - özel BlockEntity renderer
        BlockEntityRendererRegistry.register(ModBlocks.TUNNELED_BLOCK_ENTITY_TYPE, TunneledBlockEntityRenderer::new);
        BlockRenderLayerMap.putBlock(ModBlocks.TUNNELED_BLOCK, BlockRenderLayer.CUTOUT);

        // Çizim eşyası: sağ tıklayınca çizim ekranını aç
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient() && player.getStackInHand(hand).isOf(ModItems.DRAWING_ITEM)) {
                MinecraftClient.getInstance().setScreen(new DrawingScreen(player.getYaw()));
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });

        // Kontrol Kumandası: T tuşu basıldığında server'a sinyal gönder
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (CONTROL_REMOTE_KEY.wasPressed()) {
                if (client.player != null) {
                    // Elinde Kontrol Kumandası var mı kontrol et (client-side)
                    boolean holding = client.player.getMainHandStack().isOf(ModItems.CONTROL_REMOTE)
                            || client.player.getOffHandStack().isOf(ModItems.CONTROL_REMOTE);
                    if (holding) {
                        ClientPlayNetworking.send(new ControlRemoteC2SPayload());
                    }
                }
            }
        });
    }
}
