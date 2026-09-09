package com.supertntmod.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * 🟡 Altın Ateş — Altın Flint ile yakılır.
 *
 * Sönmez: zamanlanmış tick'i yok, yağmur ve su etkilemez (replaceable değil).
 * Yayılmaz: komşu blokları tutuşturmaz. Yumrukla kırılarak söndürülür.
 *
 * Yakıldığı bloğu altına çevirme işi ateşin değil, çakmağın işi
 * ({@link com.supertntmod.item.GoldenFlintItem}) — "hangi bloğun üzerine
 * yakıldıysa" bilgisi yalnız orada var.
 */
public class GoldFireBlock extends Block {

    /** Ateşin içinde kalan canlının yanma süresi (saniye). */
    private static final float BURN_SECONDS = 8.0f;

    public GoldFireBlock(Settings settings) {
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

    @Override
    protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity,
                                     EntityCollisionHandler handler, boolean pushable) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        if (entity.isFireImmune()) return;
        entity.setOnFireFor(BURN_SECONDS);
        entity.damage(serverWorld, world.getDamageSources().inFire(), 1.0f);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (random.nextInt(8) == 0) {
            world.playSoundClient(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.BLOCK_FIRE_AMBIENT, SoundCategory.BLOCKS,
                    0.5f, random.nextFloat() * 0.3f + 0.9f, false);
        }
        for (int i = 0; i < 3; i++) {
            world.addParticleClient(ParticleTypes.FLAME,
                    pos.getX() + random.nextDouble(),
                    pos.getY() + random.nextDouble() * 0.5,
                    pos.getZ() + random.nextDouble(),
                    0, 0.02, 0);
        }
        if (random.nextInt(2) == 0) {
            world.addParticleClient(ParticleTypes.END_ROD,
                    pos.getX() + random.nextDouble(),
                    pos.getY() + random.nextDouble() * 0.8,
                    pos.getZ() + random.nextDouble(),
                    0, 0.01, 0);
        }
    }
}
