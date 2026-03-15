package com.supertntmod.entity;

import net.minecraft.block.Block;
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
 * Tıpkı bir lego kutusu devrilmiş gibi!
 */
public class LegoTntEntity extends TntEntity {
    private static final int RADIUS = 12;
    private boolean done = false;

    // 16 renk beton bloğu (lego parçaları)
    private static final Block[] CONCRETE_COLORS = {
            Blocks.WHITE_CONCRETE,
            Blocks.ORANGE_CONCRETE,
            Blocks.MAGENTA_CONCRETE,
            Blocks.LIGHT_BLUE_CONCRETE,
            Blocks.YELLOW_CONCRETE,
            Blocks.LIME_CONCRETE,
            Blocks.PINK_CONCRETE,
            Blocks.GRAY_CONCRETE,
            Blocks.LIGHT_GRAY_CONCRETE,
            Blocks.CYAN_CONCRETE,
            Blocks.PURPLE_CONCRETE,
            Blocks.BLUE_CONCRETE,
            Blocks.BROWN_CONCRETE,
            Blocks.GREEN_CONCRETE,
            Blocks.RED_CONCRETE,
            Blocks.BLACK_CONCRETE
    };

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
                base = base.up(); // Yüzeyin bir üstüne

                Block color1 = CONCRETE_COLORS[world.random.nextInt(CONCRETE_COLORS.length)];
                Block color2 = CONCRETE_COLORS[world.random.nextInt(CONCRETE_COLORS.length)];

                int structureType = world.random.nextInt(5);
                switch (structureType) {
                    case 0 -> buildTower(world, base, color1, color2);    // Kule
                    case 1 -> buildWall(world, base, color1, color2);     // Duvar
                    case 2 -> buildStairs(world, base, color1, color2);   // Merdiven
                    case 3 -> buildArch(world, base, color1, color2);     // Kemer
                    case 4 -> buildPyramid(world, base, color1, color2);  // Piramit
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

    private void placeIfAir(World world, BlockPos pos, Block block) {
        if (world.getBlockState(pos).isOf(Blocks.AIR)) {
            world.setBlockState(pos, block.getDefaultState());
        }
    }

    // Kule: 3-6 blok yüksekliğinde, çizgili renk
    private void buildTower(World world, BlockPos base, Block c1, Block c2) {
        int height = 3 + world.random.nextInt(4);
        for (int y = 0; y < height; y++) {
            placeIfAir(world, base.up(y), y % 2 == 0 ? c1 : c2);
        }
    }

    // Duvar: 3-5 blok geniş, 2-3 yüksek
    private void buildWall(World world, BlockPos base, Block c1, Block c2) {
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

    // Merdiven: basamaklı yapı
    private void buildStairs(World world, BlockPos base, Block c1, Block c2) {
        int steps = 3 + world.random.nextInt(3);
        boolean xAxis = world.random.nextBoolean();
        for (int s = 0; s < steps; s++) {
            for (int h = 0; h <= s; h++) {
                BlockPos pos = xAxis ? base.add(s, h, 0) : base.add(0, h, s);
                placeIfAir(world, pos, s % 2 == 0 ? c1 : c2);
            }
        }
    }

    // Kemer: ∩ şeklinde yapı
    private void buildArch(World world, BlockPos base, Block c1, Block c2) {
        int height = 3 + world.random.nextInt(2);
        boolean xAxis = world.random.nextBoolean();
        // Sol sütun
        for (int h = 0; h < height; h++) {
            placeIfAir(world, base.up(h), c1);
        }
        // Sağ sütun (3 blok ileride)
        BlockPos right = xAxis ? base.add(3, 0, 0) : base.add(0, 0, 3);
        for (int h = 0; h < height; h++) {
            placeIfAir(world, right.up(h), c1);
        }
        // Üst kiriş
        for (int w = 0; w <= 3; w++) {
            BlockPos top = xAxis ? base.add(w, height, 0) : base.add(0, height, w);
            placeIfAir(world, top, c2);
        }
    }

    // Mini piramit
    private void buildPyramid(World world, BlockPos base, Block c1, Block c2) {
        int size = 2 + world.random.nextInt(2);
        for (int layer = 0; layer < size; layer++) {
            int extent = size - layer - 1;
            Block color = layer % 2 == 0 ? c1 : c2;
            for (int dx = -extent; dx <= extent; dx++) {
                for (int dz = -extent; dz <= extent; dz++) {
                    placeIfAir(world, base.add(dx, layer, dz), color);
                }
            }
        }
    }
}
