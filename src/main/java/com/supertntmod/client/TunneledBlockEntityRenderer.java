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
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TunneledBlock renderer: her sub-voxel'in yüzeyini, o sub-voxel'e atanmış
 * blok kimliğinin dokusunun doğru bölümüyle render eder.
 * UV koordinatları sub-voxel'in blok içindeki konumuna göre hesaplanır.
 * Birden fazla blok türü olabilir; her tür için sprite/tint cache'lenir.
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
        state.fallbackBlockState = entity.getOriginalBlockState();
        state.spriteCache.clear();
        state.tintCache.clear();
        state.voxelStates = new BlockState[64];

        BlockPos entityPos = entity.getPos();
        BlockRenderView world = (BlockRenderView) entity.getWorld();

        // Tüm dolu sub-voxel'lerin blok state'lerini topla ve sprite/tint cache'le
        for (int z = 0; z < 4; z++) {
            for (int y = 0; y < 4; y++) {
                for (int x = 0; x < 4; x++) {
                    int idx = TunneledBlockEntity.subVoxelIndex(x, y, z);
                    if ((state.subVoxels & (1L << idx)) == 0) continue;
                    BlockState bs = entity.getBlockStateAt(x, y, z);
                    if (bs == null) bs = state.fallbackBlockState;
                    if (bs == null) continue;
                    state.voxelStates[idx] = bs;

                    Identifier blockId = Registries.BLOCK.getId(bs.getBlock());
                    if (!state.spriteCache.containsKey(blockId)) {
                        cacheBlockSprites(state, blockId, bs, entityPos, world);
                    }
                }
            }
        }
    }

    private void cacheBlockSprites(TunneledRenderState state, Identifier blockId, BlockState bs,
                                   BlockPos entityPos, BlockRenderView world) {
        MinecraftClient client = MinecraftClient.getInstance();
        BlockStateModel model = client.getBlockRenderManager().getModel(bs);
        List<BlockModelPart> parts = model.getParts(Random.create(0));
        Sprite fallback = model.particleSprite();

        Sprite[] sprites = new Sprite[Direction.values().length];
        int[] tints = new int[Direction.values().length];
        for (Direction dir : Direction.values()) {
            int i = dir.ordinal();
            sprites[i] = fallback;
            tints[i] = -1;
            for (BlockModelPart part : parts) {
                List<BakedQuad> quads = part.getQuads(dir);
                if (!quads.isEmpty()) {
                    BakedQuad quad = quads.get(0);
                    sprites[i] = quad.sprite();
                    int tintIndex = quad.tintIndex();
                    if (tintIndex >= 0) {
                        tints[i] = client.getBlockColors().getColor(bs, world, entityPos, tintIndex);
                    }
                    break;
                }
            }
        }
        state.spriteCache.put(blockId, sprites);
        state.tintCache.put(blockId, tints);
    }

    @Override
    public void render(TunneledRenderState state, MatrixStack matrices,
                       OrderedRenderCommandQueue renderCommands, CameraRenderState cameraState) {
        if (state.voxelStates == null) return;

        long subVoxels = state.subVoxels;
        int light = state.lightmapCoordinates;

        // Ortak render layer için dolu ilk voxel'in state'ini kullan; karışık katmanlar olduğunda
        // her birini kendi katmanında çiz (TunneledBlock'lar genelde tek katman)
        Map<RenderLayer, java.util.List<int[]>> layerToVoxels = new HashMap<>();
        for (int z = 0; z < 4; z++) {
            for (int y = 0; y < 4; y++) {
                for (int x = 0; x < 4; x++) {
                    int idx = TunneledBlockEntity.subVoxelIndex(x, y, z);
                    if ((subVoxels & (1L << idx)) == 0) continue;
                    BlockState bs = state.voxelStates[idx];
                    if (bs == null) continue;
                    RenderLayer layer = BlockRenderLayers.getEntityBlockLayer(bs);
                    layerToVoxels.computeIfAbsent(layer, k -> new java.util.ArrayList<>())
                            .add(new int[]{x, y, z});
                }
            }
        }

        for (Map.Entry<RenderLayer, java.util.List<int[]>> e : layerToVoxels.entrySet()) {
            RenderLayer layer = e.getKey();
            java.util.List<int[]> voxels = e.getValue();
            renderCommands.submitCustom(matrices, layer, (entry, vc) -> {
                for (int[] coord : voxels) {
                    int x = coord[0], y = coord[1], z = coord[2];
                    int idx = TunneledBlockEntity.subVoxelIndex(x, y, z);
                    BlockState bs = state.voxelStates[idx];
                    if (bs == null) continue;
                    Identifier id = Registries.BLOCK.getId(bs.getBlock());
                    Sprite[] sprites = state.spriteCache.get(id);
                    int[] tints = state.tintCache.get(id);
                    if (sprites == null || tints == null) continue;

                    float x0 = x * 0.25f, x1 = x0 + 0.25f;
                    float y0 = y * 0.25f, y1 = y0 + 0.25f;
                    float z0 = z * 0.25f, z1 = z0 + 0.25f;

                    // DOWN (-Y)
                    if (!isFilledAt(subVoxels, x, y - 1, z)) {
                        Sprite sp = sprites[Direction.DOWN.ordinal()];
                        int c = tints[Direction.DOWN.ordinal()];
                        vc.vertex(entry, x0, y0, z1).color(c).texture(sp.getFrameU(x0), sp.getFrameV(1f - z1)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, -1f, 0f);
                        vc.vertex(entry, x0, y0, z0).color(c).texture(sp.getFrameU(x0), sp.getFrameV(1f - z0)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, -1f, 0f);
                        vc.vertex(entry, x1, y0, z0).color(c).texture(sp.getFrameU(x1), sp.getFrameV(1f - z0)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, -1f, 0f);
                        vc.vertex(entry, x1, y0, z1).color(c).texture(sp.getFrameU(x1), sp.getFrameV(1f - z1)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, -1f, 0f);
                    }
                    // UP (+Y)
                    if (!isFilledAt(subVoxels, x, y + 1, z)) {
                        Sprite sp = sprites[Direction.UP.ordinal()];
                        int c = tints[Direction.UP.ordinal()];
                        vc.vertex(entry, x0, y1, z0).color(c).texture(sp.getFrameU(x0), sp.getFrameV(z0)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, 1f, 0f);
                        vc.vertex(entry, x0, y1, z1).color(c).texture(sp.getFrameU(x0), sp.getFrameV(z1)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, 1f, 0f);
                        vc.vertex(entry, x1, y1, z1).color(c).texture(sp.getFrameU(x1), sp.getFrameV(z1)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, 1f, 0f);
                        vc.vertex(entry, x1, y1, z0).color(c).texture(sp.getFrameU(x1), sp.getFrameV(z0)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, 1f, 0f);
                    }
                    // NORTH (-Z)
                    if (!isFilledAt(subVoxels, x, y, z - 1)) {
                        Sprite sp = sprites[Direction.NORTH.ordinal()];
                        int c = tints[Direction.NORTH.ordinal()];
                        vc.vertex(entry, x1, y1, z0).color(c).texture(sp.getFrameU(1f - x1), sp.getFrameV(1f - y1)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, 0f, -1f);
                        vc.vertex(entry, x1, y0, z0).color(c).texture(sp.getFrameU(1f - x1), sp.getFrameV(1f - y0)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, 0f, -1f);
                        vc.vertex(entry, x0, y0, z0).color(c).texture(sp.getFrameU(1f - x0), sp.getFrameV(1f - y0)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, 0f, -1f);
                        vc.vertex(entry, x0, y1, z0).color(c).texture(sp.getFrameU(1f - x0), sp.getFrameV(1f - y1)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, 0f, -1f);
                    }
                    // SOUTH (+Z)
                    if (!isFilledAt(subVoxels, x, y, z + 1)) {
                        Sprite sp = sprites[Direction.SOUTH.ordinal()];
                        int c = tints[Direction.SOUTH.ordinal()];
                        vc.vertex(entry, x0, y1, z1).color(c).texture(sp.getFrameU(x0), sp.getFrameV(1f - y1)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, 0f, 1f);
                        vc.vertex(entry, x0, y0, z1).color(c).texture(sp.getFrameU(x0), sp.getFrameV(1f - y0)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, 0f, 1f);
                        vc.vertex(entry, x1, y0, z1).color(c).texture(sp.getFrameU(x1), sp.getFrameV(1f - y0)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, 0f, 1f);
                        vc.vertex(entry, x1, y1, z1).color(c).texture(sp.getFrameU(x1), sp.getFrameV(1f - y1)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, 0f, 1f);
                    }
                    // WEST (-X)
                    if (!isFilledAt(subVoxels, x - 1, y, z)) {
                        Sprite sp = sprites[Direction.WEST.ordinal()];
                        int c = tints[Direction.WEST.ordinal()];
                        vc.vertex(entry, x0, y1, z0).color(c).texture(sp.getFrameU(z0), sp.getFrameV(1f - y1)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, -1f, 0f, 0f);
                        vc.vertex(entry, x0, y0, z0).color(c).texture(sp.getFrameU(z0), sp.getFrameV(1f - y0)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, -1f, 0f, 0f);
                        vc.vertex(entry, x0, y0, z1).color(c).texture(sp.getFrameU(z1), sp.getFrameV(1f - y0)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, -1f, 0f, 0f);
                        vc.vertex(entry, x0, y1, z1).color(c).texture(sp.getFrameU(z1), sp.getFrameV(1f - y1)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, -1f, 0f, 0f);
                    }
                    // EAST (+X)
                    if (!isFilledAt(subVoxels, x + 1, y, z)) {
                        Sprite sp = sprites[Direction.EAST.ordinal()];
                        int c = tints[Direction.EAST.ordinal()];
                        vc.vertex(entry, x1, y1, z1).color(c).texture(sp.getFrameU(1f - z1), sp.getFrameV(1f - y1)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 1f, 0f, 0f);
                        vc.vertex(entry, x1, y0, z1).color(c).texture(sp.getFrameU(1f - z1), sp.getFrameV(1f - y0)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 1f, 0f, 0f);
                        vc.vertex(entry, x1, y0, z0).color(c).texture(sp.getFrameU(1f - z0), sp.getFrameV(1f - y0)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 1f, 0f, 0f);
                        vc.vertex(entry, x1, y1, z0).color(c).texture(sp.getFrameU(1f - z0), sp.getFrameV(1f - y1)).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 1f, 0f, 0f);
                    }
                }
            });
        }
    }

    private static boolean isFilledAt(long subVoxels, int x, int y, int z) {
        if (x < 0 || x >= 4 || y < 0 || y >= 4 || z < 0 || z >= 4) return false;
        return (subVoxels & (1L << TunneledBlockEntity.subVoxelIndex(x, y, z))) != 0;
    }

    public static class TunneledRenderState extends BlockEntityRenderState {
        public long subVoxels;
        public BlockState fallbackBlockState;
        public BlockState[] voxelStates;
        public final Map<Identifier, Sprite[]> spriteCache = new HashMap<>();
        public final Map<Identifier, int[]> tintCache = new HashMap<>();
    }
}
