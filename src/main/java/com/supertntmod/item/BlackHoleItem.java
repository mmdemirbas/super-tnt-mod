package com.supertntmod.item;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.function.Consumer;

/**
 * Kara Delik: Kullanıldığında 15 blok yarıçapındaki tüm canlıları kör eder,
 * kendine doğru çeker ve hasar verir.
 * Cooldown: 10 saniye. Tek kullanımlık (survival'da).
 */
public class BlackHoleItem extends Item {

    private static final int RADIUS = 15;
    private static final int BLINDNESS_DURATION = 100; // 5 saniye
    private static final float PULL_DAMAGE = 10.0f; // 5 kalp
    private static final double PULL_STRENGTH = 0.6;

    public BlackHoleItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context,
                              TooltipDisplayComponent displayComponent,
                              Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
        textConsumer.accept(Text.translatable("item.supertntmod.black_hole.tooltip")
                .formatted(Formatting.GRAY));
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient()) return ActionResult.SUCCESS;

        Vec3d center = new Vec3d(user.getX(), user.getY(), user.getZ());

        // Partikül efekti: siyah/mor girdap
        if (world instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                    center.x, center.y + 1, center.z,
                    200, 3.0, 2.0, 3.0, 0.1);
            sw.spawnParticles(ParticleTypes.PORTAL,
                    center.x, center.y + 1, center.z,
                    100, 2.0, 1.5, 2.0, 0.5);
            sw.spawnParticles(ParticleTypes.WITCH,
                    center.x, center.y + 1, center.z,
                    50, 1.0, 1.0, 1.0, 0.1);
        }

        // Ses efekti
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 2.0f, 0.3f);
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENTITY_WITHER_AMBIENT, SoundCategory.PLAYERS, 1.0f, 0.5f);

        // Yarıçaptaki tüm canlıları etkile
        world.getEntitiesByClass(LivingEntity.class,
                new Box(user.getBlockPos()).expand(RADIUS),
                e -> e.isAlive() && e != user
        ).forEach(entity -> {
            // Körlük efekti
            entity.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.BLINDNESS, BLINDNESS_DURATION, 0));
            entity.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.DARKNESS, BLINDNESS_DURATION, 0));

            // Çekme kuvveti (oyuncuya doğru)
            Vec3d entityPos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
            Vec3d toUser = center.subtract(entityPos).normalize();
            double dist = entityPos.distanceTo(center);
            double force = Math.min(1.5, PULL_STRENGTH * (RADIUS / Math.max(1.0, dist)));
            entity.addVelocity(toUser.x * force, toUser.y * force * 0.3 + 0.2, toUser.z * force);
            entity.velocityDirty = true;

            // Hasar
            if (world instanceof ServerWorld sw) {
                entity.damage(sw, world.getDamageSources().magic(), PULL_DAMAGE);
            }
        });

        // Dayanıklılık düşür (15 kullanım sınırı)
        ItemStack stack = user.getStackInHand(hand);
        if (!user.isCreative()) {
            stack.damage(1, user, hand == Hand.MAIN_HAND
                    ? net.minecraft.entity.EquipmentSlot.MAINHAND
                    : net.minecraft.entity.EquipmentSlot.OFFHAND);
        }

        // Cooldown (10 saniye)
        user.getItemCooldownManager().set(stack, 200);

        return ActionResult.SUCCESS;
    }
}
