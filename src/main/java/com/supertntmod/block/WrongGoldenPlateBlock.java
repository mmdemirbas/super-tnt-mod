package com.supertntmod.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Yanlış Altın Plaka: üstüne basan canlıyı anında öldürür.
 * Kırılamaz (hardness -1). Altın baskı plakası görünümü.
 */
public class WrongGoldenPlateBlock extends Block {
    private static final VoxelShape SHAPE =
            Block.createCuboidShape(1.0, 0.0, 1.0, 15.0, 0.5, 15.0);

    public WrongGoldenPlateBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity,
                                     EntityCollisionHandler handler, boolean pushable) {
        if (world.isClient() || !(entity instanceof LivingEntity living)) return;
        if (!(world instanceof ServerWorld serverWorld)) return;

        serverWorld.spawnParticles(ParticleTypes.LARGE_SMOKE,
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                20, 0.5, 0.5, 0.5, 0.1);
        world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 1.0f, 1.5f);

        living.damage(serverWorld, world.getDamageSources().genericKill(), Float.MAX_VALUE);
    }
}
