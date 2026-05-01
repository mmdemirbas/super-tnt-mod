package com.supertntmod.block;

import com.supertntmod.entity.CokluZeynepTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class CokluZeynepTntBlock extends CustomTntBlock {
    public CokluZeynepTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new CokluZeynepTntEntity(world, x, y, z, igniter));
    }
}
