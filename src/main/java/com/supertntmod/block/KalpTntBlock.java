package com.supertntmod.block;

import com.supertntmod.entity.KalpTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class KalpTntBlock extends CustomTntBlock {
    public KalpTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new KalpTntEntity(world, x, y, z, igniter));
    }
}
