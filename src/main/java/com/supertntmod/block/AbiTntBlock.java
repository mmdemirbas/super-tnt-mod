package com.supertntmod.block;

import com.supertntmod.entity.AbiTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class AbiTntBlock extends CustomTntBlock {
    public AbiTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new AbiTntEntity(world, x, y, z, igniter));
    }
}
