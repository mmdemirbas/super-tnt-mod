package com.supertntmod.block;

import com.supertntmod.entity.UykuTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class UykuTntBlock extends CustomTntBlock {
    public UykuTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new UykuTntEntity(world, x, y, z, igniter));
    }
}
