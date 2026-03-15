package com.supertntmod.entity;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Süper TNT (Mob Dondurma):
 * Bloklara zarar vermez. Tüm düşman mobları 10 dakika dondurur.
 * Moblar buzun içinde hapsolur.
 */
public class MobFreezeTntEntity extends TntEntity {
    private static final int FREEZE_DURATION = 12000; // 10 dakika = 12000 tick
    private boolean done = false;

    public MobFreezeTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public MobFreezeTntEntity(World world, double x, double y, double z,
                              @Nullable LivingEntity igniter) {
        super(ModEntities.MOB_FREEZE_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    @Override
    public void tick() {
        if (!done && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            done = true;
            World world = getEntityWorld();
            this.discard();

            // Görsel efekt (bloklara zarar vermez)
            world.playSound(null, getX(), getY(), getZ(),
                    SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 2.0f, 0.5f);

            if (world instanceof ServerWorld serverWorld) {
                // Tüm yüklü chunk'lardaki düşman mobları dondur
                serverWorld.iterateEntities().forEach(entity -> {
                    if (entity instanceof HostileEntity hostile && hostile.isAlive()) {
                        // Buz bloğuyla kapla
                        BlockPos mobPos = hostile.getBlockPos();

                        // Mobun etrafına buz blokları koy
                        for (int dx = -1; dx <= 1; dx++) {
                            for (int dz = -1; dz <= 1; dz++) {
                                for (int dy = 0; dy <= 2; dy++) {
                                    BlockPos icePos = mobPos.add(dx, dy, dz);
                                    if (world.getBlockState(icePos).isOf(Blocks.AIR)) {
                                        world.setBlockState(icePos, Blocks.ICE.getDefaultState());
                                    }
                                }
                            }
                        }

                        // Yavaşlama + hareketsizlik efekti
                        hostile.addStatusEffect(new StatusEffectInstance(
                                StatusEffects.SLOWNESS, FREEZE_DURATION, 127)); // Max yavaşlama
                        hostile.addStatusEffect(new StatusEffectInstance(
                                StatusEffects.RESISTANCE, FREEZE_DURATION, 4)); // Hasar almaz
                        hostile.addStatusEffect(new StatusEffectInstance(
                                StatusEffects.WEAKNESS, FREEZE_DURATION, 127)); // Saldıramaz
                        hostile.setFrozenTicks(FREEZE_DURATION); // Minecraft dondurma mekaniği
                    }
                });
            }
            return;
        }
        if (!done) super.tick();
    }
}
