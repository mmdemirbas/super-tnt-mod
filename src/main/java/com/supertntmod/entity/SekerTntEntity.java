package com.supertntmod.entity;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Şeker TNT: Patladığında 15 blok yarıçapındaki katı blokları
 * çikolata ve şekerleme bloklarına dönüştürür.
 * Yakındaki oyunculara hız ve zıplama güçlendirmesi verir (şeker enerjisi!).
 */
public class SekerTntEntity extends TntEntity {
    private static final int RADIUS = 15;
    private boolean done = false;

    // Şekerleme blokları
    private static final Block[] CANDY_BLOCKS = {
            Blocks.BROWN_GLAZED_TERRACOTTA,   // Çikolata
            Blocks.PINK_GLAZED_TERRACOTTA,    // Çilek şekeri
            Blocks.WHITE_GLAZED_TERRACOTTA,   // Beyaz çikolata
            Blocks.MAGENTA_GLAZED_TERRACOTTA, // Sakız
            Blocks.RED_GLAZED_TERRACOTTA,     // Kırmızı şeker
            Blocks.HONEY_BLOCK,               // Bal
            Blocks.HONEYCOMB_BLOCK,           // Bal peteği
            Blocks.BROWN_CONCRETE,            // Koyu çikolata
            Blocks.PINK_CONCRETE,             // Pamuk şeker
            Blocks.WHITE_CONCRETE,            // Marshmallow
            Blocks.COCOA,                     // Kakao
    };

    public SekerTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public SekerTntEntity(World world, double x, double y, double z,
                          @Nullable LivingEntity igniter) {
        super(ModEntities.SEKER_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    @Override
    public void tick() {
        if (!done && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            done = true;
            BlockPos center = this.getBlockPos();
            World world = getEntityWorld();
            this.discard();

            // Şeker patlaması sesi
            world.playSound(null, center.getX(), center.getY(), center.getZ(),
                    SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE, SoundCategory.BLOCKS, 2.0f, 1.5f);
            world.playSound(null, center.getX(), center.getY(), center.getZ(),
                    SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 2.0f, 1.8f);

            // Katı blokları şekerleme bloklarına dönüştür
            for (BlockPos pos : BlockPos.iterateOutwards(center, RADIUS, RADIUS, RADIUS)) {
                if (!pos.isWithinDistance(center, RADIUS)) continue;

                var state = world.getBlockState(pos);

                if (state.isOf(Blocks.AIR) || state.isOf(Blocks.BEDROCK) ||
                    state.isOf(Blocks.WATER) || state.isOf(Blocks.LAVA) ||
                    !state.isSolidBlock(world, pos)) continue;

                // Kakao sadece orman ağaçlarına yerleşir, onu atlayalım
                Block candy = CANDY_BLOCKS[world.random.nextInt(CANDY_BLOCKS.length - 1)];
                world.setBlockState(pos, candy.getDefaultState());
            }

            // Yakındaki oyunculara şeker enerjisi (hız + zıplama)
            world.getEntitiesByClass(LivingEntity.class,
                    new net.minecraft.util.math.Box(center).expand(RADIUS),
                    e -> true
            ).forEach(entity -> {
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 600, 2));
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 600, 2));
            });

            // Renkli şeker partikülleri
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.HEART,
                        center.getX() + 0.5, center.getY() + 3, center.getZ() + 0.5,
                        50, 5.0, 3.0, 5.0, 0.1);
                serverWorld.spawnParticles(ParticleTypes.FIREWORK,
                        center.getX() + 0.5, center.getY() + 5, center.getZ() + 0.5,
                        200, 6.0, 4.0, 6.0, 0.3);
                serverWorld.spawnParticles(ParticleTypes.WAX_ON,
                        center.getX() + 0.5, center.getY() + 2, center.getZ() + 0.5,
                        100, 5.0, 3.0, 5.0, 0.2);
            }

            // Görsel patlama
            world.createExplosion(null, center.getX() + 0.5, center.getY(),
                    center.getZ() + 0.5, 0.0f, false, World.ExplosionSourceType.NONE);

            return;
        }
        if (!done) super.tick();
    }
}
