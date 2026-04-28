package com.supertntmod.block;

import com.supertntmod.entity.AyTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class AyTntBlock extends CustomTntBlock {
    public AyTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new AyTntEntity(world, x, y, z, igniter));
    }
}
