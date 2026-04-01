package com.supertntmod.block;

import com.supertntmod.entity.BounceTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Zıplatan TNT: patlayınca yakındaki tüm canlıları havalandırır.
 */
public class BounceTntBlock extends CustomTntBlock {

    public BounceTntBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new BounceTntEntity(world, x, y, z, igniter));
    }
}
