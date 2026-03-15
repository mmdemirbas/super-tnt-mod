package com.supertntmod.client;

import com.supertntmod.entity.TntFrisbeeEntity;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.TntEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;

/**
 * TNT Frizbi entity'si için basit renderer.
 * Küçük TNT bloğu olarak render eder.
 */
public class TntFrisbeeEntityRenderer extends EntityRenderer<TntFrisbeeEntity, TntEntityRenderState> {

    public TntFrisbeeEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public TntEntityRenderState createRenderState() {
        return new TntEntityRenderState();
    }

    @Override
    public void updateRenderState(TntFrisbeeEntity entity, TntEntityRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
    }

    @Override
    public void render(TntEntityRenderState state, MatrixStack matrices,
                       OrderedRenderCommandQueue renderCommands, CameraRenderState cameraState) {
        matrices.push();
        matrices.scale(0.5f, 0.5f, 0.5f);
        matrices.translate(-0.5, 0.0, -0.5);
        renderCommands.submitBlock(matrices, Blocks.TNT.getDefaultState(), state.light, 0, 0);
        matrices.pop();
        super.render(state, matrices, renderCommands, cameraState);
    }
}
