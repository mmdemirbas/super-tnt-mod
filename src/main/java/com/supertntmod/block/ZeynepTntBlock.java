package com.supertntmod.block;

import com.supertntmod.entity.ZeynepTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ZeynepTntBlock extends CustomTntBlock {
    public ZeynepTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new ZeynepTntEntity(world, x, y, z, igniter));
    }
}
