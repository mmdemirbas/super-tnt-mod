package com.supertntmod.client;

import com.supertntmod.entity.EndPearlEntity;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.TntEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;

public class EndPearlEntityRenderer extends EntityRenderer<EndPearlEntity, TntEntityRenderState> {

    public EndPearlEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public TntEntityRenderState createRenderState() {
        return new TntEntityRenderState();
    }

    @Override
    public void render(TntEntityRenderState state, MatrixStack matrices,
                       OrderedRenderCommandQueue renderCommands, CameraRenderState cameraState) {
        matrices.push();
        matrices.scale(0.3f, 0.3f, 0.3f);
        matrices.translate(-0.5, 0.0, -0.5);
        renderCommands.submitBlock(matrices, Blocks.END_STONE.getDefaultState(), state.light, 0, 0);
        matrices.pop();
        super.render(state, matrices, renderCommands, cameraState);
    }
}
