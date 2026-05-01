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
 * 💚 Zümrüt Yağmuru TNT — patladığında etrafa zümrüt blokları saçar. Açık/koyu yeşil temalı.
 */
public class ZumrutYagmuruTntEntity extends TntEntity {
    private static final int BLOCK_COUNT = 40;
    private static final double SPREAD = 8.0;
    private boolean done = false;

    public ZumrutYagmuruTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public ZumrutYagmuruTntEntity(World world, double x, double y, double z, @Nullable LivingEntity igniter) {
        super(ModEntities.ZUMRUT_YAGMURU_TNT, world);
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
                    SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 1.8f, 0.9f);

            for (int i = 0; i < BLOCK_COUNT; i++) {
                double ox = (world.random.nextDouble() - 0.5) * SPREAD;
                double oy = world.random.nextDouble() * 2.5;
                double oz = (world.random.nextDouble() - 0.5) * SPREAD;
                ItemEntity gem = new ItemEntity(world, cx + ox, cy + 1 + oy, cz + oz,
                        new ItemStack(Items.EMERALD_BLOCK));
                gem.setVelocity(
                        (world.random.nextDouble() - 0.5) * 0.5,
                        world.random.nextDouble() * 0.5 + 0.3,
                        (world.random.nextDouble() - 0.5) * 0.5);
                world.spawnEntity(gem);
            }

            if (world instanceof ServerWorld sw) {
                sw.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                        cx, cy + 2, cz, 100, 5.0, 4.0, 5.0, 0.4);
            }

            world.createExplosion(null, cx, cy, cz, 0.0f, false, World.ExplosionSourceType.NONE);
            return;
        }
        if (!done) super.tick();
    }
}
