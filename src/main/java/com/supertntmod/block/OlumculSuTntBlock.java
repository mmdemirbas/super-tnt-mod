package com.supertntmod.block;

import com.supertntmod.entity.OlumculSuTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class OlumculSuTntBlock extends CustomTntBlock {
    public OlumculSuTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new OlumculSuTntEntity(world, x, y, z, igniter));
    }
}
