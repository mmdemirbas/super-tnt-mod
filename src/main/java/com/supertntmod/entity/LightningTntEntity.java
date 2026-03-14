package com.supertntmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * ⚡ Yıldırım TNT
 * Patladığında geniş alana 20 yıldırım çakar.
 * Ormanı yakar, mobları çarpar!
 */
public class LightningTntEntity extends TntEntity {
    private boolean done = false;

    public LightningTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public LightningTntEntity(World world, double x, double y, double z,
                              @Nullable LivingEntity igniter) {
        super(ModEntities.LIGHTNING_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    @Override
    public void tick() {
        if (!done && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            done = true;
            double x = getX(), y = getY(), z = getZ();
            World world = getEntityWorld();
            this.discard();

            // Küçük patlama
            world.createExplosion(null, x, y, z, 3.0f, true,
                    World.ExplosionSourceType.TNT);

            // 20 yıldırım 20 blok yarıçapına
            for (int i = 0; i < 20; i++) {
                LightningEntity bolt = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
                bolt.setPos(
                        x + (world.random.nextDouble() - 0.5) * 20,
                        y,
                        z + (world.random.nextDouble() - 0.5) * 20);
                world.spawnEntity(bolt);
            }
            return;
        }
        if (!done) super.tick();
    }
}
