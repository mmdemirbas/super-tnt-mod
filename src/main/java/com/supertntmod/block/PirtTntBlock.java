package com.supertntmod.block;

import com.supertntmod.entity.PirtTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class PirtTntBlock extends CustomTntBlock {
    public PirtTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new PirtTntEntity(world, x, y, z, igniter));
    }
}
