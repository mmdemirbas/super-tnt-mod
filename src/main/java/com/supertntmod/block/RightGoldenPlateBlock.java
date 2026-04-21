package com.supertntmod.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/**
 * Doğru Altın Plaka: üstüne basınca hiçbir şey yapmaz.
 * Yanlış Altın Plaka ile aynı görünüm, kırılamaz — tuzağın "güvenli" versiyonu.
 */
public class RightGoldenPlateBlock extends Block {
    private static final VoxelShape SHAPE =
            Block.createCuboidShape(1.0, 0.0, 1.0, 15.0, 0.5, 15.0);

    public RightGoldenPlateBlock(Settings settings) {
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
}
