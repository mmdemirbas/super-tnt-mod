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
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Dünya TNT: Patlatıldığında etrafa "dünya" benzeri itemler saçar
 * (ender pearl + dirt/grass blokları, mavi buz parçacıkları).
 */
public class DunyaTntEntity extends TntEntity {
    private boolean done = false;

    public DunyaTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public DunyaTntEntity(World world, double x, double y, double z,
                          @Nullable LivingEntity igniter) {
        super(ModEntities.DUNYA_TNT, world);
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

            world.playSound(null, x, y, z, SoundEvents.BLOCK_PORTAL_TRIGGER,
                    SoundCategory.BLOCKS, 3.0f, 0.5f);
            world.playSound(null, x, y, z, SoundEvents.ENTITY_GENERIC_EXPLODE,
                    SoundCategory.BLOCKS, 2.5f, 0.7f);

            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.PORTAL,
                        x, y + 1, z, 400, 6.0, 4.0, 6.0, 0.3);
                serverWorld.spawnParticles(ParticleTypes.END_ROD,
                        x, y + 1, z, 200, 8.0, 4.0, 8.0, 0.1);
            }

            world.createExplosion(null, x, y, z, 2.0f, false, World.ExplosionSourceType.TNT);

            // Bir sürü "dünya" — ender pearl + dirt + glow blockları
            for (int i = 0; i < 30; i++) {
                spawn(world, x, y, z, new ItemStack(Items.ENDER_PEARL));
            }
            for (int i = 0; i < 20; i++) {
                spawn(world, x, y, z, new ItemStack(Items.DIRT));
            }
            for (int i = 0; i < 15; i++) {
                spawn(world, x, y, z, new ItemStack(Items.GRASS_BLOCK));
            }
            for (int i = 0; i < 15; i++) {
                spawn(world, x, y, z, new ItemStack(Items.WATER_BUCKET));
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
