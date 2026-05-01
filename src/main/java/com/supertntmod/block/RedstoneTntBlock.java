package com.supertntmod.block;

import com.supertntmod.entity.RedstoneTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class RedstoneTntBlock extends CustomTntBlock {
    public RedstoneTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new RedstoneTntEntity(world, x, y, z, igniter));
    }
}
