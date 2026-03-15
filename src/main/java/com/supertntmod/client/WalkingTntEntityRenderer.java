package com.supertntmod.client;

import com.supertntmod.entity.WalkingTntEntity;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.TntEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;

/**
 * Yürüyen TNT entity'si için basit renderer.
 * TNT bloğu görünümü ile render eder.
 */
public class WalkingTntEntityRenderer extends EntityRenderer<WalkingTntEntity, TntEntityRenderState> {

    public WalkingTntEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public TntEntityRenderState createRenderState() {
        return new TntEntityRenderState();
    }

    @Override
    public void updateRenderState(WalkingTntEntity entity, TntEntityRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
    }

    @Override
    public void render(TntEntityRenderState state, MatrixStack matrices,
                       OrderedRenderCommandQueue renderCommands, CameraRenderState cameraState) {
        matrices.push();
        matrices.translate(-0.5, 0.0, -0.5);
        renderCommands.submitBlock(matrices, Blocks.TNT.getDefaultState(), state.light, 0, 0);
        matrices.pop();
        super.render(state, matrices, renderCommands, cameraState);
    }
}
