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
 * Makarna TNT: Patladığında 15 blok yarıçapındaki katı blokları
 * yenilebilen bloklara dönüştürür. Bir İtalyan şöleni!
 * Performans: İşlem birden fazla tick'e yayılır.
 */
public class MakarnaTntEntity extends TntEntity {
    private static final int RADIUS = 15;
    private boolean done = false;

    // Kademeli işleme durumu
    private boolean processing = false;
    private BlockPos center;
    private int idx = 0;
    private static final int MODIFICATIONS_PER_TICK = 1000;

    // Yenilebilen bloklar menüsü
    private static final Block[] FOOD_BLOCKS = {
            Blocks.CAKE,
            Blocks.MELON,
            Blocks.PUMPKIN,
            Blocks.HAY_BLOCK,
            Blocks.DRIED_KELP_BLOCK,
            Blocks.BROWN_MUSHROOM_BLOCK,
            Blocks.RED_MUSHROOM_BLOCK,
            Blocks.HONEY_BLOCK,
            Blocks.HONEYCOMB_BLOCK,
            Blocks.POTATOES,
            Blocks.CARROTS,
            Blocks.WHEAT,
            Blocks.SWEET_BERRY_BUSH,
    };

    public MakarnaTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public MakarnaTntEntity(World world, double x, double y, double z,
                            @Nullable LivingEntity igniter) {
        super(ModEntities.MAKARNA_TNT, world);
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

                Block food = FOOD_BLOCKS[world.random.nextInt(FOOD_BLOCKS.length)];
                world.setBlockState(pos, food.getDefaultState());
                modified++;
            }

            if (idx >= total) {
                processing = false;

                // Buhar ve yemek partikülleri
                if (world instanceof ServerWorld serverWorld) {
                    serverWorld.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                            center.getX() + 0.5, center.getY() + 3, center.getZ() + 0.5,
                            200, 6.0, 4.0, 6.0, 0.02);
                    serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                            center.getX() + 0.5, center.getY() + 2, center.getZ() + 0.5,
                            100, 5.0, 3.0, 5.0, 0.1);
                }

                // Görsel patlama
                world.createExplosion(null, center.getX() + 0.5, center.getY(),
                        center.getZ() + 0.5, 0.0f, false, World.ExplosionSourceType.NONE);
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
                    SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.BLOCKS, 2.0f, 1.0f);
            world.playSound(null, center.getX(), center.getY(), center.getZ(),
                    SoundEvents.BLOCK_CAMPFIRE_CRACKLE, SoundCategory.BLOCKS, 2.0f, 1.2f);
            return;
        }
        if (!done) super.tick();
    }
}
