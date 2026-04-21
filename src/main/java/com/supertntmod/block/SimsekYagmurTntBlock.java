package com.supertntmod.block;

import com.supertntmod.entity.SimsekYagmurTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class SimsekYagmurTntBlock extends CustomTntBlock {
    public SimsekYagmurTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new SimsekYagmurTntEntity(world, x, y, z, igniter));
    }
}
