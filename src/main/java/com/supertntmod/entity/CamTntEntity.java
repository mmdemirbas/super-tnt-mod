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

import java.util.ArrayList;
import java.util.List;

public class CamTntEntity extends TntEntity {
    // RADIUS=100 tek tick'te 4M+ pozisyon tarayıp sunucuyu 4-30 saniye
    // donduruyordu. 30 makul (vanilla TNT~30 ile uyumlu) ve tarama hâlâ
    // tek tick'te ~14k pozisyon — 50 ms civarı, kabul edilebilir.
    private static final int RADIUS = 30;
    private static final int BLOCKS_PER_TICK = 100;

    private boolean exploded = false;
    private List<BlockPos> pendingGlass = null;

    public CamTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public CamTntEntity(World world, double x, double y, double z, @Nullable LivingEntity igniter) {
        super(ModEntities.CAM_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    @Override
    public void tick() {
        if (pendingGlass != null) {
            if (!pendingGlass.isEmpty()) {
                World world = getEntityWorld();
                int processed = 0;
                while (!pendingGlass.isEmpty() && processed < BLOCKS_PER_TICK) {
                    BlockPos pos = pendingGlass.remove(pendingGlass.size() - 1);
                    world.breakBlock(pos, false);
                    processed++;
                }
            } else {
                this.discard();
            }
            return;
        }

        if (!exploded && this.getFuse() <= 1 && !getEntityWorld().isClient()) {
            exploded = true;
            double x = getX(), y = getY(), z = getZ();
            World world = getEntityWorld();

            world.playSound(null, x, y, z, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 3.0f, 0.5f);
            world.createExplosion(null, x, y, z, 2.0f, false, World.ExplosionSourceType.TNT);

            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.CRIT, x, y + 1, z, 200, 10.0, 5.0, 10.0, 0.5);
            }

            BlockPos center = BlockPos.ofFloored(x, y, z);
            pendingGlass = new ArrayList<>();
            BlockPos.Mutable cursor = new BlockPos.Mutable();
            for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                for (int dy = -RADIUS; dy <= RADIUS; dy++) {
                    for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                        if (dx * dx + dy * dy + dz * dz > RADIUS * RADIUS) continue;
                        cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                        var block = world.getBlockState(cursor).getBlock();
                        String blockPath = net.minecraft.registry.Registries.BLOCK.getId(block).getPath();
                        if (blockPath.contains("glass")) {
                            pendingGlass.add(cursor.toImmutable());
                        }
                    }
                }
            }
            return;
        }
        if (!exploded) super.tick();
    }
}
