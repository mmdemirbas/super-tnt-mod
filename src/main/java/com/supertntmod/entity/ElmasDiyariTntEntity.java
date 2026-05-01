package com.supertntmod.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
 * 💎 Elmas Diyarı TNT — patladığında elmas saçar VE etrafı elmas bloklarına çevirir.
 * Çok güçlü TNT: 30 blok yarıçapında dünyayı elmas blokuna dönüştürür (kademeli).
 */
public class ElmasDiyariTntEntity extends TntEntity {
    private static final int RADIUS = 30;
    private static final int DIAMOND_DROP = 64;
    private static final int OPS_PER_TICK = 800;

    private boolean exploded = false;
    private boolean processing = false;
    private BlockPos center;
    private int idx = 0;

    public ElmasDiyariTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public ElmasDiyariTntEntity(World world, double x, double y, double z, @Nullable LivingEntity igniter) {
        super(ModEntities.ELMAS_DIYARI_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    @Override
    public void tick() {
        if (!exploded && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            exploded = true;
            center = this.getBlockPos();
            double cx = center.getX() + 0.5, cy = center.getY(), cz = center.getZ() + 0.5;
            World world = getEntityWorld();

            world.playSound(null, cx, cy, cz,
                    SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 2.5f, 0.7f);

            for (int i = 0; i < DIAMOND_DROP; i++) {
                double ox = (world.random.nextDouble() - 0.5) * 8.0;
                double oz = (world.random.nextDouble() - 0.5) * 8.0;
                ItemEntity gem = new ItemEntity(world, cx + ox, cy + 1.5, cz + oz,
                        new ItemStack(Items.DIAMOND));
                gem.setVelocity(
                        (world.random.nextDouble() - 0.5) * 0.6,
                        world.random.nextDouble() * 0.5 + 0.3,
                        (world.random.nextDouble() - 0.5) * 0.6);
                world.spawnEntity(gem);
            }

            if (world instanceof ServerWorld sw) {
                sw.spawnParticles(ParticleTypes.GLOW,
                        cx, cy + 2, cz, 200, 6.0, 4.0, 6.0, 0.5);
            }

            processing = true;
            idx = 0;
            return;
        }

        if (processing && !this.getEntityWorld().isClient()) {
            World world = getEntityWorld();
            int diameter = RADIUS * 2 + 1;
            int total = diameter * diameter * diameter;
            int processed = 0;

            while (idx < total && processed < OPS_PER_TICK) {
                int dx = (idx % diameter) - RADIUS;
                int dy = ((idx / diameter) % diameter) - RADIUS;
                int dz = (idx / (diameter * diameter)) - RADIUS;
                idx++;

                if (dx * dx + dy * dy + dz * dz > RADIUS * RADIUS) continue;

                BlockPos pos = center.add(dx, dy, dz);
                BlockState state = world.getBlockState(pos);
                if (state.isAir()) continue;
                if (state.isOf(Blocks.BEDROCK)) continue;
                if (state.isOf(Blocks.DIAMOND_BLOCK)) continue;
                if (state.getHardness(world, pos) < 0) continue;

                world.setBlockState(pos, Blocks.DIAMOND_BLOCK.getDefaultState(), 2);
                processed++;
            }

            if (idx >= total) {
                processing = false;
                this.discard();
            }
            return;
        }

        if (!exploded) super.tick();
    }
}
