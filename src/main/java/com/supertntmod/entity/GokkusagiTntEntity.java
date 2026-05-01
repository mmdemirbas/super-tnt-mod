package com.supertntmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Gökkuşağı TNT: Patladığında çok güzel gökkuşakları saçar — renkli yünler,
 * cam parçacıkları, ışık partikülleri.
 */
public class GokkusagiTntEntity extends TntEntity {
    private boolean done = false;
    private static final Item[] RAINBOW_ITEMS = {
            Items.RED_WOOL, Items.ORANGE_WOOL, Items.YELLOW_WOOL,
            Items.LIME_WOOL, Items.LIGHT_BLUE_WOOL, Items.BLUE_WOOL,
            Items.PURPLE_WOOL, Items.MAGENTA_WOOL, Items.PINK_WOOL,
            Items.RED_DYE, Items.ORANGE_DYE, Items.YELLOW_DYE,
            Items.LIME_DYE, Items.BLUE_DYE, Items.PURPLE_DYE
    };

    public GokkusagiTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public GokkusagiTntEntity(World world, double x, double y, double z,
                              @Nullable LivingEntity igniter) {
        super(ModEntities.GOKKUSAGI_TNT, world);
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

            world.playSound(null, x, y, z, SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST,
                    SoundCategory.BLOCKS, 3.0f, 1.5f);
            world.playSound(null, x, y, z, SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE,
                    SoundCategory.BLOCKS, 3.0f, 1.5f);
            world.playSound(null, x, y, z, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                    SoundCategory.BLOCKS, 2.0f, 1.8f);

            if (world instanceof ServerWorld serverWorld) {
                // Gökkuşağı renkleri
                serverWorld.spawnParticles(ParticleTypes.END_ROD,
                        x, y + 3, z, 200, 10.0, 5.0, 10.0, 0.1);
                serverWorld.spawnParticles(ParticleTypes.GLOW,
                        x, y + 3, z, 200, 10.0, 5.0, 10.0, 0.1);
                serverWorld.spawnParticles(ParticleTypes.FIREWORK,
                        x, y + 4, z, 300, 12.0, 6.0, 12.0, 0.2);
                serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                        x, y + 2, z, 200, 8.0, 4.0, 8.0, 0.1);
            }

            world.createExplosion(null, x, y, z, 1.5f, false, World.ExplosionSourceType.NONE);

            // Renkli yünler
            for (Item it : RAINBOW_ITEMS) {
                for (int i = 0; i < 4; i++) {
                    spawn(world, x, y, z, new ItemStack(it, 4));
                }
            }
            return;
        }
        if (!done) super.tick();
    }

    private static void spawn(World world, double x, double y, double z, ItemStack stack) {
        ItemEntity item = new ItemEntity(world, x, y + 1.0, z, stack);
        double angle = world.random.nextDouble() * Math.PI * 2;
        double speed = 0.4 + world.random.nextDouble() * 0.4;
        item.setVelocity(Math.cos(angle) * speed,
                0.5 + world.random.nextDouble() * 0.5,
                Math.sin(angle) * speed);
        world.spawnEntity(item);
    }
}
