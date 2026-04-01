package com.supertntmod.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Yerçekimi TNT: 12 blok yarıçapındaki tüm canlıların yerçekimini
 * 10 saniye boyunca tersine çevirir. Blok hasarı yoktur.
 *
 * <p>Patlayınca etkilenen entity UUID'leri GRAVITY_INVERTED map'ine eklenir
 * ve {@link #tickGravity(MinecraftServer)} ile her tick işlenir.</p>
 */
public class GravityTntEntity extends TntEntity {

    private static final int RADIUS = 12;
    /** UUID → kalan tick sayısı (200 tick = 10 saniye) */
    public static final Map<UUID, Integer> GRAVITY_INVERTED = new ConcurrentHashMap<>();

    private boolean done = false;

    public GravityTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public GravityTntEntity(World world, double x, double y, double z,
                            @Nullable LivingEntity igniter) {
        super(ModEntities.GRAVITY_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    @Override
    public void tick() {
        if (!done && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            done = true;
            BlockPos center = this.getBlockPos();
            World world = getEntityWorld();
            this.discard();

            // Yerçekimi sesi
            world.playSound(null, center.getX(), center.getY(), center.getZ(),
                    SoundEvents.ENTITY_PHANTOM_AMBIENT, SoundCategory.BLOCKS, 2.0f, 0.5f);
            world.playSound(null, center.getX(), center.getY(), center.getZ(),
                    SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 1.0f, 0.8f);

            // Etkilenen entity'leri kaydet ve başlangıç hızını ver
            world.getEntitiesByClass(LivingEntity.class,
                    new Box(center).expand(RADIUS),
                    e -> e.isAlive()
            ).forEach(entity -> {
                entity.setNoGravity(true);
                entity.addVelocity(0, 0.8, 0); // Başlangıç yüksek itiş
                entity.velocityDirty = true;
                GRAVITY_INVERTED.put(entity.getUuid(), 200);
            });

            // Partiküller (yukarı doğru çekilen efekt)
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.END_ROD,
                        center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5,
                        150, 5.0, 1.0, 5.0, 0.2);
                serverWorld.spawnParticles(ParticleTypes.FALLING_OBSIDIAN_TEAR,
                        center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5,
                        80, 4.0, 1.0, 4.0, 0.1);
            }

            // Görsel patlama (blok hasarsız)
            world.createExplosion(null,
                    center.getX() + 0.5, center.getY(), center.getZ() + 0.5,
                    0.0f, false, World.ExplosionSourceType.NONE);
            return;
        }
        if (!done) super.tick();
    }

    /**
     * Server tick'inde çağrılır. Yerçekimi ters çevrilmiş entity'lerin
     * zamanlayıcısını düşürür; süre bitince normal yerçekimini geri yükler.
     */
    public static void tickGravity(MinecraftServer server) {
        if (GRAVITY_INVERTED.isEmpty()) return;

        Iterator<Map.Entry<UUID, Integer>> iter = GRAVITY_INVERTED.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<UUID, Integer> entry = iter.next();
            UUID uuid = entry.getKey();
            int ticksLeft = entry.getValue();

            // Entity'yi tüm dünyalarda ara
            Entity entity = null;
            for (ServerWorld world : server.getWorlds()) {
                entity = world.getEntity(uuid);
                if (entity != null) break;
            }

            if (entity == null || !entity.isAlive()) {
                iter.remove();
                continue;
            }

            if (ticksLeft <= 0) {
                // Süre bitti: normal yerçekimini geri yükle
                entity.setNoGravity(false);
                iter.remove();
            } else {
                entry.setValue(ticksLeft - 1);
            }
        }
    }
}
