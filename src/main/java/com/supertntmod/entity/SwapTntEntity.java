package com.supertntmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Takas TNT entity: 15 blok yarıçapındaki canlıların konumlarını
 * rastgele karıştırır. Blok hasarı yoktur.
 */
public class SwapTntEntity extends TntEntity {

    private static final int RADIUS = 15;
    private boolean done = false;

    public SwapTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public SwapTntEntity(World world, double x, double y, double z,
                         @Nullable LivingEntity igniter) {
        super(ModEntities.SWAP_TNT, world);
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

            // Takas sesi
            world.playSound(null, center.getX(), center.getY(), center.getZ(),
                    SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.BLOCKS, 2.0f, 0.6f);
            world.playSound(null, center.getX(), center.getY(), center.getZ(),
                    SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 1.0f, 1.2f);

            // Yakındaki canlıları topla
            List<LivingEntity> entities = world.getEntitiesByClass(
                    LivingEntity.class,
                    new Box(center).expand(RADIUS),
                    e -> e.isAlive()
            );

            if (entities.size() >= 2) {
                // Konumları kaydet
                List<Vec3d> positions = new ArrayList<>();
                for (LivingEntity e : entities) {
                    positions.add(new Vec3d(e.getX(), e.getY(), e.getZ()));
                }

                // Konumları karıştır (Fisher-Yates, MC random kullanarak)
                for (int i = positions.size() - 1; i > 0; i--) {
                    int j = world.random.nextInt(i + 1);
                    Vec3d tmp = positions.get(i);
                    positions.set(i, positions.get(j));
                    positions.set(j, tmp);
                }

                // Teleport et
                for (int i = 0; i < entities.size(); i++) {
                    Vec3d dest = positions.get(i);
                    LivingEntity entity = entities.get(i);
                    entity.setPosition(dest.x, dest.y, dest.z);
                    entity.velocityDirty = true;
                }
            }

            // Takas partikülleri
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.PORTAL,
                        center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5,
                        200, 6.0, 2.0, 6.0, 0.5);
                serverWorld.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                        center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5,
                        100, 5.0, 2.0, 5.0, 0.3);
            }

            // Görsel patlama
            world.createExplosion(null,
                    center.getX() + 0.5, center.getY(), center.getZ() + 0.5,
                    0.0f, false, World.ExplosionSourceType.NONE);
            return;
        }
        if (!done) super.tick();
    }
}
