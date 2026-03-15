package com.supertntmod.block;

import com.supertntmod.entity.RainbowTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Rainbow Dinamit: 30 blok yakınındaki blokları renkli yüne dönüştürür.
 */
public class RainbowTntBlock extends CustomTntBlock {
    public RainbowTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new RainbowTntEntity(world, x, y, z, igniter));
    }
}
