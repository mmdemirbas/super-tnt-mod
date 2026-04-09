package com.supertntmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Ender Send: 20 blok boyunda dev bir düşman mob.
 * Enderman benzeri davranış: göz temasıyla saldırır, ışınlanabilir.
 * Yüksek HP, güçlü saldırı.
 */
public class EnderSendEntity extends HostileEntity {

    private static final double TELEPORT_CHANCE = 0.03; // Her tick %3 ışınlanma şansı

    public EnderSendEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.MAX_HEALTH, 300.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.35)
                .add(EntityAttributes.ATTACK_DAMAGE, 15.0)
                .add(EntityAttributes.FOLLOW_RANGE, 64.0)
                .add(EntityAttributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(EntityAttributes.STEP_HEIGHT, 2.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 32.0f));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<PlayerEntity>(this, PlayerEntity.class, true,
                (entity, world) -> shouldTarget(entity)));
    }

    /**
     * Enderman benzeri: oyuncu mob'a bakıyorsa hedef al.
     */
    private boolean shouldTarget(LivingEntity target) {
        if (!(target instanceof PlayerEntity player)) return false;
        // Oyuncu bakıyorsa hedef al
        Vec3d playerLook = player.getRotationVec(1.0f).normalize();
        Vec3d toMob = new Vec3d(
                this.getX() - player.getX(),
                this.getEyeY() - player.getEyeY(),
                this.getZ() - player.getZ()
        ).normalize();
        double dot = playerLook.dotProduct(toMob);
        // Bakış açısı 20 derece içindeyse ve 32 blok mesafedeyse
        double distance = this.squaredDistanceTo(player);
        return dot > 0.94 && distance < 1024; // 32^2
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        boolean result = super.damage(world, source, amount);
        if (result && !this.getEntityWorld().isClient()) {
            // Hasar aldığında ışınlanma şansı
            if (this.random.nextDouble() < 0.3) {
                teleportRandomly();
            }
        }
        return result;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.getEntityWorld().isClient()) {
            // Hedef varken arada ışınlanma
            if (this.getTarget() != null && this.random.nextDouble() < TELEPORT_CHANCE) {
                teleportRandomly();
            }
        }

        // Partikül efekti (server-side, Enderman benzeri mor parçacıklar)
        if (this.getEntityWorld() instanceof ServerWorld sw && this.age % 5 == 0) {
            sw.spawnParticles(ParticleTypes.PORTAL,
                    this.getX(), this.getY() + this.getHeight() * 0.5, this.getZ(),
                    5, 1.0, this.getHeight() * 0.3, 1.0, 0.0);
        }
    }

    private void teleportRandomly() {
        double oldX = this.getX();
        double oldY = this.getY();
        double oldZ = this.getZ();
        double x = oldX + (this.random.nextDouble() - 0.5) * 32.0;
        double y = oldY + (this.random.nextDouble() - 0.5) * 8.0;
        double z = oldZ + (this.random.nextDouble() - 0.5) * 32.0;

        if (this.teleport(x, y, z, true)) {
            this.getEntityWorld().playSound(null, oldX, oldY, oldZ,
                    SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1.0f, 0.7f);
            this.getEntityWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1.0f, 0.7f);

            if (this.getEntityWorld() instanceof ServerWorld sw) {
                sw.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                        this.getX(), this.getY() + 10, this.getZ(),
                        50, 1.5, 5.0, 1.5, 0.1);
            }
        }
    }
}
