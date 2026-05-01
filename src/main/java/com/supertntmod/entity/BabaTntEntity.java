package com.supertntmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * 👨 Baba TNT — patladığında etrafa "Baba" isimli köylüler saçar. Pembe temalı.
 */
public class BabaTntEntity extends TntEntity {
    private static final int VILLAGER_COUNT = 12;
    private static final double SPREAD = 6.0;
    private boolean done = false;

    public BabaTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public BabaTntEntity(World world, double x, double y, double z, @Nullable LivingEntity igniter) {
        super(ModEntities.BABA_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    @Override
    public void tick() {
        if (!done && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            done = true;
            BlockPos center = this.getBlockPos();
            double cx = center.getX() + 0.5, cy = center.getY(), cz = center.getZ() + 0.5;
            World world = getEntityWorld();
            this.discard();

            world.playSound(null, cx, cy, cz,
                    SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 1.5f, 0.9f);

            if (world instanceof ServerWorld sw) {
                for (int i = 0; i < VILLAGER_COUNT; i++) {
                    double ox = (sw.random.nextDouble() - 0.5) * SPREAD;
                    double oz = (sw.random.nextDouble() - 0.5) * SPREAD;
                    VillagerEntity villager = EntityType.VILLAGER.create(sw, SpawnReason.MOB_SUMMONED);
                    if (villager == null) continue;
                    villager.setPosition(cx + ox, cy + 1, cz + oz);
                    villager.setCustomName(Text.literal("Baba"));
                    villager.setCustomNameVisible(true);
                    sw.spawnEntity(villager);
                }
                sw.spawnParticles(ParticleTypes.HEART, cx, cy + 2, cz, 30, 3.0, 2.0, 3.0, 0.1);
            }

            world.createExplosion(null, cx, cy, cz, 0.0f, false, World.ExplosionSourceType.NONE);
            return;
        }
        if (!done) super.tick();
    }
}
