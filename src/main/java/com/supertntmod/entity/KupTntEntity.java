package com.supertntmod.entity;

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

import java.util.ArrayList;
import java.util.List;

public class KupTntEntity extends TntEntity {
    private static final int HALF = 12;
    private static final int DEPTH = 25;
    private static final int BLOCKS_PER_TICK = 50;

    private boolean exploded = false;
    private List<BlockPos> pending = null;

    public KupTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public KupTntEntity(World world, double x, double y, double z, @Nullable LivingEntity igniter) {
        super(ModEntities.KUP_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    @Override
    public void tick() {
        if (pending != null && !pending.isEmpty()) {
            World world = getEntityWorld();
            int processed = 0;
            while (!pending.isEmpty() && processed < BLOCKS_PER_TICK) {
                BlockPos pos = pending.remove(pending.size() - 1);
                if (world.getBlockState(pos).getBlock() != Blocks.BEDROCK) {
                    world.setBlockState(pos, Blocks.AIR.getDefaultState());
                }
                processed++;
            }
            return;
        }

        if (!exploded && this.getFuse() <= 1 && !getEntityWorld().isClient()) {
            exploded = true;
            double x = getX(), y = getY(), z = getZ();
            World world = getEntityWorld();
            this.discard();

            world.playSound(null, x, y, z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 3.0f, 0.5f);
            world.createExplosion(null, x, y, z, 4.0f, false, World.ExplosionSourceType.TNT);

            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 10, 5.0, 3.0, 5.0, 0.1);
            }

            BlockPos center = BlockPos.ofFloored(x, y, z);
            pending = new ArrayList<>();
            for (int dx = -HALF; dx <= HALF; dx++) {
                for (int dz = -HALF; dz <= HALF; dz++) {
                    for (int dy = 0; dy >= -DEPTH; dy--) {
                        pending.add(center.add(dx, dy, dz));
                    }
                }
            }
            return;
        }
        if (!exploded) super.tick();
    }
}
