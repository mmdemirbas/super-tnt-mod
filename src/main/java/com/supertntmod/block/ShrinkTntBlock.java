package com.supertntmod.block;

import com.supertntmod.entity.ShrinkTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ShrinkTntBlock extends CustomTntBlock {
    public ShrinkTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new ShrinkTntEntity(world, x, y, z, igniter));
    }
}
