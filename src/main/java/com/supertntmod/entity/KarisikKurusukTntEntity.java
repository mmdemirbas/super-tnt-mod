package com.supertntmod.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * 🌀 Karışık Kuruşuk TNT — patladığında dünyayı ezik büzük yapar.
 * Yarıçap içindeki yüzey bloklarını rastgele yukarı/aşağı oynatır;
 * boşalan yerleri kahverengi/gri/siyah betonla doldurur. Kademeli işleme.
 */
public class KarisikKurusukTntEntity extends TntEntity {
    private static final int RADIUS = 18;
    private static final int OPS_PER_TICK = 600;

    private boolean exploded = false;
    private boolean processing = false;
    private BlockPos center;
    private int idx = 0;

    public KarisikKurusukTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public KarisikKurusukTntEntity(World world, double x, double y, double z, @Nullable LivingEntity igniter) {
        super(ModEntities.KARISIK_KURUSUK_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    @Override
    public void tick() {
        if (!exploded && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            exploded = true;
            center = this.getBlockPos();
            World world = getEntityWorld();
            double cx = center.getX() + 0.5, cy = center.getY(), cz = center.getZ() + 0.5;

            world.playSound(null, cx, cy, cz,
                    SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 2.0f, 0.5f);

            if (world instanceof ServerWorld sw) {
                sw.spawnParticles(ParticleTypes.LARGE_SMOKE,
                        cx, cy + 2, cz, 200, 6.0, 4.0, 6.0, 0.4);
            }

            processing = true;
            idx = 0;
            return;
        }

        if (processing && !this.getEntityWorld().isClient()) {
            World world = getEntityWorld();
            int diameter = RADIUS * 2 + 1;
            int total = diameter * diameter;
            int processed = 0;

            while (idx < total && processed < OPS_PER_TICK) {
                int dx = (idx % diameter) - RADIUS;
                int dz = (idx / diameter) - RADIUS;
                idx++;

                if (dx * dx + dz * dz > RADIUS * RADIUS) continue;

                int surfaceY = findSurfaceY(world, center.getX() + dx, center.getZ() + dz);
                if (surfaceY == Integer.MIN_VALUE) continue;

                int delta = world.random.nextInt(7) - 3;
                if (delta == 0) continue;

                BlockState fill = pickRoughBlock(world);

                if (delta > 0) {
                    for (int dy = 1; dy <= delta; dy++) {
                        BlockPos target = new BlockPos(center.getX() + dx, surfaceY + dy, center.getZ() + dz);
                        if (world.getBlockState(target).isAir()) {
                            world.setBlockState(target, fill, 2);
                        }
                    }
                } else {
                    for (int dy = 0; dy > delta; dy--) {
                        BlockPos target = new BlockPos(center.getX() + dx, surfaceY + dy, center.getZ() + dz);
                        BlockState s = world.getBlockState(target);
                        if (!s.isAir() && !s.isOf(Blocks.BEDROCK)) {
                            world.setBlockState(target, Blocks.AIR.getDefaultState(), 2);
                        }
                    }
                }
                processed++;
            }

            if (idx >= total) {
                processing = false;
                this.discard();
            }
            return;
        }

        if (!exploded) super.tick();
    }

    private static int findSurfaceY(World world, int x, int z) {
        int y = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z) - 1;
        BlockState s = world.getBlockState(new BlockPos(x, y, z));
        if (s.isAir() || s.isOf(Blocks.WATER) || s.isOf(Blocks.LAVA)) {
            for (int yy = y - 1; yy > y - 10; yy--) {
                BlockState ss = world.getBlockState(new BlockPos(x, yy, z));
                if (!ss.isAir() && !ss.isOf(Blocks.WATER) && !ss.isOf(Blocks.LAVA)) return yy;
            }
            return Integer.MIN_VALUE;
        }
        return y;
    }

    private static BlockState pickRoughBlock(World world) {
        int r = world.random.nextInt(3);
        return switch (r) {
            case 0 -> Blocks.BLACK_CONCRETE.getDefaultState();
            case 1 -> Blocks.BROWN_CONCRETE.getDefaultState();
            default -> Blocks.GRAY_CONCRETE.getDefaultState();
        };
    }
}
