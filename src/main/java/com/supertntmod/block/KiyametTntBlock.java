package com.supertntmod.block;

import com.supertntmod.entity.KiyametTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class KiyametTntBlock extends CustomTntBlock {
    public KiyametTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new KiyametTntEntity(world, x, y, z, igniter));
    }
}
