package com.supertntmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Görünmez TNT entity: standart TNT patlaması (güç 4.0), blok hasarı var.
 * Görseli taş gibi olduğu için herhangi bir uyarı vermez.
 */
public class InvisibleTntEntity extends TntEntity {

    private boolean done = false;

    public InvisibleTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public InvisibleTntEntity(World world, double x, double y, double z,
                              @Nullable LivingEntity igniter) {
        super(ModEntities.INVISIBLE_TNT, world);
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

            // Standart TNT patlaması (güç 4.0). x/y/z zaten blok merkezi
            // (CustomTntBlock.prime '+0.5' ekledi) — tekrar eklemeyiz.
            world.createExplosion(null, x, y, z,
                    4.0f, true, World.ExplosionSourceType.TNT);
            return;
        }
        if (!done) super.tick();
    }
}
