package com.supertntmod.block;

import com.supertntmod.entity.DunyaTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class DunyaTntBlock extends CustomTntBlock {
    public DunyaTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new DunyaTntEntity(world, x, y, z, igniter));
    }
}
