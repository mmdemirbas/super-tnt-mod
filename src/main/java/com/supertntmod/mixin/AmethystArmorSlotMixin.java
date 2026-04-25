package com.supertntmod.mixin;

import com.supertntmod.item.AmethystArmorState;
import com.supertntmod.item.ModItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ametist Zırh: Gevşetilmedikçe envanter slot etkileşimini engeller.
 * Enerji Kristali ile gevşetilir (AmethystArmorState).
 */
@Mixin(ScreenHandler.class)
public abstract class AmethystArmorSlotMixin {

    @Inject(method = "internalOnSlotClick", at = @At("HEAD"), cancellable = true)
    private void supertntmod$preventAmethystRemoval(int slotIndex, int button,
                                                      SlotActionType actionType,
                                                      PlayerEntity player,
                                                      CallbackInfo ci) {
        if (!(player instanceof ServerPlayerEntity)) return;

        // Zaten gevşetilmişse engelleme
        if (AmethystArmorState.isLoosened(player.getUuid())) return;

        ScreenHandler handler = (ScreenHandler) (Object) this;

        // Slot index geçerli mi?
        if (slotIndex < 0 || slotIndex >= handler.slots.size()) return;

        Slot slot = handler.slots.get(slotIndex);
        ItemStack stackInSlot = slot.getStack();

        // Sadece zırh slot'unda (PlayerScreenHandler'da 5-8) ametist zırh varken
        // çıkarmayı engelle — envanter slot'larındaki kopyalar serbestçe taşınabilir,
        // aksi halde oyuncu zırhı hiç giyemez.
        boolean isArmorSlot = slotIndex >= 5 && slotIndex <= 8;
        if (isArmorSlot && isAmethystArmorPiece(stackInSlot)) {
            ci.cancel();
            return;
        }

        // Numpad SWAP: hotbar tuşuyla zırh slot'undaki ametist zırhı kullanılan
        // hotbar slot'una atmaya çalışıyor — engelle.
        if (actionType == SlotActionType.SWAP && isArmorSlot && isAmethystArmorPiece(stackInSlot)) {
            ci.cancel();
        }
    }

    private static boolean isAmethystArmorPiece(ItemStack stack) {
        return stack.isOf(ModItems.AMETHYST_HELMET)
                || stack.isOf(ModItems.AMETHYST_CHESTPLATE)
                || stack.isOf(ModItems.AMETHYST_LEGGINGS)
                || stack.isOf(ModItems.AMETHYST_BOOTS);
    }
}
