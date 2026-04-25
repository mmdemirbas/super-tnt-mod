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

        // Yalnızca ayağın hemen altını doldur. feetPos'a yün koymak oyuncuyu
        // bloğun içinde bırakıp çarpışma fiziği ile yukarı itiyordu — zıplama
        // kırılıyor ve dar tavanlı koridorda kid kapana kısılıyordu.
        BlockPos below = player.getBlockPos().down();
        if (world.getBlockState(below).isAir()) {
            world.setBlockState(below, Blocks.WHITE_WOOL.getDefaultState());
        }
    }
}
