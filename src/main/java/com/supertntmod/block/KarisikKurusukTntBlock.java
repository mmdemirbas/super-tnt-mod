package com.supertntmod.block;

import com.supertntmod.entity.KarisikKurusukTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class KarisikKurusukTntBlock extends CustomTntBlock {
    public KarisikKurusukTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new KarisikKurusukTntEntity(world, x, y, z, igniter));
    }
}
