package com.supertntmod.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * 🦠 Zehir Toprağı
 * Sadece Koku Bombası tarafından yerleştirilir; kraftlanmaz, envanterde olmaz.
 * Üzerine basan veya çarpışan canlıyı anında öldürür.
 * 60 saniye sonra zamanlanmış tick ile kendisini yok eder.
 */
public class ZehirToprakBlock extends Block {

    public static final int DECAY_TICKS = 1200; // 60 saniye

    public ZehirToprakBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos,
                             BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        if (!world.isClient()) {
            world.scheduleBlockTick(pos, this, DECAY_TICKS);
        }
    }

    @Override
    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // Süre dolunca yok ol
        world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
        world.spawnParticles(ParticleTypes.SMOKE,
                pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5,
                8, 0.3, 0.1, 0.3, 0.02);
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        super.onSteppedOn(world, pos, state, entity);
        killOnContact(world, pos, entity);
    }

    @Override
    protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity,
                                     net.minecraft.entity.EntityCollisionHandler handler, boolean pushable) {
        super.onEntityCollision(state, world, pos, entity, handler, pushable);
        killOnContact(world, pos, entity);
    }

    private static void killOnContact(World world, BlockPos pos, Entity entity) {
        if (world.isClient()) return;
        if (!(entity instanceof LivingEntity living)) return;
        if (!(world instanceof ServerWorld sw)) return;
        if (!living.isAlive()) return;

        // Anında öldür: zırh/etki bağışıklığını atlatmak için kill() kullan
        living.kill(sw);

        sw.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SoundEvents.ENTITY_PLAYER_DEATH, SoundCategory.HOSTILE, 0.6f, 1.4f);
        sw.spawnParticles(ParticleTypes.SCULK_SOUL,
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                12, 0.4, 0.4, 0.4, 0.05);
    }
}
