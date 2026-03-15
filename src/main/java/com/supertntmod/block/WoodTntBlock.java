package com.supertntmod.block;

import com.supertntmod.entity.WoodTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class WoodTntBlock extends CustomTntBlock {
    public WoodTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new WoodTntEntity(world, x, y, z, igniter));
    }
}
