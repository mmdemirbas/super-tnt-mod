package com.supertntmod.block;

import com.supertntmod.entity.PastaTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class PastaTntBlock extends CustomTntBlock {
    public PastaTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new PastaTntEntity(world, x, y, z, igniter));
    }
}
