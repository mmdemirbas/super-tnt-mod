package com.supertntmod.client;

import com.supertntmod.block.TunneledBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BlockRenderLayers;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.List;

/**
 * TunneledBlock renderer: her sub-voxel'in yüzeyini orijinal bloğun
 * dokusunun doğru bölümüyle render eder.
 * UV koordinatları sub-voxel'in blok içindeki konumuna göre hesaplanır.
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

        BlockStateModel model = MinecraftClient.getInstance()
                .getBlockRenderManager().getModel(state.originalBlockState);
        Sprite[] sprites = getFaceSprites(model);
        RenderLayer renderLayer = BlockRenderLayers.getEntityBlockLayer(state.originalBlockState);

        long subVoxels = state.subVoxels;
        int light = state.lightmapCoordinates;

        renderCommands.submitCustom(matrices, renderLayer, (entry, vc) -> {
            for (int z = 0; z < 4; z++) {
                for (int y = 0; y < 4; y++) {
                    for (int x = 0; x < 4; x++) {
                        if ((subVoxels & (1L << TunneledBlockEntity.subVoxelIndex(x, y, z))) == 0) continue;

                        float x0 = x * 0.25f, x1 = x0 + 0.25f;
                        float y0 = y * 0.25f, y1 = y0 + 0.25f;
                        float z0 = z * 0.25f, z1 = z0 + 0.25f;

                        // DOWN (-Y): frame_u = px, frame_v = 1 - pz
                        if (!isFilledAt(subVoxels, x, y - 1, z)) {
                            Sprite sp = sprites[Direction.DOWN.ordinal()];
                            vc.vertex(entry, x0, y0, z1).color(-1).texture(sp.getFrameU(x0), sp.getFrameV(1f - z1)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, -1f, 0f);
                            vc.vertex(entry, x0, y0, z0).color(-1).texture(sp.getFrameU(x0), sp.getFrameV(1f - z0)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, -1f, 0f);
                            vc.vertex(entry, x1, y0, z0).color(-1).texture(sp.getFrameU(x1), sp.getFrameV(1f - z0)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, -1f, 0f);
                            vc.vertex(entry, x1, y0, z1).color(-1).texture(sp.getFrameU(x1), sp.getFrameV(1f - z1)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, -1f, 0f);
                        }
                        // UP (+Y): frame_u = px, frame_v = pz
                        if (!isFilledAt(subVoxels, x, y + 1, z)) {
                            Sprite sp = sprites[Direction.UP.ordinal()];
                            vc.vertex(entry, x0, y1, z0).color(-1).texture(sp.getFrameU(x0), sp.getFrameV(z0)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, 1f, 0f);
                            vc.vertex(entry, x0, y1, z1).color(-1).texture(sp.getFrameU(x0), sp.getFrameV(z1)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, 1f, 0f);
                            vc.vertex(entry, x1, y1, z1).color(-1).texture(sp.getFrameU(x1), sp.getFrameV(z1)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, 1f, 0f);
                            vc.vertex(entry, x1, y1, z0).color(-1).texture(sp.getFrameU(x1), sp.getFrameV(z0)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, 1f, 0f);
                        }
                        // NORTH (-Z): frame_u = 1 - px, frame_v = 1 - py
                        if (!isFilledAt(subVoxels, x, y, z - 1)) {
                            Sprite sp = sprites[Direction.NORTH.ordinal()];
                            vc.vertex(entry, x1, y1, z0).color(-1).texture(sp.getFrameU(1f - x1), sp.getFrameV(1f - y1)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, 0f, -1f);
                            vc.vertex(entry, x1, y0, z0).color(-1).texture(sp.getFrameU(1f - x1), sp.getFrameV(1f - y0)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, 0f, -1f);
                            vc.vertex(entry, x0, y0, z0).color(-1).texture(sp.getFrameU(1f - x0), sp.getFrameV(1f - y0)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, 0f, -1f);
                            vc.vertex(entry, x0, y1, z0).color(-1).texture(sp.getFrameU(1f - x0), sp.getFrameV(1f - y1)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, 0f, -1f);
                        }
                        // SOUTH (+Z): frame_u = px, frame_v = 1 - py
                        if (!isFilledAt(subVoxels, x, y, z + 1)) {
                            Sprite sp = sprites[Direction.SOUTH.ordinal()];
                            vc.vertex(entry, x0, y1, z1).color(-1).texture(sp.getFrameU(x0), sp.getFrameV(1f - y1)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, 0f, 1f);
                            vc.vertex(entry, x0, y0, z1).color(-1).texture(sp.getFrameU(x0), sp.getFrameV(1f - y0)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, 0f, 1f);
                            vc.vertex(entry, x1, y0, z1).color(-1).texture(sp.getFrameU(x1), sp.getFrameV(1f - y0)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, 0f, 1f);
                            vc.vertex(entry, x1, y1, z1).color(-1).texture(sp.getFrameU(x1), sp.getFrameV(1f - y1)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, 0f, 1f);
                        }
                        // WEST (-X): frame_u = pz, frame_v = 1 - py
                        if (!isFilledAt(subVoxels, x - 1, y, z)) {
                            Sprite sp = sprites[Direction.WEST.ordinal()];
                            vc.vertex(entry, x0, y1, z0).color(-1).texture(sp.getFrameU(z0), sp.getFrameV(1f - y1)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, -1f, 0f, 0f);
                            vc.vertex(entry, x0, y0, z0).color(-1).texture(sp.getFrameU(z0), sp.getFrameV(1f - y0)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, -1f, 0f, 0f);
                            vc.vertex(entry, x0, y0, z1).color(-1).texture(sp.getFrameU(z1), sp.getFrameV(1f - y0)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, -1f, 0f, 0f);
                            vc.vertex(entry, x0, y1, z1).color(-1).texture(sp.getFrameU(z1), sp.getFrameV(1f - y1)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, -1f, 0f, 0f);
                        }
                        // EAST (+X): frame_u = 1 - pz, frame_v = 1 - py
                        if (!isFilledAt(subVoxels, x + 1, y, z)) {
                            Sprite sp = sprites[Direction.EAST.ordinal()];
                            vc.vertex(entry, x1, y1, z1).color(-1).texture(sp.getFrameU(1f - z1), sp.getFrameV(1f - y1)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 1f, 0f, 0f);
                            vc.vertex(entry, x1, y0, z1).color(-1).texture(sp.getFrameU(1f - z1), sp.getFrameV(1f - y0)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 1f, 0f, 0f);
                            vc.vertex(entry, x1, y0, z0).color(-1).texture(sp.getFrameU(1f - z0), sp.getFrameV(1f - y0)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 1f, 0f, 0f);
                            vc.vertex(entry, x1, y1, z0).color(-1).texture(sp.getFrameU(1f - z0), sp.getFrameV(1f - y1)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 1f, 0f, 0f);
                        }
                    }
                }
            }
        });
    }

    /**
     * (x,y,z) koordinatındaki sub-voxel dolu mu? Sınır dışıysa false döner.
     */
    private static boolean isFilledAt(long subVoxels, int x, int y, int z) {
        if (x < 0 || x >= 4 || y < 0 || y >= 4 || z < 0 || z >= 4) return false;
        return (subVoxels & (1L << TunneledBlockEntity.subVoxelIndex(x, y, z))) != 0;
    }

    /**
     * Her yön için blok modelinden sprite alır; quad yoksa particle sprite'a düşer.
     */
    private static Sprite[] getFaceSprites(BlockStateModel model) {
        List<BlockModelPart> parts = model.getParts(Random.create(0));
        Sprite fallback = model.particleSprite();
        Direction[] directions = Direction.values();
        Sprite[] sprites = new Sprite[directions.length];
        for (Direction dir : directions) {
            sprites[dir.ordinal()] = fallback;
            for (BlockModelPart part : parts) {
                List<BakedQuad> quads = part.getQuads(dir);
                if (!quads.isEmpty()) {
                    sprites[dir.ordinal()] = quads.get(0).sprite();
                    break;
                }
            }
        }
        return sprites;
    }

    /**
     * Render state: sub-voxel bitmask ve orijinal blok bilgisini taşır.
     */
    public static class TunneledRenderState extends BlockEntityRenderState {
        public long subVoxels;
        public BlockState originalBlockState;
    }
}
