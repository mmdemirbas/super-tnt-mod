package com.supertntmod.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * Işık Bloğu: Luminance 15 ile parlar.
 * Her saniye 100 blok yarıçapındaki tüm düşman mobları ve oyuncuları yok eder.
 */
public class LightBombBlock extends Block {

    private static final int KILL_RADIUS = 100;

    public LightBombBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        if (!world.isClient()) {
            world.scheduleBlockTick(pos, this, 20);
        }
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // Düşman mobları anında öldür
        world.getEntitiesByClass(
                HostileEntity.class,
                new net.minecraft.util.math.Box(pos).expand(KILL_RADIUS),
                e -> true
        ).forEach(mob -> mob.kill(world));

        // Oyuncuları da etkile (yaratıcı mod harici)
        world.getEntitiesByClass(
                PlayerEntity.class,
                new net.minecraft.util.math.Box(pos).expand(KILL_RADIUS),
                p -> !p.isCreative() && !p.isSpectator()
        ).forEach(player -> player.damage(world, world.getDamageSources().magic(), 1000.0f));

        // 1 saniye sonra tekrar tetikle
        world.scheduleBlockTick(pos, this, 20);
    }
}
