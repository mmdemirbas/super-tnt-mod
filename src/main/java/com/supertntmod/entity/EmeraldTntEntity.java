package com.supertntmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * 💚 Zümrüt TNT
 * Patlama sonrası çevreye emerald fırlatır.
 * Çocuklar için hazine sandığı gibi!
 */
public class EmeraldTntEntity extends TntEntity {
    private boolean done = false;

    public EmeraldTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public EmeraldTntEntity(World world, double x, double y, double z,
                            @Nullable LivingEntity igniter) {
        super(ModEntities.EMERALD_TNT, world);
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

            // Normal patlama
            world.createExplosion(null, x, y, z, 4.0f, true,
                    World.ExplosionSourceType.TNT);

            // Emerald yağmuru (16-32 adet)
            int count = 16 + world.random.nextInt(16);
            for (int i = 0; i < count; i++) {
                ItemEntity item = new ItemEntity(world,
                        x + (world.random.nextDouble() - 0.5) * 5,
                        y + 1,
                        z + (world.random.nextDouble() - 0.5) * 5,
                        new ItemStack(Items.EMERALD, 1 + world.random.nextInt(3)));
                item.setVelocity(
                        (world.random.nextDouble() - 0.5) * 0.6,
                        world.random.nextDouble() * 0.8 + 0.2,
                        (world.random.nextDouble() - 0.5) * 0.6);
                world.spawnEntity(item);
            }
            return;
        }
        if (!done) super.tick();
    }
}
