package com.supertntmod.client;

import com.supertntmod.entity.GrapplingHookEntity;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.TntEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Kanca renderer: küçük gri beton küp olarak göster.
 */
public class GrapplingHookEntityRenderer extends EntityRenderer<GrapplingHookEntity, TntEntityRenderState> {

    public GrapplingHookEntityRenderer(EntityRendererFactory.Context ctx) {
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
        matrices.scale(0.2f, 0.2f, 0.2f);
        matrices.translate(-0.5, 0.0, -0.5);
        renderCommands.submitBlock(matrices, Blocks.GRAY_CONCRETE.getDefaultState(), state.light, 0, 0);
        matrices.pop();
        super.render(state, matrices, renderCommands, cameraState);
    }
}
