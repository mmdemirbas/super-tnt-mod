package com.supertntmod.block;

import com.supertntmod.entity.YagmurTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class YagmurTntBlock extends CustomTntBlock {
    public YagmurTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new YagmurTntEntity(world, x, y, z, igniter));
    }
}
