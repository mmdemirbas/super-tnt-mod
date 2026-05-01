package com.supertntmod.entity;

import com.supertntmod.item.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * 🪙 Kuruş TNT — patladığında etrafa 1 kuruş madeni paraları saçar. Sarı temalı.
 */
public class KurusTntEntity extends TntEntity {
    private static final int COIN_COUNT = 200;
    private static final double SPREAD = 9.0;
    private boolean done = false;

    public KurusTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public KurusTntEntity(World world, double x, double y, double z, @Nullable LivingEntity igniter) {
        super(ModEntities.KURUS_TNT, world);
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
                    SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 1.5f, 1.5f);
            world.playSound(null, cx, cy, cz,
                    SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 1.5f, 1.2f);

            for (int i = 0; i < COIN_COUNT; i++) {
                double ox = (world.random.nextDouble() - 0.5) * SPREAD;
                double oy = world.random.nextDouble() * 2.5;
                double oz = (world.random.nextDouble() - 0.5) * SPREAD;
                ItemEntity coin = new ItemEntity(world, cx + ox, cy + 1 + oy, cz + oz,
                        new ItemStack(ModItems.KURUS));
                coin.setVelocity(
                        (world.random.nextDouble() - 0.5) * 0.6,
                        world.random.nextDouble() * 0.5 + 0.2,
                        (world.random.nextDouble() - 0.5) * 0.6);
                world.spawnEntity(coin);
            }

            if (world instanceof ServerWorld sw) {
                sw.spawnParticles(ParticleTypes.END_ROD,
                        cx, cy + 2, cz, 60, 4.0, 3.0, 4.0, 0.2);
            }

            world.createExplosion(null, cx, cy, cz, 0.0f, false, World.ExplosionSourceType.NONE);
            return;
        }
        if (!done) super.tick();
    }
}
