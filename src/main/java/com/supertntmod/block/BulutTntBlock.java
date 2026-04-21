package com.supertntmod.block;

import com.supertntmod.entity.BulutTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class BulutTntBlock extends CustomTntBlock {
    public BulutTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new BulutTntEntity(world, x, y, z, igniter));
    }
}
