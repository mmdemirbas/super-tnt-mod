package com.supertntmod.item;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.function.Consumer;

public class LaserSwordItem extends Item {

    private static final double RANGE = 9.0;
    private static final float ENTITY_DAMAGE = 30.0f;
    private static final int COOLDOWN_TICKS = 400;

    public LaserSwordItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context,
                              TooltipDisplayComponent displayComponent,
                              Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
        textConsumer.accept(Text.translatable("item.supertntmod.laser_sword.tooltip")
                .formatted(Formatting.RED));
        textConsumer.accept(Text.translatable("item.supertntmod.laser_sword.tooltip2")
                .formatted(Formatting.GRAY));
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient()) return ActionResult.SUCCESS;
        if (!(world instanceof ServerWorld sw)) return ActionResult.PASS;

        ItemStack stack = user.getStackInHand(hand);
        if (user.getItemCooldownManager().isCoolingDown(stack)) return ActionResult.FAIL;

        Vec3d start = user.getEyePos();
        Vec3d look = user.getRotationVec(1.0f);
        Vec3d end = start.add(look.multiply(RANGE));

        LivingEntity hitEntity = findClosestEntity(world, user, start, end);

        if (hitEntity != null) {
            Vec3d hitCenter = new Vec3d(hitEntity.getX(), hitEntity.getY() + hitEntity.getHeight() / 2.0, hitEntity.getZ());
            spawnLaserBeam(sw, start, hitCenter);
            hitEntity.damage(sw, world.getDamageSources().playerAttack(user), ENTITY_DAMAGE);
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 1.2f, 1.8f);
        } else {
            BlockHitResult blockHit = world.raycast(new RaycastContext(
                    start, end, RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE, user));

            if (blockHit.getType() == HitResult.Type.BLOCK) {
                Vec3d blockCenter = Vec3d.ofCenter(blockHit.getBlockPos());
                spawnLaserBeam(sw, start, blockCenter);
                world.breakBlock(blockHit.getBlockPos(), true, user);
            } else {
                spawnLaserBeam(sw, start, end);
            }
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 1.0f, 1.8f);
        }

        user.getItemCooldownManager().set(stack, COOLDOWN_TICKS);
        return ActionResult.SUCCESS;
    }

    private LivingEntity findClosestEntity(World world, PlayerEntity user, Vec3d start, Vec3d end) {
        Box searchBox = new Box(start, end).expand(1.0);
        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;
        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, searchBox,
                e -> e != user && e.isAlive())) {
            Box entityBox = entity.getBoundingBox().expand(0.3);
            Optional<Vec3d> hit = entityBox.raycast(start, end);
            if (hit.isPresent()) {
                double dist = start.squaredDistanceTo(hit.get());
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = entity;
                }
            }
        }
        return closest;
    }

    private void spawnLaserBeam(ServerWorld world, Vec3d from, Vec3d to) {
        Vec3d dir = to.subtract(from);
        double length = dir.length();
        if (length < 0.01) return;
        Vec3d step = dir.normalize().multiply(0.25);
        int steps = (int) Math.ceil(length / 0.25);

        Vec3d pos = from;
        for (int i = 0; i < steps; i++) {
            world.spawnParticles(ParticleTypes.END_ROD,
                    pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                    pos.x, pos.y, pos.z, 2, 0.04, 0.04, 0.04, 0.01);
            if (i % 3 == 0) {
                world.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                        pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0.01);
            }
            pos = pos.add(step);
        }

        world.spawnParticles(ParticleTypes.EXPLOSION, to.x, to.y, to.z, 3, 0.2, 0.2, 0.2, 0.05);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, to.x, to.y, to.z, 20, 0.3, 0.3, 0.3, 0.1);
        world.spawnParticles(ParticleTypes.END_ROD, to.x, to.y, to.z, 12, 0.2, 0.2, 0.2, 0.15);
    }
}
