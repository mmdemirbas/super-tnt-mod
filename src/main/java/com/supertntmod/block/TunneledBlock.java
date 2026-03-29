package com.supertntmod.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

/**
 * Tünellenmiş blok: 4×4×4 sub-voxel grid ile kısmen kazılmış blok.
 * TunnelingItem tarafından oluşturulur, BlockEntity ile durumu saklar.
 * Render tamamen BlockEntityRenderer tarafından yapılır.
 */
public class TunneledBlock extends Block implements BlockEntityProvider {
    public static final MapCodec<TunneledBlock> CODEC = createCodec(TunneledBlock::new);

    public TunneledBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TunneledBlockEntity(pos, state);
    }

    // Render BlockEntityRenderer tarafından yapılacak
    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE;
    }

    // Collision: kalan sub-voxel'lere göre
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return getShapeFromEntity(world, pos);
    }

    // Outline: kalan sub-voxel'lere göre
    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return getShapeFromEntity(world, pos);
    }

    // Işık geçirsin
    @Override
    protected boolean isTransparent(BlockState state) {
        return true;
    }

    private VoxelShape getShapeFromEntity(BlockView world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof TunneledBlockEntity entity) {
            return entity.getVoxelShape();
        }
        return VoxelShapes.fullCube();
    }
}
