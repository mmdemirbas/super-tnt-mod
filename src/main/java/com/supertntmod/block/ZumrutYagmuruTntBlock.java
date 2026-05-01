package com.supertntmod.block;

import com.supertntmod.entity.ZumrutYagmuruTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ZumrutYagmuruTntBlock extends CustomTntBlock {
    public ZumrutYagmuruTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new ZumrutYagmuruTntEntity(world, x, y, z, igniter));
    }
}
