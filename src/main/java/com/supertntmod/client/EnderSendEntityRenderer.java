package com.supertntmod.client;

import com.supertntmod.entity.EnderSendEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Ender Send renderer: Obsidyen + End Stone bloklardan oluşan dev yapı.
 * Basit blok-tabanlı render — 20 blok yüksekliğinde bir yapı.
 * Gövde: Obsidyen, Kafa: End Stone (Enderman benzeri görünüm).
 */
@Environment(EnvType.CLIENT)
public class EnderSendEntityRenderer extends EntityRenderer<EnderSendEntity, EntityRenderState> {

    public EnderSendEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public void updateRenderState(EnderSendEntity entity, EntityRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
    }

    @Override
    public void render(EntityRenderState state, MatrixStack matrices,
                       OrderedRenderCommandQueue renderCommands, CameraRenderState cameraState) {
        matrices.push();

        // Gövde: obsidyen bloklardan dikey sütun (Y=0 ile Y=14 arası, 15 blok)
        for (int y = 0; y < 15; y++) {
            matrices.push();
            matrices.translate(-0.5, y, -0.5);
            renderCommands.submitBlock(matrices, Blocks.OBSIDIAN.getDefaultState(), state.light, 0, 0);
            matrices.pop();
        }

        // 8 bacak: her köşeden 2'şer çıkıntı (2 blok genişliğinde)
        int[][] legOffsets = {
                {-2, 0}, {2, 0}, {-2, -1}, {2, -1},
                {0, -2}, {0, 2}, {-1, -2}, {-1, 2}
        };
        for (int[] offset : legOffsets) {
            matrices.push();
            matrices.translate(-0.5 + offset[0], 0, -0.5 + offset[1]);
            renderCommands.submitBlock(matrices, Blocks.CRYING_OBSIDIAN.getDefaultState(), state.light, 0, 0);
            matrices.pop();
        }

        // Kafa: End Stone (5 blok — Y=15 ile Y=19 arası)
        // 3x5x3 boyutunda kafa
        for (int y = 15; y < 20; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    matrices.push();
                    matrices.translate(-0.5 + dx, y, -0.5 + dz);
                    renderCommands.submitBlock(matrices, Blocks.END_STONE.getDefaultState(), state.light, 0, 0);
                    matrices.pop();
                }
            }
        }

        // Gözler: Ender göz efekti (kırmızı/mor bloklar)
        // Kafanın ön yüzünde 2 blok
        matrices.push();
        matrices.translate(-1.5, 17, -1.5);
        renderCommands.submitBlock(matrices, Blocks.MAGENTA_STAINED_GLASS.getDefaultState(), state.light, 0, 0);
        matrices.pop();
        matrices.push();
        matrices.translate(0.5, 17, -1.5);
        renderCommands.submitBlock(matrices, Blocks.MAGENTA_STAINED_GLASS.getDefaultState(), state.light, 0, 0);
        matrices.pop();

        matrices.pop();
        super.render(state, matrices, renderCommands, cameraState);
    }
}
