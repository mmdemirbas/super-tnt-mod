package com.supertntmod.block;

import com.supertntmod.SuperTntMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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
    // Uyarı alan oyuncular (ikinci denemede patlar)
    private static final Map<UUID, BlockPos> WARNED_PLAYERS = new HashMap<>();

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
            // Başkası: ilk seferde uyar, ikinci seferde patla!
            BlockPos warnedPos = WARNED_PLAYERS.get(player.getUuid());
            if (warnedPos == null || !warnedPos.equals(basePos)) {
                // İlk deneme: uyarı ver
                WARNED_PLAYERS.put(player.getUuid(), basePos);
                world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.BLOCKS, 1.0f, 0.5f);
                player.sendMessage(Text.literal("§c⚠ DİKKAT: Bu kapı sana ait değil! Tekrar denerseniz patlayacak!"), false);
                return ActionResult.SUCCESS;
            }
            // İkinci deneme: patla!
            WARNED_PLAYERS.remove(player.getUuid());
            if (world instanceof ServerWorld serverWorld) {
                player.damage(serverWorld, world.getDamageSources().explosion(null, null), 14.0f);
            }
            world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 1.0f, 1.0f);
            world.createExplosion(null, pos.getX() + 0.5, pos.getY() + 0.5,
                    pos.getZ() + 0.5, 0.0f, false, World.ExplosionSourceType.NONE);
            player.sendMessage(Text.literal("§c💥 Bu kapı sana ait değil! BOOM!"), false);
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
