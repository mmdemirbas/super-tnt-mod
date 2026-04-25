package com.supertntmod.item;

import com.supertntmod.entity.PortalProjectileEntity;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
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
 * Portal Silahı: Sağ tıklayınca portal mermisi atar.
 * İlk atış pembe portal, ikinci atış yeşil portal açar.
 * Her atışta renk değişir.
 */
public class PortalGunItem extends Item {
    // Oyuncu başına sonraki atış rengi (true = pembe)
    private static final Map<UUID, Boolean> NEXT_IS_PINK = new ConcurrentHashMap<>();

    public PortalGunItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context,
                              TooltipDisplayComponent displayComponent,
                              Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
        textConsumer.accept(Text.translatable("item.supertntmod.portal_gun.tooltip").formatted(Formatting.GRAY));
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient()) {
            boolean isPink = NEXT_IS_PINK.getOrDefault(user.getUuid(), true);

            PortalProjectileEntity projectile = new PortalProjectileEntity(world, user, isPink);
            projectile.setVelocity(user, user.getPitch(), user.getYaw(), 0.0f, 2.0f, 0.0f);
            world.spawnEntity(projectile);

            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENTITY_ENDER_PEARL_THROW, SoundCategory.PLAYERS, 1.0f, isPink ? 1.5f : 0.8f);

            // Rengi değiştir
            NEXT_IS_PINK.put(user.getUuid(), !isPink);

            // Kullanıcıya bilgi ver
            String colorKey = isPink
                    ? "item.supertntmod.portal_gun.shot_pink"
                    : "item.supertntmod.portal_gun.shot_green";
            user.sendMessage(Text.translatable(colorKey), true);
        }

        return ActionResult.SUCCESS;
    }

    public static void onPlayerDisconnect(java.util.UUID uuid) {
        NEXT_IS_PINK.remove(uuid);
    }

    public static void clearAll() {
        NEXT_IS_PINK.clear();
    }
}
