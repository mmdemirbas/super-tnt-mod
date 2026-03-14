package com.supertntmod.block;

import com.supertntmod.entity.BedrockTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class BedrockTntBlock extends CustomTntBlock {
    public BedrockTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new BedrockTntEntity(world, x, y, z, igniter));
    }
}
