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
 * Hayalet Blok: Cam gibi görünen, içinden geçilebilen blok.
 * Hiçbir varlık ile çarpışmaz; mobların yol bulması üzerinde engel teşkil etmez.
 */
public class GhostBlock extends Block {

    public GhostBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.fullCube();
    }

    /** Düşen blok ya da minecart gibi taşıyıcılar tarafından desteklenmemeli. */
    @Override
    protected VoxelShape getCameraCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    /** Boğulmaya neden olmasın. */
    @Override
    protected VoxelShape getCullingShape(BlockState state) {
        return VoxelShapes.empty();
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        // Hayalet titreşimi: rastgele konumda hafif duman + kıvılcım
        if (random.nextInt(6) == 0) {
            world.addParticleClient(ParticleTypes.SOUL,
                    pos.getX() + random.nextDouble(),
                    pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble(),
                    (random.nextDouble() - 0.5) * 0.02,
                    0.01,
                    (random.nextDouble() - 0.5) * 0.02);
        }
        if (random.nextInt(20) == 0) {
            world.addParticleClient(ParticleTypes.END_ROD,
                    pos.getX() + 0.5,
                    pos.getY() + random.nextDouble(),
                    pos.getZ() + 0.5,
                    0, 0, 0);
        }
    }

}
