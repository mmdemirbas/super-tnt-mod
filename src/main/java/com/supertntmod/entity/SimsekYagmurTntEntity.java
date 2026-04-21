package com.supertntmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class SimsekYagmurTntEntity extends TntEntity {
    private boolean done = false;

    public SimsekYagmurTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public SimsekYagmurTntEntity(World world, double x, double y, double z,
                                  @Nullable LivingEntity igniter) {
        super(ModEntities.SIMSEK_YAGMUR_TNT, world);
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

            if (world instanceof ServerWorld serverWorld) {
                serverWorld.setWeather(0, 24000, true, true);
                serverWorld.spawnParticles(ParticleTypes.END_ROD,
                        cx, cy + 2, cz, 200, 8.0, 4.0, 8.0, 0.5);
            }

            world.playSound(null, cx, cy, cz,
                    SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 4.0f, 0.7f);

            for (int i = 0; i < 30; i++) {
                LightningEntity bolt = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
                bolt.setPos(
                        cx + (world.random.nextDouble() - 0.5) * 50,
                        cy,
                        cz + (world.random.nextDouble() - 0.5) * 50);
                world.spawnEntity(bolt);
            }
            return;
        }
        if (!done) super.tick();
    }
}
