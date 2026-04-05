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
 * Rainbow Dinamit: Patladığında 30 blok yakınındaki tüm blokları
 * rengarenk yün bloklarına dönüştürür.
 * Performans: İşlem birden fazla tick'e yayılır.
 */
public class RainbowTntEntity extends TntEntity {
    private static final int RADIUS = 30;
    private boolean done = false;

    // Kademeli işleme durumu
    private boolean processing = false;
    private BlockPos center;
    private int idx = 0;
    private static final int MODIFICATIONS_PER_TICK = 1000;

    // 16 renk yün bloğu
    private static final Block[] WOOL_COLORS = {
            Blocks.WHITE_WOOL,
            Blocks.ORANGE_WOOL,
            Blocks.MAGENTA_WOOL,
            Blocks.LIGHT_BLUE_WOOL,
            Blocks.YELLOW_WOOL,
            Blocks.LIME_WOOL,
            Blocks.PINK_WOOL,
            Blocks.GRAY_WOOL,
            Blocks.LIGHT_GRAY_WOOL,
            Blocks.CYAN_WOOL,
            Blocks.PURPLE_WOOL,
            Blocks.BLUE_WOOL,
            Blocks.BROWN_WOOL,
            Blocks.GREEN_WOOL,
            Blocks.RED_WOOL,
            Blocks.BLACK_WOOL
    };

    public RainbowTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public RainbowTntEntity(World world, double x, double y, double z,
                            @Nullable LivingEntity igniter) {
        super(ModEntities.RAINBOW_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    @Override
    public void tick() {
        // Kademeli blok işleme
        if (processing && !this.getEntityWorld().isClient()) {
            World world = getEntityWorld();
            int side = RADIUS * 2 + 1;
            int total = side * side * side;
            int modified = 0;

            while (idx < total && modified < MODIFICATIONS_PER_TICK) {
                int lz = idx % side - RADIUS;
                int lx = (idx / side) % side - RADIUS;
                int ly = (idx / (side * side)) - RADIUS;
                idx++;

                BlockPos pos = center.add(lx, ly, lz);
                if (!pos.isWithinDistance(center, RADIUS)) continue;

                var state = world.getBlockState(pos);

                if (state.isOf(Blocks.AIR) || state.isOf(Blocks.BEDROCK) ||
                    state.isOf(Blocks.WATER) || state.isOf(Blocks.LAVA) ||
                    !state.isSolidBlock(world, pos)) continue;

                Block wool = WOOL_COLORS[world.random.nextInt(WOOL_COLORS.length)];
                world.setBlockState(pos, wool.getDefaultState());
                modified++;
            }

            if (idx >= total) {
                processing = false;

                // Patlama efekti
                world.createExplosion(null, center.getX() + 0.5, center.getY(),
                        center.getZ() + 0.5, 0.0f, false, World.ExplosionSourceType.NONE);

                // Gökkuşağı partikülleri
                if (world instanceof ServerWorld serverWorld) {
                    serverWorld.spawnParticles(ParticleTypes.FIREWORK,
                            center.getX() + 0.5, center.getY() + 5, center.getZ() + 0.5,
                            300, 8.0, 5.0, 8.0, 0.5);
                    serverWorld.spawnParticles(ParticleTypes.END_ROD,
                            center.getX() + 0.5, center.getY() + 3, center.getZ() + 0.5,
                            100, 6.0, 4.0, 6.0, 0.1);
                }

                world.playSound(null, center.getX(), center.getY(), center.getZ(),
                        SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.BLOCKS, 2.0f, 1.0f);
                world.playSound(null, center.getX(), center.getY(), center.getZ(),
                        SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE, SoundCategory.BLOCKS, 1.5f, 1.2f);
                this.discard();
            }
            return;
        }

        if (!done && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            done = true;
            center = this.getBlockPos();
            processing = true;
            idx = 0;
            World world = getEntityWorld();
            world.playSound(null, center.getX(), center.getY(), center.getZ(),
                    SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.BLOCKS, 2.0f, 1.0f);
            return;
        }
        if (!done) super.tick();
    }
}
