package com.supertntmod.block;

import com.supertntmod.entity.CamTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class CamTntBlock extends CustomTntBlock {
    public CamTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new CamTntEntity(world, x, y, z, igniter));
    }
}
