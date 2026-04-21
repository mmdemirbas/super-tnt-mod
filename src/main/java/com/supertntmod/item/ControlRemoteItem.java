package com.supertntmod.item;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.function.Consumer;

/**
 * Kontrol Kumandası: T tuşuna basıldığında 25 blok yarıçapındaki tüm canlıları
 * 1.5 dakika boyunca kör eder ve dondurur.
 * Server-side mantık: ControlRemoteC2SPayload ile tetiklenir.
 */
public class ControlRemoteItem extends Item {

    private static final int RADIUS = 25;
    private static final int EFFECT_DURATION = 1800; // 1.5 dakika = 1800 tick
    private static final int COOLDOWN_TICKS = 600; // 30 saniye cooldown

    public ControlRemoteItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context,
                              TooltipDisplayComponent displayComponent,
                              Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
        textConsumer.accept(Text.translatable("item.supertntmod.control_remote.tooltip")
                .formatted(Formatting.GRAY));
        textConsumer.accept(Text.translatable("item.supertntmod.control_remote.tooltip2")
                .formatted(Formatting.RED));
    }

    /**
     * Server-side: T tuşu ile tetiklenen efekt.
     * SuperTntMod'daki payload handler'dan çağrılır.
     */
    public static void activate(ServerPlayerEntity player) {
        World world = player.getEntityWorld();
        if (!(world instanceof ServerWorld serverWorld)) return;

        // Elinde Kontrol Kumandası var mı kontrol et
        ItemStack mainHand = player.getMainHandStack();
        ItemStack offHand = player.getOffHandStack();
        boolean holdingRemote = mainHand.isOf(ModItems.CONTROL_REMOTE)
                || offHand.isOf(ModItems.CONTROL_REMOTE);
        if (!holdingRemote) return;

        // Kumandanın bulunduğu el stack'ini belirle
        ItemStack remoteStack = mainHand.isOf(ModItems.CONTROL_REMOTE) ? mainHand : offHand;

        // Cooldown kontrolü
        if (player.getItemCooldownManager().isCoolingDown(remoteStack)) return;

        // Efektler
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 2.0f, 0.5f);
        serverWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                player.getX(), player.getY() + 1, player.getZ(),
                100, 5.0, 3.0, 5.0, 0.3);

        // Yarıçaptaki tüm canlıları etkile (oyuncuyu hariç tut)
        world.getEntitiesByClass(LivingEntity.class,
                new Box(player.getBlockPos()).expand(RADIUS),
                e -> e.isAlive() && e != player
        ).forEach(entity -> {
            // Körlük + Karanlık
            entity.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.BLINDNESS, EFFECT_DURATION, 0));
            entity.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.DARKNESS, EFFECT_DURATION, 0));

            // Dondurma: Yavaşlık 255 (tamamen hareketsiz)
            entity.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SLOWNESS, EFFECT_DURATION, 255));

            // Dondurma partikülü
            if (world instanceof ServerWorld sw) {
                sw.spawnParticles(ParticleTypes.SNOWFLAKE,
                        entity.getX(), entity.getY() + 1, entity.getZ(),
                        20, 0.5, 0.5, 0.5, 0.05);
            }
        });

        // Cooldown uygula
        player.getItemCooldownManager().set(remoteStack, COOLDOWN_TICKS);

        player.sendMessage(Text.translatable("message.supertntmod.control_remote.activated"), true);
    }
}
