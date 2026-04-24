package com.supertntmod.block;

import com.supertntmod.entity.EndTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * End TNT: patladığında havada end taşı adası oluşturur, açık End portallı.
 */
public class EndTntBlock extends CustomTntBlock {

    public EndTntBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        EndTntEntity entity = new EndTntEntity(world, x, y, z, igniter);
        world.spawnEntity(entity);
    }
}
