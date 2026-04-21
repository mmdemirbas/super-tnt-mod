package com.supertntmod.block;

import com.supertntmod.entity.KupTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class KupTntBlock extends CustomTntBlock {
    public KupTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new KupTntEntity(world, x, y, z, igniter));
    }
}
