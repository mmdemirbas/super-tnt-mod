package com.supertntmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * ⚡ Yıldırım TNT
 * Patladığında geniş alana 20 yıldırım çakar.
 * Elektrik kıvılcımları ve gök gürültüsü.
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

            // Gök gürültüsü sesi
            world.playSound(null, x, y, z,
                    SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 3.0f, 0.8f);

            // Küçük patlama
            world.createExplosion(null, x, y, z, 3.0f, true,
                    World.ExplosionSourceType.TNT);

            // Elektrik kıvılcımları
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.END_ROD,
                        x, y + 1, z, 200, 5.0, 3.0, 5.0, 0.5);
                serverWorld.spawnParticles(ParticleTypes.CRIT,
                        x, y + 2, z, 100, 4.0, 3.0, 4.0, 0.3);
            }

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
        // Fitil yanarken elektrik çatırtısı (her 15 tick'te)
        if (!done && !this.getEntityWorld().isClient() && this.getFuse() % 15 == 0) {
            if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.END_ROD,
                        getX(), getY() + 1, getZ(), 10, 0.3, 0.5, 0.3, 0.1);
            }
        }
        if (!done) super.tick();
    }
}
