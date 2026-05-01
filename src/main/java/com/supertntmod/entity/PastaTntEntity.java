package com.supertntmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * 🎂 Pasta TNT — patladığında etrafa pastalar saçar. Mor temalı.
 */
public class PastaTntEntity extends TntEntity {
    private static final int CAKE_COUNT = 30;
    private static final double SPREAD = 7.0;
    private boolean done = false;

    public PastaTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public PastaTntEntity(World world, double x, double y, double z, @Nullable LivingEntity igniter) {
        super(ModEntities.PASTA_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    @Override
    public void tick() {
        if (!done && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            done = true;
            BlockPos center = this.getBlockPos();
            double cx = center.getX() + 0.5, cy = center.getY(), cz = center.getZ() + 0.5;
            World world = getEntityWorld();
            this.discard();

            world.playSound(null, cx, cy, cz,
                    SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 1.5f, 1.0f);

            for (int i = 0; i < CAKE_COUNT; i++) {
                double ox = (world.random.nextDouble() - 0.5) * SPREAD;
                double oy = world.random.nextDouble() * 2.5;
                double oz = (world.random.nextDouble() - 0.5) * SPREAD;
                ItemEntity cake = new ItemEntity(world, cx + ox, cy + 1 + oy, cz + oz,
                        new ItemStack(Items.CAKE));
                cake.setVelocity(
                        (world.random.nextDouble() - 0.5) * 0.5,
                        world.random.nextDouble() * 0.5 + 0.2,
                        (world.random.nextDouble() - 0.5) * 0.5);
                world.spawnEntity(cake);
            }

            if (world instanceof ServerWorld sw) {
                sw.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                        cx, cy + 2, cz, 50, 4.0, 3.0, 4.0, 0.3);
            }

            world.createExplosion(null, cx, cy, cz, 0.0f, false, World.ExplosionSourceType.NONE);
            return;
        }
        if (!done) super.tick();
    }
}
