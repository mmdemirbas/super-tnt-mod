package com.supertntmod.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.mob.CaveSpiderEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Mağara TNT: 5 farklı yerde küresel mağara oyar + 15 mağara yaratığı doğurur.
 * Kademeli işlem: tick başına 400 blok.
 */
public class MagaraTntEntity extends TntEntity {
    private static final int CAVE_COUNT = 5;
    private static final int CAVE_RADIUS = 8;
    private static final int MOD_PER_TICK = 400;
    private static final int MOB_COUNT = 15;

    private boolean done = false;
    private boolean processing = false;
    private BlockPos center;
    private BlockPos[] caveCenters;
    private int cavePhase = 0;
    private int blockIdx = 0;

    public MagaraTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public MagaraTntEntity(World world, double x, double y, double z,
                           @Nullable LivingEntity igniter) {
        super(ModEntities.MAGARA_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    @Override
    public void tick() {
        if (processing && !this.getEntityWorld().isClient()) {
            World world = getEntityWorld();
            int side = CAVE_RADIUS * 2 + 1;
            int total = side * side * side;
            int modified = 0;

            while (modified < MOD_PER_TICK) {
                if (cavePhase >= CAVE_COUNT) {
                    processing = false;
                    spawnMobs(world);
                    finishEffect(world);
                    return;
                }

                if (blockIdx >= total) {
                    blockIdx = 0;
                    cavePhase++;
                    continue;
                }

                int lz = blockIdx % side - CAVE_RADIUS;
                int lx = (blockIdx / side) % side - CAVE_RADIUS;
                int ly = (blockIdx / (side * side)) - CAVE_RADIUS;
                blockIdx++;

                BlockPos cc = caveCenters[cavePhase];
                BlockPos pos = cc.add(lx, ly, lz);
                if (!pos.isWithinDistance(cc, CAVE_RADIUS)) continue;
                BlockState bs = world.getBlockState(pos);
                if (!bs.isAir() && !bs.isOf(Blocks.BEDROCK)) {
                    world.breakBlock(pos, false);
                    modified++;
                }
            }
            return;
        }

        if (!done && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            done = true;
            World world = getEntityWorld();
            center = this.getBlockPos();
            double cx = center.getX() + 0.5, cy = center.getY(), cz = center.getZ() + 0.5;

            caveCenters = new BlockPos[CAVE_COUNT];
            for (int i = 0; i < CAVE_COUNT; i++) {
                int dx = world.random.nextInt(31) - 15;
                int dy = -(world.random.nextInt(15) + 5); // 5-20 blok aşağı
                int dz = world.random.nextInt(31) - 15;
                caveCenters[i] = center.add(dx, dy, dz);
            }

            world.playSound(null, cx, cy, cz,
                    SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.BLOCKS, 3.0f, 0.5f);
            world.playSound(null, cx, cy, cz,
                    SoundEvents.ENTITY_BAT_AMBIENT, SoundCategory.BLOCKS, 2.0f, 0.7f);

            if (world instanceof ServerWorld sw) {
                sw.spawnParticles(ParticleTypes.SMOKE, cx, cy + 2, cz, 100, 4.0, 3.0, 4.0, 0.05);
                sw.spawnParticles(ParticleTypes.ASH, cx, cy + 4, cz, 200, 8.0, 4.0, 8.0, 0.02);
            }

            processing = true;
            return;
        }
        if (!done) super.tick();
    }

    private void spawnMobs(World world) {
        if (!(world instanceof ServerWorld)) return;
        for (int i = 0; i < MOB_COUNT; i++) {
            int dx = world.random.nextInt(31) - 15;
            int dy = -(world.random.nextInt(10));
            int dz = world.random.nextInt(31) - 15;
            double mx = center.getX() + 0.5 + dx;
            double my = center.getY() + dy;
            double mz = center.getZ() + 0.5 + dz;

            LivingEntity mob = switch (world.random.nextInt(5)) {
                case 0 -> new ZombieEntity(EntityType.ZOMBIE, world);
                case 1 -> new SkeletonEntity(EntityType.SKELETON, world);
                case 2 -> new SpiderEntity(EntityType.SPIDER, world);
                case 3 -> new CreeperEntity(EntityType.CREEPER, world);
                default -> new CaveSpiderEntity(EntityType.CAVE_SPIDER, world);
            };
            mob.setPosition(mx, my, mz);
            world.spawnEntity(mob);
        }
    }

    private void finishEffect(World world) {
        double cx = center.getX() + 0.5, cy = center.getY(), cz = center.getZ() + 0.5;
        world.createExplosion(null, cx, cy, cz, 4.0f, false, World.ExplosionSourceType.TNT);
        if (world instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, cx, cy + 2, cz, 6, 4.0, 2.0, 4.0, 0.0);
        }
        this.discard();
    }
}
