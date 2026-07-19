package com.supertntmod.entity;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class OlumculSuTntEntity extends TntEntity {
    private static final int RADIUS = 15;
    /** Yerleştirilen suyun ömrü (tick) — 30 saniye. */
    private static final int WATER_LIFETIME = 600;
    private boolean done = false;

    // Kademeli isleme durumu
    private boolean processing = false;
    private BlockPos center;
    private int idx = 0;
    private static final int PLACEMENTS_PER_TICK = 700;

    public OlumculSuTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public OlumculSuTntEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        super(ModEntities.OLUMCUL_SU_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    @Override
    public void tick() {
        // Kademeli su yerleştirme. Yarıçap 15'lik küre ~14.000 blok eder;
        // hepsini tek tick'te koymak (üstelik her biri sıvı akış güncellemesi
        // tetikleyerek) sunucuyu donduruyordu. BedrockTnt'deki desen uygulandı.
        if (processing && !this.getEntityWorld().isClient()) {
            World world = getEntityWorld();
            int side = RADIUS * 2 + 1;
            int total = side * side * side;
            int placed = 0;

            while (idx < total && placed < PLACEMENTS_PER_TICK) {
                int lz = idx % side - RADIUS;
                int lx = (idx / side) % side - RADIUS;
                int ly = (idx / (side * side)) - RADIUS;
                idx++;

                BlockPos pos = center.add(lx, ly, lz);
                if (!pos.isWithinDistance(center, RADIUS)) continue;
                if (!world.getBlockState(pos).isAir()) continue;

                world.setBlockState(pos, Blocks.WATER.getDefaultState());
                // Her su kaynağı 30 sn sonra temizlenmek üzere kaydedilir.
                // Kaydedilmezse düz arazide yayılıp kalıcı dünya seli olur —
                // WaterTnt'de düzeltilmiş, burada gözden kaçmıştı.
                if (world instanceof ServerWorld sw) {
                    WaterTntEntity.scheduleRemoval(sw, pos, WATER_LIFETIME);
                }
                placed++;
            }

            if (idx >= total) {
                processing = false;
                this.discard();
            }
            return;
        }

        if (!done && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            done = true;
            center = this.getBlockPos();
            double cx = center.getX() + 0.5, cy = center.getY(), cz = center.getZ() + 0.5;
            World world = getEntityWorld();

            world.playSound(null, cx, cy, cz,
                    SoundEvents.BLOCK_WATER_AMBIENT, SoundCategory.BLOCKS, 3.0f, 0.8f);

            idx = 0;
            processing = true;

            if (world instanceof ServerWorld sw2) {
                DamageSource src = world.getDamageSources().drown();
                world.getEntitiesByClass(LivingEntity.class,
                        new net.minecraft.util.math.Box(center).expand(RADIUS),
                        e -> true
                ).forEach(entity -> entity.damage(sw2, src, 10.0f));
            }

            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.SPLASH,
                        cx, cy + 1, cz, 300, 6.0, 3.0, 6.0, 0.3);
                serverWorld.spawnParticles(ParticleTypes.BUBBLE,
                        cx, cy + 1, cz, 100, 5.0, 2.0, 5.0, 0.2);
            }

            world.createExplosion(null, cx, cy, cz, 0.0f, false, World.ExplosionSourceType.NONE);
            return;
        }
        if (!done) super.tick();
    }

    // Kaydet/yukle: kademeli isleme durumu kalici olmali.
    @Override
    public void readData(ReadView reader) {
        super.readData(reader);
        done = reader.getBoolean("done", false);
        processing = reader.getBoolean("processing", false);
        idx = reader.getInt("idx", 0);
        int cX = reader.getInt("centerX", Integer.MIN_VALUE);
        if (cX != Integer.MIN_VALUE) {
            center = new BlockPos(cX, reader.getInt("centerY", 0), reader.getInt("centerZ", 0));
        }
    }

    @Override
    public void writeData(WriteView writer) {
        super.writeData(writer);
        writer.putBoolean("done", done);
        writer.putBoolean("processing", processing);
        writer.putInt("idx", idx);
        if (center != null) {
            writer.putInt("centerX", center.getX());
            writer.putInt("centerY", center.getY());
            writer.putInt("centerZ", center.getZ());
        }
    }
}
