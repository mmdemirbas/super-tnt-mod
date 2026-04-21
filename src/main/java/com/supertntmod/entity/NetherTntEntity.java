package com.supertntmod.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Nether TNT entity: havada netherrack adası + 15 nether portalı inşa eder.
 * Çoklu-tick işleme ile lag engellenir.
 */
public class NetherTntEntity extends TntEntity {
    private boolean done = false;
    private final List<BlockPos[]> pendingPortals = new ArrayList<>();
    private final List<BlockPos> pendingIsland = new ArrayList<>();
    private int buildTick = 0;
    private static final int BLOCKS_PER_TICK = 50;

    public NetherTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public NetherTntEntity(World world, double x, double y, double z,
                           @Nullable LivingEntity igniter) {
        super(ModEntities.NETHER_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    @Override
    public void tick() {
        if (!done && this.getFuse() <= 1 && !getEntityWorld().isClient()) {
            done = true;
            World world = getEntityWorld();
            double cx = getX(), cy = getY() + 20, cz = getZ();
            discard();

            world.playSound(null, cx, cy, cz,
                    SoundEvents.BLOCK_PORTAL_TRIGGER, SoundCategory.BLOCKS, 2.0f, 0.5f);

            // Ada: 30x4x30 netherrack
            int r = 15;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz <= r * r) {
                        for (int dy = 0; dy < 4; dy++) {
                            pendingIsland.add(new BlockPos(
                                    (int) cx + dx, (int) cy + dy, (int) cz + dz));
                        }
                    }
                }
            }

            // 15 nether portalı (5×3 düzen, adanın üstünde)
            int portalY = (int) cy + 4;
            int[][] slots = {
                    {-24, -6}, {-18, -6}, {-12, -6}, {-6, -6}, {0, -6},
                    {-24, 0},  {-18, 0},  {-12, 0},  {-6, 0},  {0, 0},
                    {-24, 6},  {-18, 6},  {-12, 6},  {-6, 6},  {0, 6}
            };
            for (int[] slot : slots) {
                pendingPortals.add(buildPortalBlocks(
                        (int) cx + slot[0], portalY, (int) cz + slot[1]));
            }
            return;
        }

        // Çoklu-tick inşaat
        if (!pendingIsland.isEmpty() || !pendingPortals.isEmpty()) {
            World world = getEntityWorld();
            if (world.isClient()) { if (!done) super.tick(); return; }

            int count = 0;
            while (!pendingIsland.isEmpty() && count < BLOCKS_PER_TICK) {
                BlockPos p = pendingIsland.remove(pendingIsland.size() - 1);
                world.setBlockState(p, Blocks.NETHERRACK.getDefaultState());
                count++;
            }
            while (!pendingPortals.isEmpty() && count < BLOCKS_PER_TICK) {
                BlockPos[] portal = pendingPortals.remove(pendingPortals.size() - 1);
                placePortal(world, portal);
                count += portal.length;
            }

            if (!pendingIsland.isEmpty() || !pendingPortals.isEmpty()) {
                buildTick++;
                return;
            }
        }

        if (!done) super.tick();
    }

    /** 4-geniş × 5-yüksek portal çerçevesi konumlarını döndürür. */
    private BlockPos[] buildPortalBlocks(int x, int y, int z) {
        List<BlockPos> blocks = new ArrayList<>();
        // Obsidian çerçeve (4 geniş × 5 yüksek, Z ekseninde)
        for (int dx = 0; dx < 4; dx++) {
            for (int dy = 0; dy < 5; dy++) {
                boolean frame = dx == 0 || dx == 3 || dy == 0 || dy == 4;
                if (frame) {
                    blocks.add(new BlockPos(x + dx, y + dy, z));
                }
            }
        }
        // Portal blokları (2 geniş × 3 yüksek, iç kısım)
        for (int dx = 1; dx <= 2; dx++) {
            for (int dy = 1; dy <= 3; dy++) {
                blocks.add(new BlockPos(x + dx, y + dy, z));
            }
        }
        return blocks.toArray(new BlockPos[0]);
    }

    private void placePortal(World world, BlockPos[] blocks) {
        // Çerçeve obsidian, son 6 portal bloğu
        int total = blocks.length;
        int portalStart = total - 6;
        for (int i = 0; i < total; i++) {
            if (i < portalStart) {
                world.setBlockState(blocks[i], Blocks.OBSIDIAN.getDefaultState());
            } else {
                world.setBlockState(blocks[i],
                        Blocks.NETHER_PORTAL.getDefaultState()
                                .with(NetherPortalBlock.AXIS, Direction.Axis.X));
            }
        }
    }
}
