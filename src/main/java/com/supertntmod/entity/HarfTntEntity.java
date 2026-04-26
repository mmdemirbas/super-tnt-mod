package com.supertntmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class HarfTntEntity extends TntEntity {
    private static final int RADIUS = 15;
    private boolean done = false;

    public HarfTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public HarfTntEntity(World world, double x, double y, double z,
                         @Nullable LivingEntity igniter) {
        super(ModEntities.HARF_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    @Override
    public void tick() {
        if (!done && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            done = true;
            BlockPos center = this.getBlockPos();
            double cx = center.getX() + 0.5, cy = center.getY(), cz = center.getZ() + 0.5;
            World world = getEntityWorld();
            this.discard();

            world.playSound(null, cx, cy, cz,
                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.BLOCKS, 2.0f, 1.2f);

            // Sadece oyunculara can yenileme ve kalkan ver. Hostile mob'lara
            // buff vermek kid için tehlikeliydi — eskisi tüm LivingEntity'ye
            // veriyordu, zombileri/iskeletleri zorlaştırıyordu.
            world.getEntitiesByClass(net.minecraft.entity.player.PlayerEntity.class,
                    new net.minecraft.util.math.Box(center).expand(RADIUS),
                    e -> true
            ).forEach(player -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 400, 1));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 400, 2));
            });

            for (int i = 0; i < 30; i++) {
                double ox = (world.random.nextDouble() - 0.5) * 10;
                double oz = (world.random.nextDouble() - 0.5) * 10;
                ItemEntity paper = new ItemEntity(world, cx + ox, cy + 1, cz + oz,
                        new ItemStack(Items.PAPER));
                world.spawnEntity(paper);
            }

            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.HEART,
                        cx, cy + 2, cz, 30, 5.0, 3.0, 5.0, 0.1);
                serverWorld.spawnParticles(ParticleTypes.COMPOSTER,
                        cx, cy + 1, cz, 80, 5.0, 2.0, 5.0, 0.1);
            }

            world.createExplosion(null, cx, cy, cz, 0.0f, false, World.ExplosionSourceType.NONE);
            return;
        }
        if (!done) super.tick();
    }
}
