package com.supertntmod.entity;

import net.minecraft.entity.Entity;
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

/**
 * Mıknatıs TNT entity: fitil yanmaya başladığında yakındaki entity'leri
 * merkeze çeker (fuse > 20). Fuse ≤ 1 olduğunda güçlü bir patlama yapar.
 *
 * <p>Çekme kuvveti: mesafeyle ters orantılı, maks. 0.25 blok/tick.</p>
 */
public class MagnetTntEntity extends TntEntity {

    private static final int RADIUS = 15;
    private static final double PULL_STRENGTH = 0.8;
    private boolean done = false;

    public MagnetTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public MagnetTntEntity(World world, double x, double y, double z,
                           @Nullable LivingEntity igniter) {
        super(ModEntities.MAGNET_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    @Override
    public void tick() {
        if (done) return;

        int fuse = this.getFuse();

        if (!this.getEntityWorld().isClient()) {
            World world = getEntityWorld();

            // Çekme fazı: fuse > 20 iken her tick çek
            if (fuse > 20) {
                Vec3d center = new Vec3d(getX() + 0.5, getY() + 0.5, getZ() + 0.5);
                world.getEntitiesByClass(Entity.class,
                        new Box(getBlockPos()).expand(RADIUS),
                        e -> e.isAlive() && e != this
                ).forEach(entity -> {
                    double dx = center.x - entity.getX();
                    double dy = center.y - entity.getY();
                    double dz = center.z - entity.getZ();
                    double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
                    if (dist < 0.5) return;

                    // Kuvveti mesafeyle ters orantılı, maks. 0.25 blok/tick ile sınırla
                    double force = Math.min(0.25, PULL_STRENGTH / (dist * dist));
                    entity.addVelocity(dx/dist * force, dy/dist * force * 0.5, dz/dist * force);
                    entity.velocityDirty = true;
                });

                // Her 5 tick'te mıknatıs partikülleri
                if (fuse % 5 == 0 && world instanceof ServerWorld serverWorld) {
                    serverWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                            getX() + 0.5, getY() + 0.5, getZ() + 0.5,
                            10, 0.3, 0.3, 0.3, 0.05);
                }
            }

            // Patlama fazı
            if (fuse <= 1) {
                done = true;
                BlockPos center = getBlockPos();
                this.discard();

                world.playSound(null, center.getX(), center.getY(), center.getZ(),
                        SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 2.0f, 0.7f);

                // Patlama (blok hasarı var, güç 5.0 — güçlü, çünkü düşmanları topladı)
                world.createExplosion(null,
                        center.getX() + 0.5, center.getY(), center.getZ() + 0.5,
                        5.0f, false, World.ExplosionSourceType.TNT);

                if (world instanceof ServerWorld serverWorld) {
                    serverWorld.spawnParticles(ParticleTypes.EXPLOSION_EMITTER,
                            center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5,
                            3, 1.0, 1.0, 1.0, 0.0);
                }
                return;
            }
        }

        super.tick();
    }
}
