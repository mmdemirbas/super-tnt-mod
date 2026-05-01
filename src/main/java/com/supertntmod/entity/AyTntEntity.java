package com.supertntmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Ay TNT: Gündüzse güneşi aya çevirir — gece olur.
 * Gece ise sadece görsel patlama yapar.
 */
public class AyTntEntity extends TntEntity {
    private boolean done = false;

    public AyTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public AyTntEntity(World world, double x, double y, double z,
                       @Nullable LivingEntity igniter) {
        super(ModEntities.AY_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    @Override
    public void tick() {
        if (!done && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            done = true;
            World world = getEntityWorld();
            BlockPos center = this.getBlockPos();
            double cx = center.getX() + 0.5, cy = center.getY(), cz = center.getZ() + 0.5;
            this.discard();

            if (world instanceof ServerWorld serverWorld) {
                long t = serverWorld.getTimeOfDay();
                long tod = t % 24000L;
                if (tod < 12300L) {
                    // Gündüz — geceye çevir (18000 = gece yarısı)
                    serverWorld.setTimeOfDay(t + (18000L - tod + 24000L) % 24000L);
                }
                serverWorld.spawnParticles(ParticleTypes.END_ROD,
                        cx, cy + 3, cz, 200, 6.0, 4.0, 6.0, 0.1);
                serverWorld.spawnParticles(ParticleTypes.WHITE_ASH,
                        cx, cy + 5, cz, 150, 8.0, 4.0, 8.0, 0.02);
            }

            world.playSound(null, cx, cy, cz,
                    SoundEvents.ENTITY_ENDERMAN_AMBIENT, SoundCategory.BLOCKS, 3.0f, 0.7f);
            world.playSound(null, cx, cy, cz,
                    SoundEvents.ENTITY_BAT_AMBIENT, SoundCategory.BLOCKS, 2.0f, 0.5f);

            world.createExplosion(null, cx, cy, cz, 1.0f, false, World.ExplosionSourceType.NONE);
            return;
        }
        if (!done) super.tick();
    }
}
