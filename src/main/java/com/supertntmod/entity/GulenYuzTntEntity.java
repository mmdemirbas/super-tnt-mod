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
 * Gülen Yüz Çıngırak TNT: Patlatıldığında dünyayı sarıya boyar.
 * 12 blok yarıçapındaki katı blokları sarı blokların biriyle değiştirir.
 */
public class GulenYuzTntEntity extends TntEntity {
    private static final int RADIUS = 12;
    private static final int MODIFICATIONS_PER_TICK = 800;

    private boolean done = false;
    private boolean processing = false;
    private BlockPos center;
    private int idx = 0;

    private static final Block[] YELLOW_BLOCKS = {
            Blocks.YELLOW_CONCRETE,
            Blocks.YELLOW_WOOL,
            Blocks.YELLOW_TERRACOTTA,
            Blocks.YELLOW_GLAZED_TERRACOTTA,
            Blocks.GOLD_BLOCK,
            Blocks.HONEY_BLOCK,
    };

    public GulenYuzTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public GulenYuzTntEntity(World world, double x, double y, double z,
                             @Nullable LivingEntity igniter) {
        super(ModEntities.GULEN_YUZ_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    @Override
    public void tick() {
        if (processing && !this.getEntityWorld().isClient()) {
            World world = getEntityWorld();
            int side = RADIUS * 2 + 1;
            int total = side * side * side;
            int modified = 0;
            while (modified < MODIFICATIONS_PER_TICK && idx < total) {
                int lx = (idx % side) - RADIUS;
                int lz = ((idx / side) % side) - RADIUS;
                int ly = (idx / (side * side)) - RADIUS;
                idx++;

                BlockPos pos = center.add(lx, ly, lz);
                if (!pos.isWithinDistance(center, RADIUS)) continue;

                var state = world.getBlockState(pos);
                if (state.isOf(Blocks.AIR) || state.isOf(Blocks.BEDROCK)
                        || state.isOf(Blocks.WATER) || state.isOf(Blocks.LAVA)
                        || !state.isSolidBlock(world, pos)) continue;

                Block yellow = YELLOW_BLOCKS[world.random.nextInt(YELLOW_BLOCKS.length)];
                world.setBlockState(pos, yellow.getDefaultState());
                modified++;
            }
            if (idx >= total) {
                processing = false;
                this.discard();
            }
            return;
        }

        if (!done && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            done = true;
            World world = getEntityWorld();
            double x = getX(), y = getY(), z = getZ();
            center = this.getBlockPos();

            world.playSound(null, x, y, z, SoundEvents.BLOCK_BELL_USE,
                    SoundCategory.BLOCKS, 4.0f, 1.5f);
            world.playSound(null, x, y, z, SoundEvents.ENTITY_VILLAGER_CELEBRATE,
                    SoundCategory.BLOCKS, 3.0f, 1.5f);

            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                        x, y + 2, z, 300, RADIUS, 3.0, RADIUS, 0.1);
            }

            world.createExplosion(null, x, y, z, 1.0f, false, World.ExplosionSourceType.NONE);
            processing = true;
            return;
        }
        if (!done) super.tick();
    }
}
