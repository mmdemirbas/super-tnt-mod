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
 * Çizgi Çalışması TNT: Patladığında her yere kağıt ve mürekkep saçar —
 * "her yer çizgi çalışması olur".
 */
public class CizgiTntEntity extends TntEntity {
    private boolean done = false;

    public CizgiTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public CizgiTntEntity(World world, double x, double y, double z,
                          @Nullable LivingEntity igniter) {
        super(ModEntities.CIZGI_TNT, world);
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

            world.playSound(null, x, y, z, SoundEvents.ITEM_BOOK_PAGE_TURN,
                    SoundCategory.BLOCKS, 3.0f, 1.0f);
            world.playSound(null, x, y, z, SoundEvents.ENTITY_GENERIC_EXPLODE,
                    SoundCategory.BLOCKS, 2.0f, 1.2f);

            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.SQUID_INK,
                        x, y + 1, z, 200, 6.0, 4.0, 6.0, 0.1);
                serverWorld.spawnParticles(ParticleTypes.WHITE_ASH,
                        x, y + 2, z, 300, 8.0, 5.0, 8.0, 0.05);
            }

            world.createExplosion(null, x, y, z, 1.5f, false, World.ExplosionSourceType.NONE);

            // Çok bol kağıt + mürekkep + tüy
            for (int i = 0; i < 64; i++) {
                spawn(world, x, y, z, new ItemStack(Items.PAPER, 16));
            }
            for (int i = 0; i < 20; i++) {
                spawn(world, x, y, z, new ItemStack(Items.INK_SAC, 8));
            }
            for (int i = 0; i < 20; i++) {
                spawn(world, x, y, z, new ItemStack(Items.FEATHER, 4));
            }
            return;
        }
        if (!done) super.tick();
    }

    private static void spawn(World world, double x, double y, double z, ItemStack stack) {
        ItemEntity item = new ItemEntity(world, x, y + 1.0, z, stack);
        double angle = world.random.nextDouble() * Math.PI * 2;
        double speed = 0.4 + world.random.nextDouble() * 0.5;
        item.setVelocity(Math.cos(angle) * speed,
                0.5 + world.random.nextDouble() * 0.5,
                Math.sin(angle) * speed);
        world.spawnEntity(item);
    }
}
