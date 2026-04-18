package com.supertntmod.block;

import com.supertntmod.entity.HerobrineEntity;
import com.supertntmod.entity.ModEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class HerobrineSpawnerBlock extends Block {

    public HerobrineSpawnerBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state,
                         LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (world.isClient()) return;
        if (!(world instanceof ServerWorld sw)) return;

        world.removeBlock(pos, false);

        HerobrineEntity herobrine = new HerobrineEntity(ModEntities.HEROBRINE, world);
        herobrine.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        world.spawnEntity(herobrine);

        world.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 2.0f, 0.5f);

        sw.spawnParticles(ParticleTypes.LARGE_SMOKE,
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                60, 1.0, 1.5, 1.0, 0.05);
        sw.spawnParticles(ParticleTypes.PORTAL,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                120, 1.0, 1.0, 1.0, 0.5);
        sw.spawnParticles(ParticleTypes.SOUL,
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                20, 0.5, 0.5, 0.5, 0.1);
    }
}
