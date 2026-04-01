package com.supertntmod.block;

import com.supertntmod.entity.SwapTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Takas TNT: patlayınca yakındaki canlıların konumlarını karıştırır.
 */
public class SwapTntBlock extends CustomTntBlock {

    public SwapTntBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new SwapTntEntity(world, x, y, z, igniter));
    }
}
