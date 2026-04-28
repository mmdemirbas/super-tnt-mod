package com.supertntmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Z-Güneş TNT: Geceyse ayı güneşe çevirir — gündüz olur.
 * Gündüz ise sadece görsel patlama yapar.
 */
public class ZGunesTntEntity extends TntEntity {
    private boolean done = false;

    public ZGunesTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public ZGunesTntEntity(World world, double x, double y, double z,
                           @Nullable LivingEntity igniter) {
        super(ModEntities.Z_GUNES_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    @Override
    public void tick() {
        if (!done && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            done = true;
            World world = getEntityWorld();
            BlockPos center = this.getBlockPos();
            double cx = center.getX() + 0.5, cy = center.getY(), cz = center.getZ() + 0.5;
            this.discard();

            if (world instanceof ServerWorld serverWorld) {
                long t = serverWorld.getTimeOfDay();
                long tod = t % 24000L;
                if (tod >= 12300L) {
                    // Gece — gündüze çevir (6000 = öğle)
                    serverWorld.setTimeOfDay(t + (6000L - tod + 24000L) % 24000L);
                    // Havayı da temizle
                    serverWorld.setWeather(0, 24000, false, false);
                }
                serverWorld.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING,
                        cx, cy + 3, cz, 300, 6.0, 4.0, 6.0, 0.3);
                serverWorld.spawnParticles(ParticleTypes.FIREWORK,
                        cx, cy + 5, cz, 150, 8.0, 4.0, 8.0, 0.2);
            }

            world.playSound(null, cx, cy, cz,
                    SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 3.0f, 1.0f);
            world.playSound(null, cx, cy, cz,
                    SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.BLOCKS, 2.5f, 0.8f);

            world.createExplosion(null, cx, cy, cz, 1.0f, false, World.ExplosionSourceType.NONE);
            return;
        }
        if (!done) super.tick();
    }
}
