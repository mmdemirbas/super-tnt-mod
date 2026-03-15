package com.supertntmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Yürüyen TNT Blok: Enderman gibi göz göze gelince yaklaşır.
 * Koyan kişiyi görmezden gelir, sadece başkalarına saldırır.
 */
public class WalkingTntEntity extends PathAwareEntity {
    private PlayerEntity target = null;
    private boolean exploded = false;
    private @Nullable UUID ownerUuid = null;
    private static final double EXPLOSION_DISTANCE = 2.0;
    private static final float EXPLOSION_POWER = 8.0f;
    private static final int DETECTION_RANGE = 30;

    public WalkingTntEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }

    public WalkingTntEntity(World world, double x, double y, double z) {
        super(ModEntities.WALKING_TNT, world);
        this.setPosition(x, y, z);
    }

    public void setOwnerUuid(@Nullable UUID uuid) {
        this.ownerUuid = uuid;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.MAX_HEALTH, 20.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.35)
                .add(EntityAttributes.FOLLOW_RANGE, DETECTION_RANGE);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.getEntityWorld().isClient() || exploded) return;

        // Hedef yoksa veya öldüyse, göz göze gelen oyuncu ara
        if (target == null || !target.isAlive()) {
            target = null;
            findLookingPlayer();
        }

        if (target != null) {
            // Hedefe doğru yürü
            this.getNavigation().startMovingTo(target, 1.5);

            // Yeterince yakınsa patla
            double distance = this.squaredDistanceTo(target);
            if (distance <= EXPLOSION_DISTANCE * EXPLOSION_DISTANCE) {
                explode();
            }
        }
    }

    /**
     * Enderman gibi: Oyuncunun bakış yönünü kontrol et.
     * Oyuncu bu entity'ye bakıyorsa (göz göze gelme), hedefe al.
     * Sahibini (koyan kişiyi) görmezden gelir.
     */
    private void findLookingPlayer() {
        for (PlayerEntity player : this.getEntityWorld().getPlayers()) {
            if (player.isSpectator() || !player.isAlive()) continue;
            // Sahibini atla
            if (ownerUuid != null && player.getUuid().equals(ownerUuid)) continue;
            if (this.squaredDistanceTo(player) > DETECTION_RANGE * DETECTION_RANGE) continue;

            // Oyuncunun bakış yönünü kontrol et
            Vec3d playerLook = player.getRotationVec(1.0f).normalize();
            Vec3d entityPos = new Vec3d(this.getX(), this.getEyeY(), this.getZ());
            Vec3d dirToEntity = entityPos.subtract(player.getEyePos()).normalize();

            // Bakış açısı 10 dereceden azsa (dot product > ~0.985)
            double dot = playerLook.dotProduct(dirToEntity);
            if (dot > 0.985) {
                this.target = player;
                this.getEntityWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ENTITY_ENDERMAN_STARE, SoundCategory.HOSTILE, 1.0f, 0.5f);
                break;
            }
        }
    }

    private void explode() {
        if (exploded) return;
        exploded = true;

        double x = getX(), y = getY(), z = getZ();
        World world = getEntityWorld();

        this.discard();

        world.createExplosion(null, x, y, z, EXPLOSION_POWER, true,
                World.ExplosionSourceType.TNT);
    }

    @Override
    public boolean cannotDespawn() {
        return true;
    }
}
