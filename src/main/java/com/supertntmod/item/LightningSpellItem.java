package com.supertntmod.item;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.function.Consumer;

/**
 * Yıldırım Büyüsü: Sağ tıklayınca bakılan noktaya yıldırım çaktırır.
 * Menzil: 64 blok. Dayanıklılık: 32 kullanım.
 */
public class LightningSpellItem extends Item {

    private static final double RANGE = 64.0;

    public LightningSpellItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context,
                              TooltipDisplayComponent displayComponent,
                              Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
        textConsumer.accept(Text.translatable("item.supertntmod.lightning_spell.tooltip")
                .formatted(Formatting.GRAY));
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        // Bakılan noktayı bul (raycast)
        Vec3d start = user.getEyePos();
        Vec3d direction = user.getRotationVec(1.0f);
        Vec3d end = start.add(direction.multiply(RANGE));

        BlockHitResult hitResult = world.raycast(new RaycastContext(
                start, end,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                user));

        Vec3d hitPos;
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            hitPos = Vec3d.ofCenter(hitResult.getBlockPos().up());
        } else {
            hitPos = end;
        }

        // Yıldırım oluştur
        LightningEntity bolt = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
        bolt.setPos(hitPos.x, hitPos.y, hitPos.z);
        world.spawnEntity(bolt);

        // Efektler
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 1.0f, 1.2f);

        if (world instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.END_ROD,
                    user.getX(), user.getEyeY(), user.getZ(),
                    10, 0.2, 0.2, 0.2, 0.1);
        }

        // Dayanıklılık düşür
        ItemStack stack = user.getStackInHand(hand);
        if (!user.isCreative()) {
            stack.damage(1, user, hand == Hand.MAIN_HAND
                    ? net.minecraft.entity.EquipmentSlot.MAINHAND
                    : net.minecraft.entity.EquipmentSlot.OFFHAND);
        }

        // Cooldown (1 saniye = 20 tick)
        user.getItemCooldownManager().set(stack, 20);

        return ActionResult.SUCCESS;
    }
}
