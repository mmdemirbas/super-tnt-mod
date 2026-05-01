package com.supertntmod.block;

import com.supertntmod.entity.AnneTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class AnneTntBlock extends CustomTntBlock {
    public AnneTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new AnneTntEntity(world, x, y, z, igniter));
    }
}
