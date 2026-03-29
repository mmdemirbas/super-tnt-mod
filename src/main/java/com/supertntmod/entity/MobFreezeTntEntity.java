package com.supertntmod.entity;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.particle.ParticleTypes;
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
    private static final int RADIUS = 30;
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
            world.playSound(null, getX(), getY(), getZ(),
                    SoundEvents.ENTITY_SNOW_GOLEM_AMBIENT, SoundCategory.BLOCKS, 1.5f, 0.8f);

            // Buz partikülleri
            if (world instanceof ServerWorld sw) {
                sw.spawnParticles(ParticleTypes.SNOWFLAKE,
                        getX(), getY() + 2, getZ(), 500, 10.0, 5.0, 10.0, 0.1);
                sw.spawnParticles(ParticleTypes.WHITE_ASH,
                        getX(), getY() + 3, getZ(), 200, 8.0, 6.0, 8.0, 0.02);
            }

            if (world instanceof ServerWorld serverWorld) {
                // Yarıçap içindeki düşman mobları dondur
                BlockPos center = BlockPos.ofFloored(getX(), getY(), getZ());
                serverWorld.getEntitiesByClass(HostileEntity.class,
                        new net.minecraft.util.math.Box(center).expand(RADIUS),
                        HostileEntity::isAlive
                ).forEach(hostile -> {
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
                });
            }
            return;
        }
        if (!done) super.tick();
    }
}
