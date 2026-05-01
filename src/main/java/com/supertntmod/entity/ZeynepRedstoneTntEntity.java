package com.supertntmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Zeynep Redstone TNT: Çok büyük bir patlama yapar ve patlatan dışındaki
 * tüm oyuncuları öldürür. (TODO'daki "oyunu kapatır" yerine güvenli bir
 * "dünyayı yok et" yorumu — gerçek istemci/sunucu kapatma yapmıyoruz.)
 */
public class ZeynepRedstoneTntEntity extends TntEntity {
    private boolean done = false;
    private static final float EXPLOSION_POWER = 30.0f;

    public ZeynepRedstoneTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(100);
    }

    public ZeynepRedstoneTntEntity(World world, double x, double y, double z,
                                   @Nullable LivingEntity igniter) {
        super(ModEntities.ZEYNEP_REDSTONE_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(100);
    }

    @Override
    public void tick() {
        if (!done && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            done = true;
            World world = getEntityWorld();
            double x = getX(), y = getY(), z = getZ();

            // Ateşleyeni belirle (patlatan kişiyi koru)
            UUID owner = (this.getOwner() != null) ? this.getOwner().getUuid() : null;
            this.discard();

            world.playSound(null, x, y, z, SoundEvents.ENTITY_WITHER_SPAWN,
                    SoundCategory.BLOCKS, 4.0f, 0.3f);
            world.playSound(null, x, y, z, SoundEvents.ENTITY_ENDER_DRAGON_GROWL,
                    SoundCategory.BLOCKS, 4.0f, 0.2f);
            world.playSound(null, x, y, z, SoundEvents.ENTITY_GENERIC_EXPLODE,
                    SoundCategory.BLOCKS, 4.0f, 0.4f);

            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.EXPLOSION_EMITTER,
                        x, y, z, 50, 15.0, 8.0, 15.0, 0.5);
                serverWorld.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        x, y, z, 500, 20.0, 10.0, 20.0, 0.2);

                // Patlatan dışındaki tüm oyuncuları öldür
                for (ServerPlayerEntity player : serverWorld.getPlayers()) {
                    if (owner != null && player.getUuid().equals(owner)) continue;
                    player.kill(serverWorld);
                }
            }

            // Dev patlama
            world.createExplosion(null, x, y, z, EXPLOSION_POWER, true,
                    World.ExplosionSourceType.TNT);
            return;
        }
        if (!done) super.tick();
    }
}
