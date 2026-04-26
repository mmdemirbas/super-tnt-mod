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
import net.minecraft.util.Formatting;

import java.util.function.Consumer;

/**
 * Kalp Baltası: lacivert balta. Mob'lara vurulduğunda canına bakılmaksızın
 * anında öldürür. Oyunculara vurulduğunda ise tek vuruşta 250 can
 * (= 125 kalp) götürür.
 */
public class HeartAxeItem extends Item {

    private static final float PLAYER_DAMAGE = 250.0f;

    public HeartAxeItem(Settings settings) {
        super(settings);
    }

    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.postHit(stack, target, attacker);
        if (!(target.getEntityWorld() instanceof ServerWorld sw)) return;

        if (target instanceof PlayerEntity) {
            // Oyuncuya 250 can hasar (vanilla canı 20 olsa bile component'lardan
            // gelen ek canı eziyor; default cana karşı zaten ölümcül).
            target.damage(sw, sw.getDamageSources().playerAttack(
                    attacker instanceof PlayerEntity p ? p : null), PLAYER_DAMAGE);
        } else {
            // Mob: doğrudan öldür (canı ne olursa olsun).
            target.kill(sw);
        }

        // Görsel + ses efekti
        sw.spawnParticles(ParticleTypes.HEART,
                target.getX(), target.getY() + target.getHeight() * 0.7, target.getZ(),
                12, 0.4, 0.4, 0.4, 0.05);
        sw.spawnParticles(ParticleTypes.SOUL,
                target.getX(), target.getY() + target.getHeight() * 0.5, target.getZ(),
                20, 0.3, 0.5, 0.3, 0.02);
        sw.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.ENTITY_WITHER_DEATH, SoundCategory.PLAYERS, 0.7f, 1.4f);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context,
                              TooltipDisplayComponent displayComponent,
                              Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
        textConsumer.accept(Text.translatable("item.supertntmod.heart_axe.tooltip").formatted(Formatting.DARK_BLUE));
    }
}
