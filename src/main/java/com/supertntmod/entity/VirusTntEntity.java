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
 * Virüs TNT: Patladığında etrafa "virüsler" saçar; sonra hepsi yok olur.
 * Sonuç: hiçbir şey olmaz. Sadece görsel.
 */
public class VirusTntEntity extends TntEntity {
    private boolean done = false;

    public VirusTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public VirusTntEntity(World world, double x, double y, double z,
                          @Nullable LivingEntity igniter) {
        super(ModEntities.VIRUS_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    @Override
    public void tick() {
        if (!done && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            done = true;
            World world = getEntityWorld();
            double x = getX(), y = getY(), z = getZ();
            this.discard();

            world.playSound(null, x, y, z, SoundEvents.ENTITY_BAT_AMBIENT,
                    SoundCategory.BLOCKS, 2.5f, 0.5f);
            world.playSound(null, x, y, z, SoundEvents.ENTITY_SILVERFISH_AMBIENT,
                    SoundCategory.BLOCKS, 2.0f, 0.7f);

            if (world instanceof ServerWorld serverWorld) {
                // Yeşil mor partiküller - virüsler
                serverWorld.spawnParticles(ParticleTypes.WITCH,
                        x, y + 1, z, 300, 8.0, 4.0, 8.0, 0.2);
                serverWorld.spawnParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                        x, y + 1, z, 200, 6.0, 4.0, 6.0, 0.05);
                // Hepsi yok olur — beyaz toz
                serverWorld.spawnParticles(ParticleTypes.WHITE_ASH,
                        x, y + 2, z, 100, 8.0, 4.0, 8.0, 0.01);
            }

            // Sadece görsel patlama — hiç hasar yok
            world.createExplosion(null, x, y, z, 0.0f, false, World.ExplosionSourceType.NONE);
            return;
        }
        if (!done) super.tick();
    }
}
