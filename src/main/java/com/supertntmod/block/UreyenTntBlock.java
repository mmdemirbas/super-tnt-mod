package com.supertntmod.block;

import com.supertntmod.entity.UreyenTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class UreyenTntBlock extends CustomTntBlock {
    public UreyenTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new UreyenTntEntity(world, x, y, z, igniter));
    }
}
