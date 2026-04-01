package com.supertntmod;

import com.supertntmod.block.ModBlocks;
import com.supertntmod.client.GrowBallEntityRenderer;
import com.supertntmod.client.PortalProjectileEntityRenderer;
import com.supertntmod.client.ShrinkBallEntityRenderer;
import com.supertntmod.client.TntFrisbeeEntityRenderer;
import com.supertntmod.client.TunneledBlockEntityRenderer;
import com.supertntmod.client.WalkingTntEntityRenderer;
import com.supertntmod.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.entity.TntEntityRenderer;

public class SuperTntModClient implements ClientModInitializer {
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

        EntityRendererRegistry.register(ModEntities.BOUNCE_TNT, TntEntityRenderer::new);

        // Yürüyen TNT - özel renderer (TNT bloğu görünümü)
        EntityRendererRegistry.register(ModEntities.WALKING_TNT, WalkingTntEntityRenderer::new);

        // TNT Frizbi - özel renderer (küçük TNT)
        EntityRendererRegistry.register(ModEntities.TNT_FRISBEE, TntFrisbeeEntityRenderer::new);

        // Portal mermisi - özel renderer
        EntityRendererRegistry.register(ModEntities.PORTAL_PROJECTILE, PortalProjectileEntityRenderer::new);

        // Top entity'leri - özel renderer (renkli beton küp)
        EntityRendererRegistry.register(ModEntities.SHRINK_BALL, ShrinkBallEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.GROW_BALL, GrowBallEntityRenderer::new);

        // Portal bloğu render katmanı
        BlockRenderLayerMap.putBlock(ModBlocks.PORTAL_BLOCK, BlockRenderLayer.CUTOUT);

        // Tünellenmiş blok - özel BlockEntity renderer
        BlockEntityRendererRegistry.register(ModBlocks.TUNNELED_BLOCK_ENTITY_TYPE, TunneledBlockEntityRenderer::new);
        BlockRenderLayerMap.putBlock(ModBlocks.TUNNELED_BLOCK, BlockRenderLayer.CUTOUT);
    }
}
