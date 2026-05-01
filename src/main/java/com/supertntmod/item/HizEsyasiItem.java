package com.supertntmod.item;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Hız Eşyası: Kullanıldığında 1 dakika hız verir; bu süre içinde
 * oyuncu bir şeye çarparsa (duvar, sert düşüş) patlar.
 */
public class HizEsyasiItem extends Item {
    private static final int DURATION_TICKS = 1200; // 60 saniye
    public static final Map<UUID, Integer> ACTIVE = new ConcurrentHashMap<>();

    public HizEsyasiItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient()) return ActionResult.SUCCESS;

        // Hız II — 60 saniye
        user.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, DURATION_TICKS, 1));
        // Sıçrama I — 60 saniye (hızlı dolaşmayı destekler)
        user.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, DURATION_TICKS, 0));

        // Çarpışma izleme listesine ekle
        ACTIVE.put(user.getUuid(), DURATION_TICKS);

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1.0f, 1.5f);

        if (!user.isCreative()) {
            stack.decrement(1);
        }
        if (user instanceof ServerPlayerEntity sp) {
            sp.sendMessage(Text.translatable("item.supertntmod.hiz_esyasi.activated")
                    .formatted(Formatting.RED), true);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context,
                              TooltipDisplayComponent displayComponent,
                              Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
        textConsumer.accept(Text.translatable("item.supertntmod.hiz_esyasi.tooltip")
                .formatted(Formatting.RED));
        textConsumer.accept(Text.translatable("item.supertntmod.hiz_esyasi.tooltip2")
                .formatted(Formatting.GRAY));
    }
}
