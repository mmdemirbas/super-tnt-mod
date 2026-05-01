package com.supertntmod.entity;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Buz TNT: Patladığında 14 blok yarıçapındaki suyu/karı/çimi buza çevirir.
 * Patlatan dışındaki tüm oyuncuları 30 saniye dondurur (slowness IV + freeze ticks).
 * 30 saniye boyunca thunder/snow yağdırır (gerçek 5 saat oyunu kilitlerdi).
 */
public class BuzTntEntity extends TntEntity {
    private static final int RADIUS = 14;
    private static final int FREEZE_TICKS = 600; // 30 saniye
    private static final int WEATHER_TICKS = 600; // 30 saniye
    private static final int MODIFICATIONS_PER_TICK = 600;

    private boolean done = false;
    private boolean processing = false;
    private BlockPos center;
    private int idx = 0;

    public BuzTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public BuzTntEntity(World world, double x, double y, double z,
                        @Nullable LivingEntity igniter) {
        super(ModEntities.BUZ_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    @Override
    public void tick() {
        if (processing && !this.getEntityWorld().isClient()) {
            World world = getEntityWorld();
            int side = RADIUS * 2 + 1;
            int total = side * side * side;
            int modified = 0;
            while (modified < MODIFICATIONS_PER_TICK && idx < total) {
                int lx = (idx % side) - RADIUS;
                int lz = ((idx / side) % side) - RADIUS;
                int ly = (idx / (side * side)) - RADIUS;
                idx++;

                BlockPos pos = center.add(lx, ly, lz);
                if (!pos.isWithinDistance(center, RADIUS)) continue;

                var state = world.getBlockState(pos);
                if (state.isOf(Blocks.AIR) || state.isOf(Blocks.BEDROCK)) continue;

                // Su -> buz, lav -> taş, çim/toprak -> kar
                Block target = null;
                if (state.isOf(Blocks.WATER)) target = Blocks.PACKED_ICE;
                else if (state.isOf(Blocks.LAVA)) target = Blocks.STONE;
                else if (state.isOf(Blocks.GRASS_BLOCK) || state.isOf(Blocks.DIRT)
                        || state.isOf(Blocks.PODZOL)) target = Blocks.SNOW_BLOCK;
                else if (state.isOf(Blocks.SAND) || state.isOf(Blocks.RED_SAND))
                    target = Blocks.SNOW_BLOCK;
                else if (state.isIn(net.minecraft.registry.tag.BlockTags.LEAVES))
                    target = Blocks.SNOW_BLOCK;
                else if (state.isSolidBlock(world, pos)
                        && world.random.nextFloat() < 0.15f) target = Blocks.PACKED_ICE;

                if (target != null) {
                    world.setBlockState(pos, target.getDefaultState());
                    modified++;
                }
            }
            if (idx >= total) {
                processing = false;
                this.discard();
            }
            return;
        }

        if (!done && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            done = true;
            World world = getEntityWorld();
            double x = getX(), y = getY(), z = getZ();
            center = this.getBlockPos();

            UUID owner = (this.getOwner() != null) ? this.getOwner().getUuid() : null;

            world.playSound(null, x, y, z, SoundEvents.BLOCK_GLASS_BREAK,
                    SoundCategory.BLOCKS, 4.0f, 0.5f);
            world.playSound(null, x, y, z, SoundEvents.BLOCK_POWDER_SNOW_PLACE,
                    SoundCategory.BLOCKS, 3.0f, 0.7f);

            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.SNOWFLAKE,
                        x, y + 3, z, 600, RADIUS, 6.0, RADIUS, 0.1);
                serverWorld.spawnParticles(ParticleTypes.WHITE_ASH,
                        x, y + 4, z, 300, RADIUS, 6.0, RADIUS, 0.05);

                // Yağmur/kar yağdır (gece görünmesin diye thunder değil)
                serverWorld.setWeather(0, WEATHER_TICKS, true, false);

                // Patlatan dışındaki tüm oyuncuları dondur
                for (ServerPlayerEntity p : serverWorld.getPlayers()) {
                    if (owner != null && p.getUuid().equals(owner)) continue;
                    p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,
                            FREEZE_TICKS, 6));
                    p.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE,
                            FREEZE_TICKS, 4));
                    p.setFrozenTicks(FREEZE_TICKS);
                }
            }

            world.createExplosion(null, x, y, z, 1.0f, false, World.ExplosionSourceType.NONE);
            processing = true;
            return;
        }
        if (!done) super.tick();
    }
}
