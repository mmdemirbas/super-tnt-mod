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
 * Crafting Table TNT: Patladığında etrafa altın kasklar, netherite kılıçlar,
 * demir baltalar ve bir netherite külçesi saçar.
 */
public class CraftingTableTntEntity extends TntEntity {
    private boolean done = false;

    public CraftingTableTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public CraftingTableTntEntity(World world, double x, double y, double z,
                                  @Nullable LivingEntity igniter) {
        super(ModEntities.CRAFTING_TABLE_TNT, world);
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
            world.playSound(null, x, y, z, SoundEvents.UI_LOOM_TAKE_RESULT,
                    SoundCategory.BLOCKS, 2.0f, 0.7f);

            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.EXPLOSION, x, y, z,
                        20, 3.0, 2.0, 3.0, 0.1);
            }

            world.createExplosion(null, x, y, z, 3.0f, false, World.ExplosionSourceType.TNT);

            // Bir sürü altın kask
            for (int i = 0; i < 8; i++) {
                spawnDrop(world, x, y, z, new ItemStack(Items.GOLDEN_HELMET));
            }
            // Bir sürü netherite kılıç
            for (int i = 0; i < 6; i++) {
                spawnDrop(world, x, y, z, new ItemStack(Items.NETHERITE_SWORD));
            }
            // Bir sürü demir balta
            for (int i = 0; i < 8; i++) {
                spawnDrop(world, x, y, z, new ItemStack(Items.IRON_AXE));
            }
            // Tek bir netherite külçesi
            spawnDrop(world, x, y, z, new ItemStack(Items.NETHERITE_INGOT));
            return;
        }
        if (!done) super.tick();
    }

    private static void spawnDrop(World world, double x, double y, double z, ItemStack stack) {
        ItemEntity item = new ItemEntity(world, x, y + 1.0, z, stack);
        double angle = world.random.nextDouble() * Math.PI * 2;
        double speed = 0.3 + world.random.nextDouble() * 0.3;
        item.setVelocity(Math.cos(angle) * speed,
                0.4 + world.random.nextDouble() * 0.4,
                Math.sin(angle) * speed);
        world.spawnEntity(item);
    }
}
