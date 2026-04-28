package com.supertntmod.block;

import com.supertntmod.entity.DagTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class DagTntBlock extends CustomTntBlock {
    public DagTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new DagTntEntity(world, x, y, z, igniter));
    }
}
