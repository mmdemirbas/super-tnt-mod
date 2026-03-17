package com.supertntmod.block;

import com.supertntmod.entity.CleanseTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class CleanseTntBlock extends CustomTntBlock {
    public CleanseTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new CleanseTntEntity(world, x, y, z, igniter));
    }
}
