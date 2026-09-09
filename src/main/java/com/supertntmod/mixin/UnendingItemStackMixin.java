package com.supertntmod.mixin;

import com.supertntmod.item.ModEnchantments;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tükenmezlik büyülü yığınlar eksilmez.
 *
 * decrement(int) tek nokta olarak yeterli: decrementUnlessCreative ve
 * splitUnlessCreative dahil, yığın tüketen bütün vanilla yolları buradan geçer.
 * Dayanıklılık ve ok tüketimi ayrıca büyünün item_damage / ammo_use
 * efektleriyle kapatılıyor — onlar mixin gerektirmiyor.
 */
@Mixin(ItemStack.class)
public class UnendingItemStackMixin {

    @Inject(method = "decrement(I)V", at = @At("HEAD"), cancellable = true)
    private void supertntmod$keepUnendingStack(int amount, CallbackInfo ci) {
        if (ModEnchantments.isUnending((ItemStack) (Object) this)) {
            ci.cancel();
        }
    }
}
