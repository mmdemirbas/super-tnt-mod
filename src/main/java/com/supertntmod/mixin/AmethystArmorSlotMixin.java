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

        // Slot'taki item ametist zırh parçası mı?
        if (isAmethystArmorPiece(stackInSlot)) {
            ci.cancel();
            return;
        }

        // Shift-click ile ametist zırhı taşıma girişimi (başka slot'tan zırh slot'una)
        if (actionType == SlotActionType.QUICK_MOVE) {
            // Bu durumda zaten slot'taki item kontrol edildi, ek kontrol gerekmez
            return;
        }

        // Swap (numpad) ile zırh slot'undaki ametist zırhı değiştirme girişimi
        if (actionType == SlotActionType.SWAP) {
            // 5-8 arası armor slot'ları (PlayerScreenHandler'da)
            // Swap edilen slot'u kontrol et
            for (int armorSlot = 5; armorSlot <= 8; armorSlot++) {
                if (armorSlot < handler.slots.size()) {
                    ItemStack armorStack = handler.slots.get(armorSlot).getStack();
                    if (isAmethystArmorPiece(armorStack) && slotIndex != armorSlot) {
                        // Numpad ile swap edilen slot armor slot'unu hedefliyorsa
                        // Button değeri hotbar slot indeksini temsil eder
                        ci.cancel();
                        return;
                    }
                }
            }
        }
    }

    private static boolean isAmethystArmorPiece(ItemStack stack) {
        return stack.isOf(ModItems.AMETHYST_HELMET)
                || stack.isOf(ModItems.AMETHYST_CHESTPLATE)
                || stack.isOf(ModItems.AMETHYST_LEGGINGS)
                || stack.isOf(ModItems.AMETHYST_BOOTS);
    }
}
