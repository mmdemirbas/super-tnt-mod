package com.supertntmod.client;

import com.supertntmod.entity.HerobrineEntity;
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
 * Herobrine renderer: blok tabanlı insansı siluet.
 * 0.2 ölçek → 10 blok = 2m. Beyaz beton kafa + gri gövde + beyaz cam gözler.
 */
@Environment(EnvType.CLIENT)
public class HerobrineEntityRenderer extends EntityRenderer<HerobrineEntity, EntityRenderState> {

    public HerobrineEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public void updateRenderState(HerobrineEntity entity, EntityRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
    }

    @Override
    public void render(EntityRenderState state, MatrixStack matrices,
                       OrderedRenderCommandQueue renderCommands, CameraRenderState cameraState) {
        matrices.push();
        matrices.scale(0.2f, 0.2f, 0.2f);

        // Bacaklar (Y=0..3): sol ve sağ, ayrı
        for (int y = 0; y <= 3; y++) {
            renderBlock(matrices, renderCommands, state, -2, y, Blocks.GRAY_CONCRETE);
            renderBlock(matrices, renderCommands, state, 1, y, Blocks.GRAY_CONCRETE);
        }

        // Gövde (Y=4..7): 3 blok geniş
        for (int y = 4; y <= 7; y++) {
            for (int x = -1; x <= 1; x++) {
                renderBlock(matrices, renderCommands, state, x, y, Blocks.GRAY_CONCRETE);
            }
        }

        // Kollar (Y=4..6): her iki yanda birer blok, deri rengi (beyaz beton)
        for (int y = 4; y <= 6; y++) {
            renderBlock(matrices, renderCommands, state, -3, y, Blocks.WHITE_CONCRETE);
            renderBlock(matrices, renderCommands, state, 2, y, Blocks.WHITE_CONCRETE);
        }

        // Kafa (Y=8..9): 3 blok geniş, 2 blok yüksek
        for (int y = 8; y <= 9; y++) {
            for (int x = -1; x <= 1; x++) {
                // Üst satırda orta boşluk bırak (kafa konturu)
                boolean isEye = (y == 9) && (x == -1 || x == 1);
                net.minecraft.block.Block block = isEye
                        ? Blocks.WHITE_STAINED_GLASS   // Herobrine'in beyaz gözleri
                        : Blocks.WHITE_CONCRETE;
                renderBlock(matrices, renderCommands, state, x, y, block);
            }
        }

        matrices.pop();
        super.render(state, matrices, renderCommands, cameraState);
    }

    private void renderBlock(MatrixStack matrices, OrderedRenderCommandQueue renderCommands,
                             EntityRenderState state, int x, int y, net.minecraft.block.Block block) {
        matrices.push();
        matrices.translate(x - 0.5, y, -0.5);
        renderCommands.submitBlock(matrices, block.getDefaultState(), state.light, 0, 0);
        matrices.pop();
    }
}
