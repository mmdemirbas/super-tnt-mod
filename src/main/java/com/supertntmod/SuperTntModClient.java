package com.supertntmod;

import com.supertntmod.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.TntEntityRenderer;

public class SuperTntModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.DIAMOND_TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.GOLD_TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.BEDROCK_TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.EMERALD_TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.LIGHTNING_TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.NUCLEAR_TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.FREEZE_TNT, TntEntityRenderer::new);
    }
}
