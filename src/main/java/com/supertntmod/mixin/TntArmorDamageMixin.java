package com.supertntmod.mixin;

import com.supertntmod.item.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * TNT Zırh: Giyen oyuncuya vuran kişi/mob patlar.
 * En az bir parça TNT zırhı giymek yeterli.
 */
@Mixin(LivingEntity.class)
public abstract class TntArmorDamageMixin {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    @Inject(method = "damage", at = @At("HEAD"))
    private void supertntmod$onDamage(ServerWorld world, DamageSource source, float amount,
                                       CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayerEntity player)) return;

        Entity attacker = source.getAttacker();
        if (attacker == null || attacker == self) return;

        // En az bir TNT zırh parçası giyiyor mu?
        boolean wearingTntArmor = false;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getEquippedStack(slot);
            if (isTntArmorPiece(stack)) {
                wearingTntArmor = true;
                // Patlama her zırh parçasını 5 puan yıpratır
                stack.damage(5, player, slot);
            }
        }

        if (wearingTntArmor) {
            // Sadece saldırgana hasar — patlama görseldir, etrafa (giyenin de
            // dahil olduğu) hasar vermez. Aksi halde bitişik melee saldırılar
            // sahibi de patlatır.
            if (attacker instanceof LivingEntity livingAttacker) {
                livingAttacker.damage(world,
                        world.getDamageSources().explosion(player, player), 8.0f);
            }
            world.createExplosion(
                    null,
                    attacker.getX(), attacker.getBodyY(0.5), attacker.getZ(),
                    0.0f,
                    World.ExplosionSourceType.NONE
            );
        }
    }

    private static boolean isTntArmorPiece(ItemStack stack) {
        return stack.isOf(ModItems.TNT_ARMOR_HELMET)
                || stack.isOf(ModItems.TNT_ARMOR_CHESTPLATE)
                || stack.isOf(ModItems.TNT_ARMOR_LEGGINGS)
                || stack.isOf(ModItems.TNT_ARMOR_BOOTS);
    }
}
