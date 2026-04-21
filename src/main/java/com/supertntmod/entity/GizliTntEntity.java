package com.supertntmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class GizliTntEntity extends TntEntity {
    private static final int RADIUS = 25;
    private boolean done = false;

    public GizliTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public GizliTntEntity(World world, double x, double y, double z, @Nullable LivingEntity igniter) {
        super(ModEntities.GIZLI_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    @Override
    public void tick() {
        if (!done && this.getFuse() <= 1 && !getEntityWorld().isClient()) {
            done = true;
            double x = getX(), y = getY(), z = getZ();
            World world = getEntityWorld();
            this.discard();

            if (world instanceof ServerWorld serverWorld) {
                world.getEntitiesByClass(LivingEntity.class,
                        new Box(x - RADIUS, y - RADIUS, z - RADIUS, x + RADIUS, y + RADIUS, z + RADIUS),
                        e -> true
                ).forEach(entity -> entity.damage(serverWorld,
                        world.getDamageSources().genericKill(), Float.MAX_VALUE));
            }
            return;
        }
        if (!done) super.tick();
    }
}
