package com.supertntmod.block;

import com.supertntmod.entity.MadenTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class MadenTntBlock extends CustomTntBlock {
    public MadenTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new MadenTntEntity(world, x, y, z, igniter));
    }
}
