package com.supertntmod.block;

import com.supertntmod.entity.MagaraTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class MagaraTntBlock extends CustomTntBlock {
    public MagaraTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new MagaraTntEntity(world, x, y, z, igniter));
    }
}
