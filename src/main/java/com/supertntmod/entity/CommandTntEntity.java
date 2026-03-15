package com.supertntmod.entity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
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
 * Komut Bloğu TNT: Belirli bir blok türünü seçili yarıçap içinde patlatır.
 * Varsayılan: Taş (stone), 30 blok yarıçap.
 * Blok türü ve yarıçap CommandTntBlock üzerinden ayarlanabilir.
 */
public class CommandTntEntity extends TntEntity {
    private Block targetBlock = Blocks.STONE;
    private int radius = 30;
    private boolean done = false;

    public CommandTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public CommandTntEntity(World world, double x, double y, double z,
                            @Nullable LivingEntity igniter) {
        super(ModEntities.COMMAND_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    public void setTargetBlock(Block block) {
        this.targetBlock = block;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    @Override
    public void tick() {
        if (!done && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            done = true;
            BlockPos center = this.getBlockPos();
            World world = getEntityWorld();
            this.discard();

            // Hedef blok türünü yarıçap içinde yok et
            int destroyed = 0;
            for (BlockPos pos : BlockPos.iterateOutwards(center, radius, radius, radius)) {
                if (!pos.isWithinDistance(center, radius)) continue;

                BlockState state = world.getBlockState(pos);
                if (state.isOf(targetBlock)) {
                    world.breakBlock(pos, true); // drop items
                    destroyed++;
                }
            }

            // Görsel patlama efekti
            world.createExplosion(null, center.getX() + 0.5, center.getY(),
                    center.getZ() + 0.5, 3.0f, false, World.ExplosionSourceType.NONE);

            world.playSound(null, center.getX(), center.getY(), center.getZ(),
                    SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 1.0f, 1.0f);
            return;
        }
        if (!done) super.tick();
    }
}
