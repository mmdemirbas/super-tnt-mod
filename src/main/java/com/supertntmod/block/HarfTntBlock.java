package com.supertntmod.block;

import com.supertntmod.entity.HarfTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class HarfTntBlock extends CustomTntBlock {
    public HarfTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new HarfTntEntity(world, x, y, z, igniter));
    }
}
