package com.supertntmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Yağmur TNT: 5 saniyeliğine yağmur başlatır, yakın oyuncuları "üşütür"
 * (kısa zayıflık + açlık).
 */
public class YagmurTntEntity extends TntEntity {
    private boolean done = false;
    private static final int RAIN_TICKS = 100;   // 5 saniye
    private static final int CHILL_TICKS = 200;  // 10 saniye
    private static final int RADIUS = 16;

    public YagmurTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public YagmurTntEntity(World world, double x, double y, double z,
                           @Nullable LivingEntity igniter) {
        super(ModEntities.YAGMUR_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    @Override
    public void tick() {
        if (!done && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            done = true;
            World world = getEntityWorld();
            double x = getX(), y = getY(), z = getZ();
            this.discard();

            world.playSound(null, x, y, z, SoundEvents.WEATHER_RAIN,
                    SoundCategory.WEATHER, 4.0f, 1.0f);
            world.playSound(null, x, y, z, SoundEvents.ENTITY_GENERIC_SPLASH,
                    SoundCategory.BLOCKS, 2.0f, 0.8f);

            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.SPLASH,
                        x, y + 3, z, 200, RADIUS, 5.0, RADIUS, 0.1);
                serverWorld.spawnParticles(ParticleTypes.FALLING_WATER,
                        x, y + 4, z, 200, RADIUS, 5.0, RADIUS, 0.1);
                // Sadece 5 saniyelik yağmur — 100 tick clear, 100 tick yağmur
                serverWorld.setWeather(0, RAIN_TICKS, true, false);
            }

            // Yakındaki oyunculara üşütme
            world.getEntitiesByClass(PlayerEntity.class,
                    new Box(x - RADIUS, y - RADIUS, z - RADIUS,
                            x + RADIUS, y + RADIUS, z + RADIUS),
                    e -> true).forEach(p -> {
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, CHILL_TICKS, 0));
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, CHILL_TICKS, 0));
            });

            world.createExplosion(null, x, y, z, 0.5f, false, World.ExplosionSourceType.NONE);
            return;
        }
        if (!done) super.tick();
    }
}
