package com.supertntmod.block;

import com.supertntmod.entity.VirusTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class VirusTntBlock extends CustomTntBlock {
    public VirusTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new VirusTntEntity(world, x, y, z, igniter));
    }
}
