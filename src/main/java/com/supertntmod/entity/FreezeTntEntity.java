package com.supertntmod.entity;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * ❄ Dondurucu TNT
 * Etrafı karla kaplar, suları buza dönüştürür.
 * Düşmanlar karda yavaşlar.
 */
public class FreezeTntEntity extends TntEntity {
    private static final int RADIUS = 9;
    private boolean done = false;

    public FreezeTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public FreezeTntEntity(World world, double x, double y, double z,
                           @Nullable LivingEntity igniter) {
        super(ModEntities.FREEZE_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    @Override
    public void tick() {
        if (!done && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            done = true;
            BlockPos center = this.getBlockPos();
            World world = getEntityWorld();
            this.discard();

            for (BlockPos pos : BlockPos.iterateOutwards(center, RADIUS, RADIUS, RADIUS)) {
                if (!pos.isWithinDistance(center, RADIUS)) continue;

                var state = world.getBlockState(pos);

                // Suyu buza dönüştür
                if (state.isOf(Blocks.WATER)) {
                    world.setBlockState(pos, Blocks.ICE.getDefaultState());
                }
                // Havayı kar katmanıyla kaplama (%40 şans)
                else if (state.isOf(Blocks.AIR) &&
                         world.getBlockState(pos.down()).isSolid() &&
                         world.random.nextFloat() < 0.4f) {
                    world.setBlockState(pos, Blocks.SNOW.getDefaultState());
                }
            }

            // Küçük patlama (görsel efekt)
            world.createExplosion(null, center.getX() + 0.5, center.getY(),
                    center.getZ() + 0.5, 2.0f, false, World.ExplosionSourceType.TNT);
            return;
        }
        if (!done) super.tick();
    }
}
