package com.supertntmod.entity;

import com.supertntmod.item.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * 💵 200 TL TNT — patladığında etrafa 200 TL banknotları saçar. Pembe temalı.
 */
public class IkiYuzTlTntEntity extends TntEntity {
    private static final int NOTE_COUNT = 100;
    private static final double SPREAD = 8.0;
    private boolean done = false;

    public IkiYuzTlTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public IkiYuzTlTntEntity(World world, double x, double y, double z, @Nullable LivingEntity igniter) {
        super(ModEntities.IKI_YUZ_TL_TNT, world);
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
                    SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 1.5f, 1.0f);

            for (int i = 0; i < NOTE_COUNT; i++) {
                double ox = (world.random.nextDouble() - 0.5) * SPREAD;
                double oy = world.random.nextDouble() * 2.5;
                double oz = (world.random.nextDouble() - 0.5) * SPREAD;
                ItemEntity note = new ItemEntity(world, cx + ox, cy + 1 + oy, cz + oz,
                        new ItemStack(ModItems.IKI_YUZ_TL));
                note.setVelocity(
                        (world.random.nextDouble() - 0.5) * 0.5,
                        world.random.nextDouble() * 0.5 + 0.2,
                        (world.random.nextDouble() - 0.5) * 0.5);
                world.spawnEntity(note);
            }

            if (world instanceof ServerWorld sw) {
                sw.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                        cx, cy + 2, cz, 80, 4.0, 3.0, 4.0, 0.3);
            }

            world.createExplosion(null, cx, cy, cz, 0.0f, false, World.ExplosionSourceType.NONE);
            return;
        }
        if (!done) super.tick();
    }
}
