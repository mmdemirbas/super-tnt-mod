package com.supertntmod.block;

import com.supertntmod.entity.BabaTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class BabaTntBlock extends CustomTntBlock {
    public BabaTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new BabaTntEntity(world, x, y, z, igniter));
    }
}
