package com.supertntmod.block;

import com.supertntmod.entity.GizliTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class GizliTntBlock extends CustomTntBlock {
    public GizliTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new GizliTntEntity(world, x, y, z, igniter));
    }
}
