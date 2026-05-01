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
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Dağ TNT: Patladığında etrafa rastgele dağlar saçar.
 * Her dağ taş tabanlı, üstü çimli/karlı konik bir yapı.
 */
public class DagTntEntity extends TntEntity {
    private static final int MOUNTAIN_COUNT = 12;
    private static final int SPREAD = 20;
    private boolean done = false;

    public DagTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public DagTntEntity(World world, double x, double y, double z,
                        @Nullable LivingEntity igniter) {
        super(ModEntities.DAG_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    @Override
    public void tick() {
        if (!done && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            done = true;
            World world = getEntityWorld();
            BlockPos center = this.getBlockPos();
            double cx = center.getX() + 0.5, cy = center.getY(), cz = center.getZ() + 0.5;
            this.discard();

            int count = MOUNTAIN_COUNT + world.random.nextInt(5);
            for (int m = 0; m < count; m++) {
                int dx = world.random.nextInt(SPREAD * 2 + 1) - SPREAD;
                int dz = world.random.nextInt(SPREAD * 2 + 1) - SPREAD;
                int peak = 6 + world.random.nextInt(10); // 6-15 blok yükseklik
                int baseX = center.getX() + dx;
                int baseZ = center.getZ() + dz;
                int baseY = center.getY();

                for (int h = 0; h < peak; h++) {
                    int halfW = (peak - h) / 3;
                    BlockState state;
                    if (h == peak - 1) {
                        state = (peak > 9) ? Blocks.SNOW_BLOCK.getDefaultState()
                                : Blocks.GRASS_BLOCK.getDefaultState();
                    } else if (h >= peak - 3) {
                        state = Blocks.DIRT.getDefaultState();
                    } else {
                        state = Blocks.STONE.getDefaultState();
                    }
                    for (int bx = -halfW; bx <= halfW; bx++) {
                        for (int bz = -halfW; bz <= halfW; bz++) {
                            if (bx * bx + bz * bz <= halfW * halfW + halfW) {
                                world.setBlockState(new BlockPos(baseX + bx, baseY + h, baseZ + bz), state, 3);
                            }
                        }
                    }
                }
            }

            world.playSound(null, cx, cy, cz,
                    SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 2.5f, 0.6f);
            world.playSound(null, cx, cy, cz,
                    SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.BLOCKS, 1.5f, 1.2f);

            if (world instanceof ServerWorld sw) {
                sw.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, cx, cy + 2, cz, 8, 5.0, 2.0, 5.0, 0.0);
                sw.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, cx, cy + 5, cz, 150, 10.0, 5.0, 10.0, 0.03);
            }

            world.createExplosion(null, cx, cy, cz, 2.0f, false, World.ExplosionSourceType.TNT);
            return;
        }
        if (!done) super.tick();
    }
}
