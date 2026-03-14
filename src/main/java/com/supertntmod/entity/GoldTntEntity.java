package com.supertntmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * 🥇 Altın TNT
 * Tek büyük patlama yerine 5 küçük patlama dalgası saçar.
 */
public class GoldTntEntity extends TntEntity {
    private boolean done = false;

    public GoldTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(60);
    }

    public GoldTntEntity(World world, double x, double y, double z,
                         @Nullable LivingEntity igniter) {
        super(ModEntities.GOLD_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(60);
    }

    @Override
    public void tick() {
        if (!done && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            done = true;
            double x = getX(), y = getY(), z = getZ();
            World world = getEntityWorld();
            this.discard();
            // Merkez patlama
            world.createExplosion(null, x, y, z, 4.0f, true,
                    World.ExplosionSourceType.TNT);
            // 5 rastgele küçük patlama
            for (int i = 0; i < 5; i++) {
                double ox = x + (world.random.nextDouble() - 0.5) * 8;
                double oz = z + (world.random.nextDouble() - 0.5) * 8;
                world.createExplosion(null, ox, y, oz, 2.5f, true,
                        World.ExplosionSourceType.TNT);
            }
            return;
        }
        if (!done) super.tick();
    }
}
