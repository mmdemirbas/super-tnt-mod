package com.supertntmod.block;

import com.supertntmod.entity.IkiYuzTlTntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class IkiYuzTlTntBlock extends CustomTntBlock {
    public IkiYuzTlTntBlock(Settings settings) { super(settings); }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        world.spawnEntity(new IkiYuzTlTntEntity(world, x, y, z, igniter));
    }
}
