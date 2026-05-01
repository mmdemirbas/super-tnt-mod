package com.supertntmod.block;

import com.supertntmod.entity.KurusTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class KurusTntBlock extends CustomTntBlock {
    public KurusTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new KurusTntEntity(world, x, y, z, igniter));
    }
}
