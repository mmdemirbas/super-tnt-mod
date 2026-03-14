package com.supertntmod.entity;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * 🪨 Bedrock TNT
 * Blast resistance hesaba katmadan her bloğu kırar.
 * Bedrock, obsidian, taş - hepsi gider.
 */
public class BedrockTntEntity extends TntEntity {
    private static final int RADIUS = 7;
    private boolean done = false;

    public BedrockTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public BedrockTntEntity(World world, double x, double y, double z,
                            @Nullable LivingEntity igniter) {
        super(ModEntities.BEDROCK_TNT, world);
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

            // Blast resistance'ı tamamen atla, doğrudan kır
            for (BlockPos pos : BlockPos.iterateOutwards(center, RADIUS, RADIUS, RADIUS)) {
                if (pos.isWithinDistance(center, RADIUS)) {
                    if (!world.getBlockState(pos).isOf(Blocks.AIR)) {
                        world.breakBlock(pos, true); // true = drop items
                    }
                }
            }
            // Görsel patlama efekti
            world.createExplosion(null, center.getX() + 0.5, center.getY(),
                    center.getZ() + 0.5, 6.0f, false, World.ExplosionSourceType.TNT);
            return;
        }
        if (!done) super.tick();
    }
}
