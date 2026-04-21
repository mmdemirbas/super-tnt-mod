package com.supertntmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ElmasZirhTntEntity extends TntEntity {
    private boolean done = false;
    private int ticksAfterExplode = -1;

    public ElmasZirhTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public ElmasZirhTntEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        super(ModEntities.ELMAS_ZIRH_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    @Override
    public void tick() {
        if (ticksAfterExplode >= 0) {
            ticksAfterExplode++;
            if (ticksAfterExplode >= 60) {
                if (getEntityWorld() instanceof ServerWorld serverWorld) {
                    serverWorld.getServer().stop(false);
                }
            }
            return;
        }

        if (!done && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            done = true;
            double x = getX(), y = getY(), z = getZ();
            World world = getEntityWorld();
            this.discard();

            world.playSound(null, x, y, z,
                    SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 5.0f, 0.5f);

            world.createExplosion(null, x, y, z, 20.0f, true, World.ExplosionSourceType.TNT);

            if (world instanceof ServerWorld serverWorld2) {
                DamageSource src = world.getDamageSources().explosion(null, null);
                world.getEntitiesByClass(LivingEntity.class,
                        new net.minecraft.util.math.Box(BlockPos.ofFloored(x, y, z)).expand(50),
                        e -> true
                ).forEach(entity -> entity.damage(serverWorld2, src, Float.MAX_VALUE));
            }

            ticksAfterExplode = 0;
            return;
        }
        if (!done) super.tick();
    }
}
