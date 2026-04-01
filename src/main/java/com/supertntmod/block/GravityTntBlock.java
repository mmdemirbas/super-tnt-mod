package com.supertntmod.block;

import com.supertntmod.entity.GravityTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Yerçekimi TNT: patlayınca yakındaki canlıların yerçekimini ters çevirir.
 */
public class GravityTntBlock extends CustomTntBlock {

    public GravityTntBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new GravityTntEntity(world, x, y, z, igniter));
    }
}
