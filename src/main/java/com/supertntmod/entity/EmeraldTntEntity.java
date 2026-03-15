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
 * 💚 Zümrüt TNT
 * Patlama sonrası çevreye hazine yağdırır:
 * emerald, diamond, gold, lapis lazuli.
 * Hazine sandığı gibi!
 */
public class EmeraldTntEntity extends TntEntity {
    private boolean done = false;

    // Hazine tablosu: item, ağırlık (daha yüksek = daha sık)
    private static final Item[] TREASURE_ITEMS = {
            Items.EMERALD, Items.EMERALD, Items.EMERALD, Items.EMERALD, // %40
            Items.GOLD_INGOT, Items.GOLD_INGOT, Items.GOLD_INGOT,       // %30
            Items.DIAMOND,                                                // %10
            Items.LAPIS_LAZULI, Items.LAPIS_LAZULI,                      // %20
    };

    public EmeraldTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public EmeraldTntEntity(World world, double x, double y, double z,
                            @Nullable LivingEntity igniter) {
        super(ModEntities.EMERALD_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    @Override
    public void tick() {
        if (!done && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            done = true;
            double x = getX(), y = getY(), z = getZ();
            World world = getEntityWorld();
            this.discard();

            // Hazine sesi
            world.playSound(null, x, y, z,
                    SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.BLOCKS, 2.0f, 1.2f);

            // Normal patlama
            world.createExplosion(null, x, y, z, 4.0f, true,
                    World.ExplosionSourceType.TNT);

            // Hazine yağmuru (24-40 adet)
            int count = 24 + world.random.nextInt(16);
            for (int i = 0; i < count; i++) {
                Item treasureItem = TREASURE_ITEMS[world.random.nextInt(TREASURE_ITEMS.length)];
                int stackSize = treasureItem == Items.DIAMOND ? 1 : 1 + world.random.nextInt(3);

                ItemEntity item = new ItemEntity(world,
                        x + (world.random.nextDouble() - 0.5) * 6,
                        y + 2,
                        z + (world.random.nextDouble() - 0.5) * 6,
                        new ItemStack(treasureItem, stackSize));
                item.setVelocity(
                        (world.random.nextDouble() - 0.5) * 0.8,
                        world.random.nextDouble() * 1.0 + 0.5,
                        (world.random.nextDouble() - 0.5) * 0.8);
                world.spawnEntity(item);
            }

            // Mutlu köylü partikülleri + ışıltı
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                        x, y + 2, z, 80, 4.0, 3.0, 4.0, 0.1);
                serverWorld.spawnParticles(ParticleTypes.COMPOSTER,
                        x, y + 3, z, 50, 3.0, 4.0, 3.0, 0.05);
            }
            return;
        }
        if (!done) super.tick();
    }
}
