package com.supertntmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * 💎 Elmas TNT
 * Normal TNT'nin 2.5 katı patlama gücü. Crater bırakır.
 */
public class DiamondTntEntity extends TntEntity {
    private boolean done = false;

    public DiamondTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public DiamondTntEntity(World world, double x, double y, double z,
                            @Nullable LivingEntity igniter) {
        super(ModEntities.DIAMOND_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    @Override
    public void tick() {
        if (!done && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            done = true;
            double x = getX(), y = getY(), z = getZ();
            this.discard();
            // 2.5x patlama gücü (normal=4, biz=10)
            getEntityWorld().createExplosion(null, x, y, z, 10.0f, true,
                    World.ExplosionSourceType.TNT);
            return;
        }
        if (!done) super.tick();
    }
}
