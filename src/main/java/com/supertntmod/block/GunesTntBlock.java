package com.supertntmod.block;

import com.supertntmod.entity.GunesTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class GunesTntBlock extends CustomTntBlock {
    public GunesTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new GunesTntEntity(world, x, y, z, igniter));
    }
}
