package com.supertntmod.mixin;

import com.supertntmod.item.AmethystArmorState;
import com.supertntmod.item.ModItems;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ametist Zırh: ArmorItem.use sağ-tıkla-değiş yolunu da engeller.
 * AmethystArmorSlotMixin yalnızca envanter slot tıklamalarını yakalar;
 * vanilla swap-and-equip ScreenHandler üzerinden geçmez, doğrudan
 * LivingEntity.equipStack çağırır. Bu mixin orayı yakalar.
 */
@Mixin(LivingEntity.class)
public abstract class AmethystArmorEquipMixin {

    @Inject(method = "equipStack", at = @At("HEAD"), cancellable = true)
    private void supertntmod$preventAmethystReplace(EquipmentSlot slot, ItemStack stack, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayerEntity player)) return;
        if (AmethystArmorState.isLoosened(player.getUuid())) return;

        // Yalnızca zırh slot'larında geçerli (HEAD/CHEST/LEGS/FEET)
        if (slot != EquipmentSlot.HEAD && slot != EquipmentSlot.CHEST
                && slot != EquipmentSlot.LEGS && slot != EquipmentSlot.FEET) return;

        ItemStack current = player.getEquippedStack(slot);
        if (isAmethystArmorPiece(current)) {
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
