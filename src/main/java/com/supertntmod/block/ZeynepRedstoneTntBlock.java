package com.supertntmod.block;

import com.supertntmod.entity.ZeynepRedstoneTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ZeynepRedstoneTntBlock extends CustomTntBlock {
    public ZeynepRedstoneTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new ZeynepRedstoneTntEntity(world, x, y, z, igniter));
    }
}
