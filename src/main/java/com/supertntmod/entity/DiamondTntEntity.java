package com.supertntmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * 💎 Elmas TNT
 * Normal TNT'nin 2.5 katı patlama gücü. Crater bırakır.
 * Ağır darbe sesi ve büyük patlama partikülleri.
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
            World world = getEntityWorld();
            this.discard();

            // Ağır darbe sesi
            world.playSound(null, x, y, z,
                    SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 2.0f, 0.5f);

            // 2.5x patlama gücü (normal=4, biz=10)
            world.createExplosion(null, x, y, z, 10.0f, true,
                    World.ExplosionSourceType.TNT);

            // Büyük patlama partikülleri
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.EXPLOSION_EMITTER,
                        x, y + 1, z, 5, 2.0, 2.0, 2.0, 0.0);
                serverWorld.spawnParticles(ParticleTypes.CLOUD,
                        x, y + 2, z, 100, 4.0, 3.0, 4.0, 0.1);
            }
            return;
        }
        if (!done) super.tick();
    }
}
