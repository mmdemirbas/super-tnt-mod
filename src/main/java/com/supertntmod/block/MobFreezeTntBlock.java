package com.supertntmod.block;

import com.supertntmod.entity.MobFreezeTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Süper TNT (Mob Dondurma TNT):
 * Bloklara zarar vermez, tüm düşman mobları 10 dakika dondurur.
 */
public class MobFreezeTntBlock extends CustomTntBlock {
    public MobFreezeTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new MobFreezeTntEntity(world, x, y, z, igniter));
    }
}
