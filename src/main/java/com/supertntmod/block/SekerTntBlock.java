package com.supertntmod.block;

import com.supertntmod.entity.SekerTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class SekerTntBlock extends CustomTntBlock {
    public SekerTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new SekerTntEntity(world, x, y, z, igniter));
    }
}
