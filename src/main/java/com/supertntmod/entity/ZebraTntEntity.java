package com.supertntmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Zebra TNT: patladığında etrafa boş kağıt saçar ve at sesi çıkarır
 * (zebra için en yakın vanilla ses). Siyah-beyaz çizgili tema.
 */
public class ZebraTntEntity extends TntEntity {
    private static final int PAPER_COUNT = 40;
    private static final double SPREAD = 7.0;
    private boolean done = false;

    public ZebraTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public ZebraTntEntity(World world, double x, double y, double z,
                          @Nullable LivingEntity igniter) {
        super(ModEntities.ZEBRA_TNT, world);
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

            // Zebra sesi (vanilla'da zebra yok — atın gulleme/kişneme sesleri en yakın)
            world.playSound(null, cx, cy, cz,
                    SoundEvents.ENTITY_HORSE_ANGRY, SoundCategory.BLOCKS, 3.0f, 0.7f);
            world.playSound(null, cx, cy, cz,
                    SoundEvents.ENTITY_DONKEY_AMBIENT, SoundCategory.BLOCKS, 2.5f, 1.0f);

            for (int i = 0; i < PAPER_COUNT; i++) {
                double ox = (world.random.nextDouble() - 0.5) * SPREAD;
                double oy = world.random.nextDouble() * 2.0;
                double oz = (world.random.nextDouble() - 0.5) * SPREAD;
                ItemEntity paper = new ItemEntity(world, cx + ox, cy + 1 + oy, cz + oz,
                        new ItemStack(Items.PAPER));
                paper.setVelocity(
                        (world.random.nextDouble() - 0.5) * 0.4,
                        world.random.nextDouble() * 0.4 + 0.2,
                        (world.random.nextDouble() - 0.5) * 0.4);
                world.spawnEntity(paper);
            }

            if (world instanceof ServerWorld sw) {
                sw.spawnParticles(ParticleTypes.WHITE_ASH,
                        cx, cy + 2, cz, 100, 4.0, 2.0, 4.0, 0.1);
                sw.spawnParticles(ParticleTypes.SMOKE,
                        cx, cy + 2, cz, 80, 4.0, 2.0, 4.0, 0.05);
            }

            world.createExplosion(null, cx, cy, cz, 0.0f, false, World.ExplosionSourceType.NONE);
            return;
        }
        if (!done) super.tick();
    }
}
