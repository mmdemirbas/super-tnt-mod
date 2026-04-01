package com.supertntmod.block;

import com.supertntmod.entity.MagnetTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Mıknatıs TNT: önce 3 saniye çevresindeki canlıları çeker, sonra patlar.
 */
public class MagnetTntBlock extends CustomTntBlock {

    public MagnetTntBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new MagnetTntEntity(world, x, y, z, igniter));
    }
}
