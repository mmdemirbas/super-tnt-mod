package com.supertntmod.block;

import com.supertntmod.entity.ElmasZirhTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ElmasZirhTntBlock extends CustomTntBlock {
    public ElmasZirhTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new ElmasZirhTntEntity(world, x, y, z, igniter));
    }
}
