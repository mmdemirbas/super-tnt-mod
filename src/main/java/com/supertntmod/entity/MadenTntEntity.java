package com.supertntmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Maden TNT: Patladığında her işlenmiş madenden 10 tane saçar.
 */
public class MadenTntEntity extends TntEntity {
    private boolean done = false;
    private static final Item[] ORES = {
            Items.IRON_INGOT,
            Items.GOLD_INGOT,
            Items.DIAMOND,
            Items.EMERALD,
            Items.COPPER_INGOT,
            Items.NETHERITE_INGOT,
            Items.LAPIS_LAZULI,
            Items.QUARTZ,
            Items.AMETHYST_SHARD,
            Items.COAL,
            Items.REDSTONE
    };

    public MadenTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public MadenTntEntity(World world, double x, double y, double z,
                          @Nullable LivingEntity igniter) {
        super(ModEntities.MADEN_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    @Override
    public void tick() {
        if (!done && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            done = true;
            World world = getEntityWorld();
            double x = getX(), y = getY(), z = getZ();
            this.discard();

            world.playSound(null, x, y, z, SoundEvents.ENTITY_GENERIC_EXPLODE,
                    SoundCategory.BLOCKS, 3.0f, 1.0f);
            world.playSound(null, x, y, z, SoundEvents.BLOCK_ANVIL_LAND,
                    SoundCategory.BLOCKS, 2.0f, 0.8f);

            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.EXPLOSION, x, y, z,
                        30, 3.0, 2.0, 3.0, 0.1);
                serverWorld.spawnParticles(ParticleTypes.GLOW, x, y + 1, z,
                        100, 3.0, 2.0, 3.0, 0.1);
            }

            world.createExplosion(null, x, y, z, 3.0f, false, World.ExplosionSourceType.TNT);

            // Her madenden 10 tane saçılarak düşür
            for (Item ore : ORES) {
                ItemStack stack = new ItemStack(ore, 10);
                ItemEntity itemEntity = new ItemEntity(world,
                        x, y + 1.0, z, stack);
                double angle = world.random.nextDouble() * Math.PI * 2;
                double speed = 0.3 + world.random.nextDouble() * 0.2;
                itemEntity.setVelocity(Math.cos(angle) * speed,
                        0.4 + world.random.nextDouble() * 0.3,
                        Math.sin(angle) * speed);
                world.spawnEntity(itemEntity);
            }
            return;
        }
        if (!done) super.tick();
    }
}
