package com.supertntmod.entity;

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
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Uyku TNT: Patladığında patlatan dışındaki tüm oyuncuları "uyutur"
 * (yavaşlama + körlük + bulantı) — 30 saniye boyunca.
 * Not: Minecraft gerçek "uyku" debuff'ı yok — kombinasyon ile simüle ediyoruz.
 * 5 saatlik gerçek uyku oyunu bozardı.
 */
public class UykuTntEntity extends TntEntity {
    private boolean done = false;
    private static final int SLEEP_TICKS = 600; // 30 saniye

    public UykuTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public UykuTntEntity(World world, double x, double y, double z,
                         @Nullable LivingEntity igniter) {
        super(ModEntities.UYKU_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    @Override
    public void tick() {
        if (!done && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            done = true;
            World world = getEntityWorld();
            double x = getX(), y = getY(), z = getZ();

            UUID owner = (this.getOwner() != null) ? this.getOwner().getUuid() : null;
            this.discard();

            world.playSound(null, x, y, z, SoundEvents.ENTITY_PHANTOM_AMBIENT,
                    SoundCategory.BLOCKS, 3.0f, 0.7f);
            world.playSound(null, x, y, z, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                    SoundCategory.BLOCKS, 2.0f, 0.5f);

            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.END_ROD,
                        x, y + 2, z, 200, 12.0, 5.0, 12.0, 0.05);
                serverWorld.spawnParticles(ParticleTypes.PORTAL,
                        x, y + 1, z, 150, 10.0, 4.0, 10.0, 0.1);

                for (ServerPlayerEntity player : serverWorld.getPlayers()) {
                    if (owner != null && player.getUuid().equals(owner)) continue;
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,
                            SLEEP_TICKS, 4));
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS,
                            SLEEP_TICKS, 0));
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA,
                            SLEEP_TICKS, 0));
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE,
                            SLEEP_TICKS, 4));
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS,
                            SLEEP_TICKS, 4));
                }
            }

            world.createExplosion(null, x, y, z, 0.5f, false, World.ExplosionSourceType.NONE);
            return;
        }
        if (!done) super.tick();
    }
}
