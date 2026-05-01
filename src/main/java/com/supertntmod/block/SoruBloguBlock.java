package com.supertntmod.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * ❓ Soru Bloğu
 * Çim blok görünümlü; kırıldığında üç olasılıktan biri olur:
 * - savaş eşyası dropu (kılıç/balta/yay/ok),
 * - lav (yerine sıvı lav konur),
 * - saldırgan mob (zombie/skeleton/creeper/husk/spider/witch).
 *
 * Lav dropu, oyuncuyu cezalandırmak içindir; varsayılan ile uyumlu.
 */
public class SoruBloguBlock extends Block {

    private static final EntityType<?>[] HOSTILE_POOL = new EntityType<?>[] {
            EntityType.ZOMBIE,
            EntityType.SKELETON,
            EntityType.CREEPER,
            EntityType.HUSK,
            EntityType.SPIDER,
            EntityType.WITCH,
            EntityType.PILLAGER,
            EntityType.STRAY
    };

    public SoruBloguBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient() && world instanceof ServerWorld serverWorld) {
            triggerSurprise(serverWorld, pos);
        }
        return super.onBreak(world, pos, state, player);
    }

    /** Drop-süreci, oyuncu hayatta kalsın diye blok kaldırıldıktan SONRA çalışır. */
    private static void triggerSurprise(ServerWorld world, BlockPos pos) {
        Random random = world.random;
        int roll = random.nextInt(3);

        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;

        world.playSound(null, cx, cy, cz,
                SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(),
                SoundCategory.BLOCKS, 1.0f, 0.6f + random.nextFloat() * 0.6f);

        switch (roll) {
            case 0 -> dropWarItem(world, pos, random);
            case 1 -> placeLava(world, pos);
            default -> spawnHostile(world, pos, random);
        }
    }

    private static void dropWarItem(ServerWorld world, BlockPos pos, Random random) {
        ItemStack stack = switch (random.nextInt(7)) {
            case 0 -> new ItemStack(Items.NETHERITE_SWORD);
            case 1 -> new ItemStack(Items.DIAMOND_AXE);
            case 2 -> new ItemStack(Items.IRON_SWORD);
            case 3 -> new ItemStack(Items.BOW);
            case 4 -> new ItemStack(Items.ARROW, 32);
            case 5 -> new ItemStack(Items.SHIELD);
            default -> new ItemStack(Items.CROSSBOW);
        };
        ItemEntity item = new ItemEntity(world,
                pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, stack);
        item.setVelocity(0, 0.3, 0);
        world.spawnEntity(item);
        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5,
                15, 0.3, 0.3, 0.3, 0.05);
    }

    private static void placeLava(ServerWorld world, BlockPos pos) {
        // Bloğun kaldırıldığı yere lav koy. Hâlâ oyuncu komşulukta olduğu için anlamlı.
        world.setBlockState(pos, Blocks.LAVA.getDefaultState(), 3);
        world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SoundEvents.BLOCK_LAVA_POP, SoundCategory.BLOCKS, 1.0f, 0.8f);
        world.spawnParticles(ParticleTypes.LAVA,
                pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5,
                10, 0.3, 0.3, 0.3, 0.0);
    }

    private static void spawnHostile(ServerWorld world, BlockPos pos, Random random) {
        EntityType<?> type = HOSTILE_POOL[random.nextInt(HOSTILE_POOL.length)];
        var entity = type.create(world, SpawnReason.MOB_SUMMONED);
        if (entity == null) return;

        double sx = pos.getX() + 0.5;
        double sy = pos.getY() + 0.05;
        double sz = pos.getZ() + 0.5;
        entity.setPosition(sx, sy, sz);
        entity.setYaw(random.nextFloat() * 360.0f);

        // Saldırganlık: HostileEntity ve PathAwareEntity yardımcılarını kullan
        if (entity instanceof PathAwareEntity pae) {
            pae.setPersistent();
        }
        // Hostile'ler zaten saldırgandır; spec gereği "Çıkan bütün moblar saldırgandır"
        // şartını sağlamak için sadece hostile pool kullanılıyor.

        world.spawnEntity(entity);

        // Görsel ipucu
        world.playSound(null, sx, sy, sz,
                SoundEvents.ENTITY_ZOMBIE_INFECT, SoundCategory.HOSTILE, 1.0f, 1.0f);
        world.spawnParticles(ParticleTypes.LARGE_SMOKE,
                sx, pos.getY() + 1.0, sz, 25, 0.4, 0.5, 0.4, 0.05);

        // Eğer saldırgansa hedef olarak en yakın oyuncuyu seç
        if (entity instanceof HostileEntity he) {
            PlayerEntity nearest = world.getClosestPlayer(sx, sy, sz, 32.0, p -> !p.isSpectator());
            if (nearest instanceof LivingEntity living) {
                he.setTarget(living);
            }
        }
    }
}
