package com.supertntmod.entity;

import com.supertntmod.block.LegoBrickBlock;
import com.supertntmod.block.ModBlocks;
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
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Lego TNT: Patladığında 12 blok yarıçapında renkli lego yapıları inşa eder.
 * Rastgele kuleler, duvarlar ve merdivenler oluşturur.
 * Tüm bloklar üzerinde lego çıkıntıları olan gerçek lego tuğlalarıdır!
 */
public class LegoTntEntity extends TntEntity {
    private static final int RADIUS = 12;
    private boolean done = false;

    public LegoTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public LegoTntEntity(World world, double x, double y, double z,
                         @Nullable LivingEntity igniter) {
        super(ModEntities.LEGO_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    /** Rastgele renkli lego tuğlası BlockState döndürür */
    private BlockState randomLegoBrick(World world) {
        int color = world.random.nextInt(16);
        return ModBlocks.LEGO_BRICK.getDefaultState().with(LegoBrickBlock.COLOR, color);
    }

    @Override
    public void tick() {
        if (!done && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            done = true;
            BlockPos center = this.getBlockPos();
            World world = getEntityWorld();
            this.discard();

            // Lego kutusu dökülme sesi
            world.playSound(null, center.getX(), center.getY(), center.getZ(),
                    SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.BLOCKS, 2.0f, 1.5f);
            world.playSound(null, center.getX(), center.getY(), center.getZ(),
                    SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, SoundCategory.BLOCKS, 2.0f, 0.8f);

            // Küçük patlama efekti
            world.createExplosion(null, center.getX() + 0.5, center.getY(),
                    center.getZ() + 0.5, 3.0f, true, World.ExplosionSourceType.TNT);

            // Yüzeyi bul
            int surfaceY = center.getY();

            // 15-25 adet rastgele lego yapısı oluştur
            int structureCount = 15 + world.random.nextInt(11);
            for (int i = 0; i < structureCount; i++) {
                int ox = center.getX() + (int) ((world.random.nextDouble() - 0.5) * RADIUS * 2);
                int oz = center.getZ() + (int) ((world.random.nextDouble() - 0.5) * RADIUS * 2);
                BlockPos base = new BlockPos(ox, surfaceY, oz);

                // Yüzeye oturt
                while (world.getBlockState(base).isOf(Blocks.AIR) && base.getY() > surfaceY - 5) {
                    base = base.down();
                }
                base = base.up();

                int c1 = world.random.nextInt(16);
                int c2 = world.random.nextInt(16);

                int structureType = world.random.nextInt(5);
                switch (structureType) {
                    case 0 -> buildTower(world, base, c1, c2);
                    case 1 -> buildWall(world, base, c1, c2);
                    case 2 -> buildStairs(world, base, c1, c2);
                    case 3 -> buildArch(world, base, c1, c2);
                    case 4 -> buildPyramid(world, base, c1, c2);
                }
            }

            // Renkli partiküller
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.FIREWORK,
                        center.getX() + 0.5, center.getY() + 3, center.getZ() + 0.5,
                        200, 6.0, 4.0, 6.0, 0.3);
                serverWorld.spawnParticles(ParticleTypes.END_ROD,
                        center.getX() + 0.5, center.getY() + 5, center.getZ() + 0.5,
                        50, 4.0, 3.0, 4.0, 0.05);
            }

            return;
        }
        if (!done) super.tick();
    }

    private BlockState legoColor(int color) {
        return ModBlocks.LEGO_BRICK.getDefaultState().with(LegoBrickBlock.COLOR, color);
    }

    private void placeIfAir(World world, BlockPos pos, int color) {
        if (world.getBlockState(pos).isOf(Blocks.AIR)) {
            world.setBlockState(pos, legoColor(color));
        }
    }

    private void buildTower(World world, BlockPos base, int c1, int c2) {
        int height = 3 + world.random.nextInt(4);
        for (int y = 0; y < height; y++) {
            placeIfAir(world, base.up(y), y % 2 == 0 ? c1 : c2);
        }
    }

    private void buildWall(World world, BlockPos base, int c1, int c2) {
        int width = 3 + world.random.nextInt(3);
        int height = 2 + world.random.nextInt(2);
        boolean xAxis = world.random.nextBoolean();
        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                BlockPos pos = xAxis ? base.add(w, h, 0) : base.add(0, h, w);
                placeIfAir(world, pos, (w + h) % 2 == 0 ? c1 : c2);
            }
        }
    }

    private void buildStairs(World world, BlockPos base, int c1, int c2) {
        int steps = 3 + world.random.nextInt(3);
        boolean xAxis = world.random.nextBoolean();
        for (int s = 0; s < steps; s++) {
            for (int h = 0; h <= s; h++) {
                BlockPos pos = xAxis ? base.add(s, h, 0) : base.add(0, h, s);
                placeIfAir(world, pos, s % 2 == 0 ? c1 : c2);
            }
        }
    }

    private void buildArch(World world, BlockPos base, int c1, int c2) {
        int height = 3 + world.random.nextInt(2);
        boolean xAxis = world.random.nextBoolean();
        for (int h = 0; h < height; h++) {
            placeIfAir(world, base.up(h), c1);
        }
        BlockPos right = xAxis ? base.add(3, 0, 0) : base.add(0, 0, 3);
        for (int h = 0; h < height; h++) {
            placeIfAir(world, right.up(h), c1);
        }
        for (int w = 0; w <= 3; w++) {
            BlockPos top = xAxis ? base.add(w, height, 0) : base.add(0, height, w);
            placeIfAir(world, top, c2);
        }
    }

    private void buildPyramid(World world, BlockPos base, int c1, int c2) {
        int size = 2 + world.random.nextInt(2);
        for (int layer = 0; layer < size; layer++) {
            int extent = size - layer - 1;
            int color = layer % 2 == 0 ? c1 : c2;
            for (int dx = -extent; dx <= extent; dx++) {
                for (int dz = -extent; dz <= extent; dz++) {
                    placeIfAir(world, base.add(dx, layer, dz), color);
                }
            }
        }
    }
}
