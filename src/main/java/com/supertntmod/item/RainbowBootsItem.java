package com.supertntmod.item;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Gökkuşağı Botları: giyildiğinde adım attığın yerde beyaz yün blok çıkar.
 * Boşluğa düşmek imkânsız.
 */
public class RainbowBootsItem extends Item {

    public RainbowBootsItem(Settings settings) {
        super(settings);
    }

    /** SuperTntMod tick event'inden çağrılır. */
    public static void onTick(ServerPlayerEntity player) {
        ItemStack boots = player.getEquippedStack(EquipmentSlot.FEET);
        if (!boots.isOf(ModItems.RAINBOW_BOOTS)) return;
        ServerWorld world = (ServerWorld) player.getEntityWorld();

        BlockPos feetPos = player.getBlockPos();
        // Ayağın altındaki blok ve ayak bloğu hava ise yün koy
        BlockPos below = feetPos.down();
        if (world.getBlockState(feetPos).isAir()) {
            world.setBlockState(feetPos, Blocks.WHITE_WOOL.getDefaultState());
        }
        if (world.getBlockState(below).isAir() && !world.getBlockState(feetPos).isAir()) {
            // Sadece feetPos doluysa below'u atla (zaten ayakta)
        } else if (world.getBlockState(below).isAir()) {
            world.setBlockState(below, Blocks.WHITE_WOOL.getDefaultState());
        }
    }
}
