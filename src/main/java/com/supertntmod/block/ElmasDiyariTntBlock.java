package com.supertntmod.block;

import com.supertntmod.entity.ElmasDiyariTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ElmasDiyariTntBlock extends CustomTntBlock {
    public ElmasDiyariTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new ElmasDiyariTntEntity(world, x, y, z, igniter));
    }
}
