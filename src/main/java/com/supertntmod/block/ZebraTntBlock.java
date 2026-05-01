package com.supertntmod.block;

import com.supertntmod.entity.ZebraTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ZebraTntBlock extends CustomTntBlock {
    public ZebraTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new ZebraTntEntity(world, x, y, z, igniter));
    }
}
