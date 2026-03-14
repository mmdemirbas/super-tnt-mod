package com.supertntmod.block;

import com.supertntmod.entity.GoldTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class GoldTntBlock extends CustomTntBlock {
    public GoldTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new GoldTntEntity(world, x, y, z, igniter));
    }
}
