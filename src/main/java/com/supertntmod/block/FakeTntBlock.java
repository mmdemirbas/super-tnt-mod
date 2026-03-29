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
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient()) {
            explode(world, pos, player);
        }
        return super.onBreak(world, pos, state, player);
    }

    private void explode(World world, BlockPos pos, PlayerEntity player) {
        // Pasta yeme sesi (kandırma)
        world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.BLOCKS, 1.0f, 1.0f);

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
        player.sendMessage(Text.translatable("message.supertntmod.fake_tnt.boom"), false);
    }
}
