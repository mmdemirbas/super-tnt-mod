package com.supertntmod.block;

import com.supertntmod.entity.NuclearTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class NuclearTntBlock extends CustomTntBlock {
    public NuclearTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new NuclearTntEntity(world, x, y, z, igniter));
    }
}
