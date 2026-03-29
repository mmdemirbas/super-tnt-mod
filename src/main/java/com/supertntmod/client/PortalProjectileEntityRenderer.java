package com.supertntmod.client;

import com.supertntmod.entity.PortalProjectileEntity;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.TntEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Portal mermisi renderer: küçük pembe/yeşil beton bloğu olarak render eder.
 */
public class PortalProjectileEntityRenderer extends EntityRenderer<PortalProjectileEntity, TntEntityRenderState> {
    private boolean isPink = true;

    public PortalProjectileEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public TntEntityRenderState createRenderState() {
        return new TntEntityRenderState();
    }

    @Override
    public void updateRenderState(PortalProjectileEntity entity, TntEntityRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        this.isPink = entity.isPink();
    }

    @Override
    public void render(TntEntityRenderState state, MatrixStack matrices,
                       OrderedRenderCommandQueue renderCommands, CameraRenderState cameraState) {
        matrices.push();
        matrices.scale(0.3f, 0.3f, 0.3f);
        matrices.translate(-0.5, 0.0, -0.5);
        var block = isPink ? Blocks.PINK_CONCRETE : Blocks.GREEN_CONCRETE;
        renderCommands.submitBlock(matrices, block.getDefaultState(), state.light, 0, 0);
        matrices.pop();
        super.render(state, matrices, renderCommands, cameraState);
    }
}
