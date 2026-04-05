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

import java.util.function.Consumer;

/**
 * Among Us Report: Bir mob veya oyuncuya sağ tıklayınca anında öldürür.
 */
public class AmongUsReportItem extends Item {

    public AmongUsReportItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context,
                              TooltipDisplayComponent displayComponent,
                              Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
        textConsumer.accept(Text.translatable("item.supertntmod.among_us_report.tooltip").formatted(Formatting.RED));
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (user.getEntityWorld().isClient()) {
            return ActionResult.SUCCESS;
        }

        if (!(user.getEntityWorld() instanceof ServerWorld serverWorld)) {
            return ActionResult.FAIL;
        }

        // Hedefi anında öldür
        entity.kill(serverWorld);

        // Ses ve partikül efektleri
        serverWorld.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.ENTITY_WITHER_DEATH, SoundCategory.PLAYERS, 1.0f, 2.0f);

        serverWorld.spawnParticles(ParticleTypes.SOUL,
                entity.getX(), entity.getY() + entity.getHeight() / 2, entity.getZ(),
                20, 0.3, 0.5, 0.3, 0.05);

        if (!user.isCreative()) {
            stack.decrement(1);
        }

        return ActionResult.SUCCESS;
    }
}
