package com.supertntmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GunesTntEntity extends TntEntity {
    private boolean exploded = false;
    private int ticksAfterExplode = -1;
    private final List<UUID> crystalUuids = new ArrayList<>();

    public GunesTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public GunesTntEntity(World world, double x, double y, double z,
                          @Nullable LivingEntity igniter) {
        super(ModEntities.GUNES_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    @Override
    public void tick() {
        if (!exploded && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            exploded = true;
            ticksAfterExplode = 0;
            BlockPos center = this.getBlockPos();
            double cx = center.getX() + 0.5, cy = center.getY(), cz = center.getZ() + 0.5;
            World world = getEntityWorld();

            world.playSound(null, cx, cy, cz,
                    SoundEvents.AMBIENT_BASALT_DELTAS_LOOP, SoundCategory.BLOCKS, 2.0f, 1.5f);

            for (int i = 0; i < 15; i++) {
                double angle = (2 * Math.PI / 15) * i;
                double ex = cx + Math.cos(angle) * 10;
                double ez = cz + Math.sin(angle) * 10;
                EndCrystalEntity crystal = new EndCrystalEntity(EntityType.END_CRYSTAL, world);
                crystal.setPos(ex, cy + 1, ez);
                crystal.setShowBottom(false);
                world.spawnEntity(crystal);
                crystalUuids.add(crystal.getUuid());
            }

            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.GLOW_SQUID_INK,
                        cx, cy + 2, cz, 100, 5.0, 3.0, 5.0, 0.2);
                serverWorld.spawnParticles(ParticleTypes.END_ROD,
                        cx, cy + 3, cz, 150, 6.0, 4.0, 6.0, 0.3);
            }
            return;
        }

        if (exploded && ticksAfterExplode >= 0 && !this.getEntityWorld().isClient()) {
            ticksAfterExplode++;
            if (ticksAfterExplode >= 100) {
                ticksAfterExplode = -1;
                World world = getEntityWorld();
                boolean first = true;
                for (UUID uuid : crystalUuids) {
                    if (first) {
                        first = false;
                        continue;
                    }
                    net.minecraft.entity.Entity e = ((ServerWorld) world).getEntity(uuid);
                    if (e != null) e.discard();
                }
                crystalUuids.clear();
                this.discard();
            }
            return;
        }

        if (!exploded) super.tick();
    }
}
