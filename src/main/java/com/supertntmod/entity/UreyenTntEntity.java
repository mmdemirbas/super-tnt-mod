package com.supertntmod.entity;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class UreyenTntEntity extends TntEntity {
    private static final int MAX_GENERATION = 5;
    private int generation;
    private boolean done = false;

    public UreyenTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
        this.generation = 0;
    }

    public UreyenTntEntity(World world, double x, double y, double z, @Nullable LivingEntity igniter) {
        super(ModEntities.UREYEN_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
        this.generation = 0;
    }

    private UreyenTntEntity(World world, double x, double y, double z, int generation) {
        super(ModEntities.UREYEN_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(Math.max(20, 80 - generation * 15));
        this.generation = generation;
    }

    @Override
    public void tick() {
        if (!done && this.getFuse() <= 1 && !getEntityWorld().isClient()) {
            done = true;
            double x = getX(), y = getY(), z = getZ();
            World world = getEntityWorld();
            this.discard();

            world.createExplosion(null, x, y, z, 3.0f, false, World.ExplosionSourceType.TNT);

            if (generation < MAX_GENERATION) {
                BlockPos center = BlockPos.ofFloored(x, y, z);
                BlockPos[] neighbors = {
                    center.north(), center.south(), center.east(), center.west(), center.up(), center.down()
                };
                for (BlockPos neighbor : neighbors) {
                    var state = world.getBlockState(neighbor);
                    if (state.getBlock() == Blocks.BEDROCK) continue;
                    if (state.isAir() || state.isReplaceable()) {
                        world.spawnEntity(new UreyenTntEntity(world,
                                neighbor.getX() + 0.5, neighbor.getY(), neighbor.getZ() + 0.5,
                                generation + 1));
                    }
                }
            }
            return;
        }
        if (!done) super.tick();
    }
}
