package com.supertntmod.item;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * 🦝 Eşya Çalmaca — kullanılınca 10 blok yarıçapındaki en yakın başka oyuncunun
 * ana envanterini sandık benzeri bir UI olarak açar. Oyuncu eşyaları kendi
 * envanterine sürükleyebilir (vanilla mekanik).
 *
 * UI: vanilla generic_9x4 chest UI; üstte hedef oyuncunun 36 slotu,
 * altta kullanıcının kendi envanteri (ekstra bir UI elementine gerek yok).
 *
 * Spec'teki "mob da olabilir" kısmı dışarıda bırakıldı — moblar
 * (vanilla'daki gibi) envanter taşımıyor; sadece zırh slot'ları taşıyanlar
 * için sınırlı destek olurdu, bu da farklı bir UI gerektirir. Pragmatik olarak
 * sadece oyuncular hedeflenebilir.
 */
public class EsyaCalmacaItem extends Item {

    private static final double RADIUS = 10.0;

    public EsyaCalmacaItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context,
                              TooltipDisplayComponent displayComponent,
                              Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
        textConsumer.accept(Text.translatable("item.supertntmod.esya_calmaca.tooltip").formatted(Formatting.GRAY));
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient()) return ActionResult.SUCCESS;
        if (!(user instanceof ServerPlayerEntity sp)) return ActionResult.PASS;

        ServerPlayerEntity target = findNearestOtherPlayer(sp);
        if (target == null) {
            sp.sendMessage(Text.translatable("item.supertntmod.esya_calmaca.no_target"), true);
            return ActionResult.SUCCESS;
        }

        sp.openHandledScreen(makeFactory(target));
        sp.getEntityWorld().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                SoundEvents.BLOCK_ENDER_CHEST_OPEN, SoundCategory.PLAYERS, 0.5f, 1.4f);
        // Hedefe görünür uyarı
        target.sendMessage(Text.translatable("item.supertntmod.esya_calmaca.target_warned",
                sp.getName()), true);
        return ActionResult.SUCCESS;
    }

    private @Nullable ServerPlayerEntity findNearestOtherPlayer(ServerPlayerEntity self) {
        if (!(self.getEntityWorld() instanceof net.minecraft.server.world.ServerWorld sw)) return null;
        List<ServerPlayerEntity> all = sw.getPlayers();
        ServerPlayerEntity best = null;
        double bestSq = RADIUS * RADIUS + 1;
        for (ServerPlayerEntity p : all) {
            if (p == self) continue;
            if (p.isSpectator()) continue;
            double dSq = p.squaredDistanceTo(self);
            if (dSq <= RADIUS * RADIUS && dSq < bestSq) {
                best = p;
                bestSq = dSq;
            }
        }
        return best;
    }

    private NamedScreenHandlerFactory makeFactory(ServerPlayerEntity target) {
        PlayerInventoryProxy proxy = new PlayerInventoryProxy(target.getInventory());
        Text title = Text.translatable("item.supertntmod.esya_calmaca.title", target.getName());

        return new NamedScreenHandlerFactory() {
            @Override
            public Text getDisplayName() {
                return title;
            }

            @Override
            public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory userInv, PlayerEntity playerEntity) {
                return new GenericContainerScreenHandler(
                        ScreenHandlerType.GENERIC_9X4, syncId, userInv, proxy, 4);
            }
        };
    }
}
