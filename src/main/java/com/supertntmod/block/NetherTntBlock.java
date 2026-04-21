package com.supertntmod.block;

import com.supertntmod.entity.NetherTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Nether TNT: patladığında havada büyük bir netherrack adası oluşturur,
 * üstünde 15 nether portalı ile.
 */
public class NetherTntBlock extends CustomTntBlock {

    public NetherTntBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        NetherTntEntity entity = new NetherTntEntity(world, x, y, z, igniter);
        world.spawnEntity(entity);
    }
}
