package com.supertntmod.block;

import com.supertntmod.entity.GulenYuzTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class GulenYuzTntBlock extends CustomTntBlock {
    public GulenYuzTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new GulenYuzTntEntity(world, x, y, z, igniter));
    }
}
