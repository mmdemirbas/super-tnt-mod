package com.supertntmod.entity;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Lego TNT: Patladığında 15 blok yarıçapındaki blokları
 * renkli beton bloklarına dönüştürür (lego parçaları gibi).
 */
public class LegoTntEntity extends TntEntity {
    private static final int RADIUS = 15;
    private boolean done = false;

    // 16 renk beton bloğu (lego parçaları)
    private static final Block[] CONCRETE_COLORS = {
            Blocks.WHITE_CONCRETE,
            Blocks.ORANGE_CONCRETE,
            Blocks.MAGENTA_CONCRETE,
            Blocks.LIGHT_BLUE_CONCRETE,
            Blocks.YELLOW_CONCRETE,
            Blocks.LIME_CONCRETE,
            Blocks.PINK_CONCRETE,
            Blocks.GRAY_CONCRETE,
            Blocks.LIGHT_GRAY_CONCRETE,
            Blocks.CYAN_CONCRETE,
            Blocks.PURPLE_CONCRETE,
            Blocks.BLUE_CONCRETE,
            Blocks.BROWN_CONCRETE,
            Blocks.GREEN_CONCRETE,
            Blocks.RED_CONCRETE,
            Blocks.BLACK_CONCRETE
    };

    public LegoTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public LegoTntEntity(World world, double x, double y, double z,
                         @Nullable LivingEntity igniter) {
        super(ModEntities.LEGO_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    @Override
    public void tick() {
        if (!done && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            done = true;
            BlockPos center = this.getBlockPos();
            World world = getEntityWorld();
            this.discard();

            // 15 blok yarıçapındaki katı blokları rastgele renkli betona dönüştür
            for (BlockPos pos : BlockPos.iterateOutwards(center, RADIUS, RADIUS, RADIUS)) {
                if (!pos.isWithinDistance(center, RADIUS)) continue;

                var state = world.getBlockState(pos);

                // Hava, sıvı ve bedrock'u atla
                if (state.isOf(Blocks.AIR) || state.isOf(Blocks.BEDROCK) ||
                    state.isOf(Blocks.WATER) || state.isOf(Blocks.LAVA) ||
                    !state.isSolidBlock(world, pos)) continue;

                // Rastgele bir beton rengi seç
                Block concrete = CONCRETE_COLORS[world.random.nextInt(CONCRETE_COLORS.length)];
                world.setBlockState(pos, concrete.getDefaultState());
            }

            // Küçük patlama efekti (bloklara zarar vermez, sadece görsel)
            world.createExplosion(null, center.getX() + 0.5, center.getY(),
                    center.getZ() + 0.5, 0.0f, false, World.ExplosionSourceType.NONE);

            world.playSound(null, center.getX(), center.getY(), center.getZ(),
                    SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, SoundCategory.BLOCKS, 2.0f, 0.8f);
            return;
        }
        if (!done) super.tick();
    }
}
