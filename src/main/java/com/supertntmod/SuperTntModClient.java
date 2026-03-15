package com.supertntmod;

import com.supertntmod.client.TntFrisbeeEntityRenderer;
import com.supertntmod.client.WalkingTntEntityRenderer;
import com.supertntmod.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
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

        // Yürüyen TNT - özel renderer (TNT bloğu görünümü)
        EntityRendererRegistry.register(ModEntities.WALKING_TNT, WalkingTntEntityRenderer::new);

        // TNT Frizbi - özel renderer (küçük TNT)
        EntityRendererRegistry.register(ModEntities.TNT_FRISBEE, TntFrisbeeEntityRenderer::new);
    }
}
