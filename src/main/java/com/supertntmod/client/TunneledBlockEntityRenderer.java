package com.supertntmod.client;

import com.supertntmod.block.TunneledBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.command.ModelCommandRenderer;

/**
 * TunneledBlock renderer: kalan her sub-voxel'i orijinal bloğun
 * küçültülmüş kopyası olarak render eder (4×4×4 grid).
 */
public class TunneledBlockEntityRenderer implements BlockEntityRenderer<TunneledBlockEntity, TunneledBlockEntityRenderer.TunneledRenderState> {

    public TunneledBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
    }

    @Override
    public TunneledRenderState createRenderState() {
        return new TunneledRenderState();
    }

    @Override
    public void updateRenderState(TunneledBlockEntity entity, TunneledRenderState state, float tickDelta,
                                  Vec3d cameraPos, ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay) {
        BlockEntityRenderState.updateBlockEntityRenderState(entity, state, crumblingOverlay);
        state.subVoxels = entity.getSubVoxels();
        state.originalBlockState = entity.getOriginalBlockState();
    }

    @Override
    public void render(TunneledRenderState state, MatrixStack matrices,
                       OrderedRenderCommandQueue renderCommands, CameraRenderState cameraState) {
        if (state.originalBlockState == null) return;

        for (int z = 0; z < 4; z++) {
            for (int y = 0; y < 4; y++) {
                for (int x = 0; x < 4; x++) {
                    int index = TunneledBlockEntity.subVoxelIndex(x, y, z);
                    if ((state.subVoxels & (1L << index)) != 0) {
                        matrices.push();
                        matrices.translate(x * 0.25, y * 0.25, z * 0.25);
                        matrices.scale(0.25f, 0.25f, 0.25f);
                        renderCommands.submitBlock(matrices, state.originalBlockState,
                                state.lightmapCoordinates, 0, 0);
                        matrices.pop();
                    }
                }
            }
        }
    }

    /**
     * Render state: sub-voxel bitmask ve orijinal blok bilgisini taşır.
     */
    public static class TunneledRenderState extends BlockEntityRenderState {
        public long subVoxels;
        public BlockState originalBlockState;
    }
}
