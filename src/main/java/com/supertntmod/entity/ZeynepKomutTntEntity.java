package com.supertntmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ZeynepKomutTntEntity extends TntEntity {
    private static final int RADIUS = 50;
    private boolean done = false;

    public ZeynepKomutTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public ZeynepKomutTntEntity(World world, double x, double y, double z,
                                 @Nullable LivingEntity igniter) {
        super(ModEntities.ZEYNEP_KOMUT_TNT, world);
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
                    SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 3.0f, 1.0f);

            world.getEntitiesByClass(PlayerEntity.class,
                    new net.minecraft.util.math.Box(center).expand(RADIUS),
                    e -> true
            ).forEach(player -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 6000, 4));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 6000, 4));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 6000, 4));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 6000, 4));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 6000, 0));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 6000, 0));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, 6000, 4));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 6000, 4));
                player.giveItemStack(new ItemStack(Items.NETHER_STAR, 10));
            });

            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.WITCH,
                        cx, cy + 2, cz, 200, 8.0, 4.0, 8.0, 0.3);
                serverWorld.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING,
                        cx, cy + 2, cz, 300, 8.0, 5.0, 8.0, 0.6);
            }

            // Buff TNT — bloklara zarar vermez. Tooltip "güçlü efektler verir"
            // diyor, "evi yıkar" demiyor; kid kendi yapısını kaybedince üzülür.
            world.createExplosion(null, cx, cy, cz, 0.0f, false, World.ExplosionSourceType.NONE);
            return;
        }
        if (!done) super.tick();
    }
}
