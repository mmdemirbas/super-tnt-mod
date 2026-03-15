package com.supertntmod.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.TntBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.block.WireOrientation;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.Nullable;

/**
 * Tüm özel TNT bloklarının temel sınıfı.
 * Ateşleme mantığını merkezileştirir.
 */
public abstract class CustomTntBlock extends TntBlock {

    public CustomTntBlock(Settings settings) {
        super(settings);
    }

    /** Alt sınıf kendi TNT entity'sini spawn eder */
    protected abstract void spawnEntity(World world, double x, double y, double z,
                                        @Nullable LivingEntity igniter);

    public void prime(World world, BlockPos pos, @Nullable LivingEntity igniter) {
        if (!world.isClient()) {
            double x = pos.getX() + 0.5, y = pos.getY(), z = pos.getZ() + 0.5;
            spawnEntity(world, x, y, z, igniter);
            world.playSound(null, x, y, z, SoundEvents.ENTITY_TNT_PRIMED,
                    SoundCategory.BLOCKS, 1.0f, 1.0f);
        }
    }

    // Çakmak taşı / ateş topu ile ateşleme
    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world,
                                         BlockPos pos, PlayerEntity player, Hand hand,
                                         BlockHitResult hit) {
        if (stack.isOf(Items.FLINT_AND_STEEL) || stack.isOf(Items.FIRE_CHARGE)) {
            prime(world, pos, player);
            world.setBlockState(pos, Blocks.AIR.getDefaultState());
            if (!world.isClient()) {
                if (stack.isOf(Items.FLINT_AND_STEEL)) {
                    stack.damage(1, player,
                            hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                } else if (!player.isCreative()) {
                    stack.decrement(1);
                }
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
    }

    // Redstone sinyali ile ateşleme
    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos,
                               Block sourceBlock, @Nullable WireOrientation wireOrientation, boolean notify) {
        if (world.isReceivingRedstonePower(pos)) {
            prime(world, pos, null);
            world.setBlockState(pos, Blocks.AIR.getDefaultState());
        }
    }

    // Patlama zinciri ile ateşleme
    @Override
    public void onDestroyedByExplosion(ServerWorld world, BlockPos pos, Explosion explosion) {
        prime(world, pos, null);
    }
}
