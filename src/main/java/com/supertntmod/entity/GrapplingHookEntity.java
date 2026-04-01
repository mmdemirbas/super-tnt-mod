package com.supertntmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Kanca Entity: bir bloğa çarptığında sahibini o noktaya doğru fırlatır.
 * Entity'ye çarparsa bir şey yapmaz.
 */
public class GrapplingHookEntity extends ThrownEntity {

    public GrapplingHookEntity(EntityType<? extends ThrownEntity> type, World world) {
        super(type, world);
    }

    public GrapplingHookEntity(World world, PlayerEntity owner) {
        super(ModEntities.GRAPPLING_HOOK, owner.getX(), owner.getEyeY() - 0.1,
                owner.getZ(), world);
        this.setOwner(owner);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        // Ekstra veri gerekmez
    }

    @Override
    protected void onBlockHit(BlockHitResult hit) {
        super.onBlockHit(hit);
        if (this.getEntityWorld().isClient()) return;

        if (!(this.getOwner() instanceof PlayerEntity owner) || !owner.isAlive()) {
            this.discard();
            return;
        }

        // Kancadan sahibine doğru vektör
        Vec3d hookPos = new Vec3d(hit.getPos().x, hit.getPos().y, hit.getPos().z);
        Vec3d ownerPos = new Vec3d(owner.getX(), owner.getEyeY(), owner.getZ());
        Vec3d pull = hookPos.subtract(ownerPos).normalize();

        double speed = 2.2;
        owner.setVelocity(pull.x * speed, pull.y * speed, pull.z * speed);
        owner.velocityDirty = true;

        this.getEntityWorld().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                SoundEvents.ENTITY_FISHING_BOBBER_RETRIEVE, SoundCategory.PLAYERS, 1.0f, 1.2f);

        this.discard();
    }

    @Override
    protected void onEntityHit(EntityHitResult hit) {
        // Canlılara çarparsa hiçbir şey yapma, sadece kendini yok et
        this.discard();
    }
}
