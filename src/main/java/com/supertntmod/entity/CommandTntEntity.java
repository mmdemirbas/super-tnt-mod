package com.supertntmod.entity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Komut Bloğu TNT: Belirli bir blok türünü seçili yarıçap içinde patlatır.
 * Varsayılan: Taş (stone), 30 blok yarıçap.
 * Performans: İşlem birden fazla tick'e yayılır.
 */
public class CommandTntEntity extends TntEntity {
    private Block targetBlock = Blocks.STONE;
    private int radius = 30;
    private boolean done = false;

    // Kademeli işleme durumu
    private boolean processing = false;
    private BlockPos center;
    private int idx = 0;
    private static final int MODIFICATIONS_PER_TICK = 1000;

    public CommandTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public CommandTntEntity(World world, double x, double y, double z,
                            @Nullable LivingEntity igniter) {
        super(ModEntities.COMMAND_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    public void setTargetBlock(Block block) {
        this.targetBlock = block;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    @Override
    public void readData(ReadView reader) {
        super.readData(reader);
        radius = reader.getInt("radius", 30);
        done = reader.getBoolean("done", false);
        processing = reader.getBoolean("processing", false);
        idx = reader.getInt("idx", 0);
        reader.getOptionalString("targetBlock")
                .ifPresent(s -> targetBlock = Registries.BLOCK.get(Identifier.of(s)));
        if (processing || done) {
            int cx = reader.getInt("centerX", 0);
            int cy = reader.getInt("centerY", 0);
            int cz = reader.getInt("centerZ", 0);
            center = new BlockPos(cx, cy, cz);
        }
    }

    @Override
    public void writeData(WriteView writer) {
        super.writeData(writer);
        writer.putInt("radius", radius);
        writer.putBoolean("done", done);
        writer.putBoolean("processing", processing);
        writer.putInt("idx", idx);
        writer.putString("targetBlock", Registries.BLOCK.getId(targetBlock).toString());
        if (center != null) {
            writer.putInt("centerX", center.getX());
            writer.putInt("centerY", center.getY());
            writer.putInt("centerZ", center.getZ());
        }
    }

    @Override
    public void tick() {
        // Kademeli blok işleme
        if (processing && !this.getEntityWorld().isClient()) {
            World world = getEntityWorld();
            int side = radius * 2 + 1;
            int total = side * side * side;
            int modified = 0;

            while (idx < total && modified < MODIFICATIONS_PER_TICK) {
                int lx = idx % side - radius;
                int ly = (idx / side) % side - radius;
                int lz = (idx / (side * side)) - radius;
                idx++;

                BlockPos pos = center.add(lx, ly, lz);
                if (!pos.isWithinDistance(center, radius)) continue;

                BlockState state = world.getBlockState(pos);
                if (state.isOf(targetBlock)) {
                    world.breakBlock(pos, true);
                    modified++;
                }
            }

            if (idx >= total) {
                processing = false;

                // Görsel patlama efekti
                world.createExplosion(null, center.getX() + 0.5, center.getY(),
                        center.getZ() + 0.5, 3.0f, false, World.ExplosionSourceType.NONE);

                world.playSound(null, center.getX(), center.getY(), center.getZ(),
                        SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 1.0f, 1.0f);

                if (world instanceof ServerWorld serverWorld) {
                    serverWorld.spawnParticles(ParticleTypes.DUST_PLUME,
                            center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5,
                            100, 5.0, 3.0, 5.0, 0.1);
                }
                this.discard();
            }
            return;
        }

        if (!done && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            done = true;
            center = this.getBlockPos();
            processing = true;
            idx = 0;
            // Ses efekti hemen çalsın
            World world = getEntityWorld();
            world.playSound(null, center.getX(), center.getY(), center.getZ(),
                    SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 2.0f, 0.5f);
            return;
        }
        if (!done) super.tick();
    }
}
