package com.supertntmod.block;

import com.supertntmod.entity.CommandTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class CommandTntBlock extends CustomTntBlock {
    public CommandTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new CommandTntEntity(world, x, y, z, igniter));
    }
}
