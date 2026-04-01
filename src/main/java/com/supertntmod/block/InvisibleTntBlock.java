package com.supertntmod.block;

import com.supertntmod.entity.InvisibleTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Görünmez TNT: yerleştirildiğinde sıradan taş gibi görünür.
 * Ateşlenince standart bir patlama yaratır.
 */
public class InvisibleTntBlock extends CustomTntBlock {

    public InvisibleTntBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new InvisibleTntEntity(world, x, y, z, igniter));
    }
}
