package com.supertntmod.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Sahte TNT (Pasta Görünümlü):
 * Tıpkı pasta gibi görünür. Oyuncu sağ tıklayarak
 * "yemeye" çalışırsa patlama olur!
 * Bloklara zarar vermez, sadece oyuncuya hasar verir.
 */
public class FakeTntBlock extends Block {

    public FakeTntBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos,
                                  PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) return ActionResult.SUCCESS;

        // Pasta yeme sesi (kandırma)
        world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.BLOCKS, 1.0f, 1.0f);

        // Kısa bir beklemeden sonra patlama! (anında olsun, daha komik)
        // Bloğu kaldır
        world.setBlockState(pos, Blocks.AIR.getDefaultState());

        // Patlama sesi
        world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 1.5f, 1.0f);

        // Oyuncuya hasar (7 kalp)
        if (world instanceof ServerWorld serverWorld) {
            player.damage(serverWorld, world.getDamageSources().explosion(null, null), 14.0f);
        }

        // Görsel patlama (bloklara zarar vermez)
        world.createExplosion(null, pos.getX() + 0.5, pos.getY() + 0.5,
                pos.getZ() + 0.5, 0.0f, false, World.ExplosionSourceType.NONE);

        // Pasta parçaları uçuşsun
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.CLOUD,
                    pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
                    50, 1.5, 1.0, 1.5, 0.1);
            serverWorld.spawnParticles(ParticleTypes.ITEM_SNOWBALL,
                    pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
                    30, 1.0, 0.5, 1.0, 0.2);
        }

        // Trolleme mesajı
        player.sendMessage(Text.literal("§c💥 Bu pasta değildi! BOOM!"), false);

        return ActionResult.SUCCESS;
    }
}
