package com.supertntmod.block;

import com.supertntmod.entity.WaterTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class WaterTntBlock extends CustomTntBlock {
    public WaterTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new WaterTntEntity(world, x, y, z, igniter));
    }
}
