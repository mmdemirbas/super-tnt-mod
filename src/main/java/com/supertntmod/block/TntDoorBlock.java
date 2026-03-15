package com.supertntmod.block;

import com.supertntmod.SuperTntMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockSetType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * TNT Kapı: Sahibi güvenle geçebilir, başkaları patlar.
 * Etraftaki bloklara zarar vermez.
 */
public class TntDoorBlock extends DoorBlock {
    // Kapının sahibini takip eden map (pozisyon -> oyuncu UUID)
    private static final Map<BlockPos, UUID> OWNERS = new HashMap<>();

    public TntDoorBlock(Settings settings) {
        super(BlockSetType.IRON, settings);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos,
                                  PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) return ActionResult.SUCCESS;

        // Alt blok pozisyonunu bul (sahiplik takibi için)
        BlockPos basePos = state.get(HALF) == DoubleBlockHalf.UPPER ? pos.down() : pos;

        // İlk kez kullanıldığında sahibini kaydet
        if (!OWNERS.containsKey(basePos)) {
            OWNERS.put(basePos, player.getUuid());
            player.sendMessage(Text.literal("Bu TNT kapısının sahibi oldunuz!"), true);
        }

        UUID ownerUuid = OWNERS.get(basePos);

        if (player.getUuid().equals(ownerUuid)) {
            // Sahibi: kapıyı aç/kapa
            boolean isOpen = state.get(OPEN);
            world.setBlockState(pos, state.with(OPEN, !isOpen), Block.NOTIFY_LISTENERS | Block.NOTIFY_NEIGHBORS);
            // Diğer yarıyı da güncelle
            BlockPos otherHalf = state.get(HALF) == DoubleBlockHalf.LOWER ? pos.up() : pos.down();
            BlockState otherState = world.getBlockState(otherHalf);
            if (otherState.getBlock() == this) {
                world.setBlockState(otherHalf, otherState.with(OPEN, !isOpen), Block.NOTIFY_LISTENERS | Block.NOTIFY_NEIGHBORS);
            }
            world.playSound(null, pos, isOpen ? SoundEvents.BLOCK_IRON_DOOR_CLOSE : SoundEvents.BLOCK_IRON_DOOR_OPEN,
                    SoundCategory.BLOCKS, 1.0f, 1.0f);
            return ActionResult.SUCCESS;
        } else {
            // Başkası: patla! (sadece oyuncuya zarar, bloklara değil)
            if (world instanceof ServerWorld serverWorld) {
                player.damage(serverWorld, world.getDamageSources().explosion(null, null), 20.0f);
            }
            world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 1.0f, 1.0f);
            // Görsel patlama efekti (bloklara zarar vermez)
            world.createExplosion(null, pos.getX() + 0.5, pos.getY() + 0.5,
                    pos.getZ() + 0.5, 0.0f, false, World.ExplosionSourceType.NONE);
            player.sendMessage(Text.literal("Bu kapı sana ait değil!"), true);
            return ActionResult.SUCCESS;
        }
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        BlockPos basePos = state.get(HALF) == DoubleBlockHalf.UPPER ? pos.down() : pos;
        OWNERS.remove(basePos);
        return super.onBreak(world, pos, state, player);
    }
}
