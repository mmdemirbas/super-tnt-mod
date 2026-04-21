package com.supertntmod.block;

import com.supertntmod.entity.ZehirTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ZehirTntBlock extends CustomTntBlock {
    public ZehirTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new ZehirTntEntity(world, x, y, z, igniter));
    }
}
