package com.supertntmod.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.EndPortalFrameBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * End TNT entity: havada end taşı adası + 2 açık End portalı inşa eder.
 * Çoklu-tick işleme.
 */
public class EndTntEntity extends TntEntity {
    private boolean done = false;
    private final List<BlockPos> pendingIsland = new ArrayList<>();
    private final List<BlockPos[]> pendingPortals = new ArrayList<>();
    private static final int BLOCKS_PER_TICK = 50;

    public EndTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public EndTntEntity(World world, double x, double y, double z,
                        @Nullable LivingEntity igniter) {
        super(ModEntities.END_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    @Override
    public void tick() {
        if (!done && this.getFuse() <= 1 && !getEntityWorld().isClient()) {
            done = true;
            World world = getEntityWorld();
            double cx = getX(), cy = getY() + 20, cz = getZ();

            world.playSound(null, cx, cy, cz,
                    SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.BLOCKS, 2.0f, 0.5f);

            // Ada: 20x3x20 end taşı
            int r = 10;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz <= r * r) {
                        for (int dy = 0; dy < 3; dy++) {
                            pendingIsland.add(new BlockPos(
                                    (int) cx + dx, (int) cy + dy, (int) cz + dz));
                        }
                    }
                }
            }

            // 2 End portalı
            int portalY = (int) cy + 3;
            pendingPortals.add(buildEndPortalBlocks((int) cx - 10, portalY, (int) cz - 2));
            pendingPortals.add(buildEndPortalBlocks((int) cx + 5, portalY, (int) cz - 2));
            return;
        }

        if (!pendingIsland.isEmpty() || !pendingPortals.isEmpty()) {
            World world = getEntityWorld();
            if (world.isClient()) { if (!done) super.tick(); return; }

            int count = 0;
            while (!pendingIsland.isEmpty() && count < BLOCKS_PER_TICK) {
                BlockPos p = pendingIsland.remove(pendingIsland.size() - 1);
                world.setBlockState(p, Blocks.END_STONE.getDefaultState());
                count++;
            }
            while (!pendingPortals.isEmpty() && count < BLOCKS_PER_TICK) {
                BlockPos[] portal = pendingPortals.remove(pendingPortals.size() - 1);
                placeEndPortal(world, portal);
                count += portal.length;
            }

            if (pendingIsland.isEmpty() && pendingPortals.isEmpty()) {
                this.discard();
            }
            return;
        }

        if (!done) super.tick();
    }

    /**
     * End portalı: 3×3 çerçeve (12 frame bloğu) + 9 portal bloğu.
     * Çerçeve blokları önce, sonra portal blokları.
     */
    private BlockPos[] buildEndPortalBlocks(int x, int y, int z) {
        List<BlockPos> blocks = new ArrayList<>();
        // Çerçeve: 4 kenar ortası (3'er tane)
        // Kuzey kenar (z-1)
        blocks.add(new BlockPos(x, y, z - 1));
        blocks.add(new BlockPos(x + 1, y, z - 1));
        blocks.add(new BlockPos(x + 2, y, z - 1));
        // Güney kenar (z+3)
        blocks.add(new BlockPos(x, y, z + 3));
        blocks.add(new BlockPos(x + 1, y, z + 3));
        blocks.add(new BlockPos(x + 2, y, z + 3));
        // Batı kenar (x-1)
        blocks.add(new BlockPos(x - 1, y, z));
        blocks.add(new BlockPos(x - 1, y, z + 1));
        blocks.add(new BlockPos(x - 1, y, z + 2));
        // Doğu kenar (x+3)
        blocks.add(new BlockPos(x + 3, y, z));
        blocks.add(new BlockPos(x + 3, y, z + 1));
        blocks.add(new BlockPos(x + 3, y, z + 2));
        // Portal blokları (3×3 iç)
        for (int dx = 0; dx < 3; dx++) {
            for (int dz = 0; dz < 3; dz++) {
                blocks.add(new BlockPos(x + dx, y, z + dz));
            }
        }
        return blocks.toArray(new BlockPos[0]);
    }

    private void placeEndPortal(World world, BlockPos[] blocks) {
        int frameCount = 12;
        BlockState frameState = Blocks.END_PORTAL_FRAME.getDefaultState()
                .with(EndPortalFrameBlock.EYE, true);
        for (int i = 0; i < blocks.length; i++) {
            if (i < frameCount) {
                world.setBlockState(blocks[i], frameState);
            } else {
                world.setBlockState(blocks[i], Blocks.END_PORTAL.getDefaultState());
            }
        }
    }
}
