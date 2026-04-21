package com.supertntmod.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Hayalet Blok: içinden geçilebilir (no collision).
 * Komşu bloklara bakarak en yaygın bloğu tespit eder;
 * şu an için görsel olarak cam gibi render edilir.
 */
public class GhostBlock extends Block {

    public GhostBlock(Settings settings) {
        super(settings);
    }

    /** Geçilebilir — çarpışma şekli yok. */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.fullCube();
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        // Hafif bir işaret partiküllü görünüm
        if (random.nextInt(4) == 0) {
            world.addParticleClient(ParticleTypes.SMOKE,
                    pos.getX() + random.nextDouble(),
                    pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble(),
                    0, 0.01, 0);
        }
    }
}
