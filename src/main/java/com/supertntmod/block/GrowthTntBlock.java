package com.supertntmod.block;

import com.supertntmod.entity.GrowthTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class GrowthTntBlock extends CustomTntBlock {
    public GrowthTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new GrowthTntEntity(world, x, y, z, igniter));
    }
}
