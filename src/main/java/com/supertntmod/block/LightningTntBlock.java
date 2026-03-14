package com.supertntmod.block;

import com.supertntmod.entity.LightningTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class LightningTntBlock extends CustomTntBlock {
    public LightningTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new LightningTntEntity(world, x, y, z, igniter));
    }
}
