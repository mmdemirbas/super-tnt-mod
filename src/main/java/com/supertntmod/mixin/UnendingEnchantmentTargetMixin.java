package com.supertntmod.mixin;

import com.supertntmod.item.ModEnchantments;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tükenmezlik her eşyaya basılabilsin diye örsün kabul kontrolünü genişletir.
 *
 * Örs {@code Enchantment.isAcceptableItem} çağırır, o da JSON'daki
 * {@code supported_items} etiketine bakar. "Bütün eşyalar" diyen bir vanilla
 * etiketi yok; elle yazılan liste ise her Minecraft sürümünde eskir. Bu yüzden
 * kabul edilen eşya kümesinin tek yetkilisi burasıdır ve JSON'daki etiket
 * bilerek boştur.
 *
 * Büyü {@code #minecraft:in_enchanting_table} içinde olmadığı için büyü masası
 * ve köylü ticareti bundan etkilenmez; genişleme yalnızca örsü kapsar.
 */
@Mixin(Enchantment.class)
public class UnendingEnchantmentTargetMixin {

    @Inject(method = "isAcceptableItem", at = @At("HEAD"), cancellable = true)
    private void supertntmod$acceptEveryItem(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        Text description = ((Enchantment) (Object) this).description();
        if (description.getContent() instanceof TranslatableTextContent content
                && ModEnchantments.TUKENMEZLIK_DESCRIPTION_KEY.equals(content.getKey())) {
            cir.setReturnValue(!stack.isEmpty());
        }
    }
}
