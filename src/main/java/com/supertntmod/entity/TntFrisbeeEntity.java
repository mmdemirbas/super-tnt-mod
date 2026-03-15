package com.supertntmod.entity;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import com.supertntmod.item.ModItems;

/**
 * TNT Frizbi Entity: Havada döner, yere düştüğünde + şeklinde patlama yapar.
 * Sahibine zarar vermez, patladıktan sonra sahibine geri döner.
 */
public class TntFrisbeeEntity extends ThrownEntity {
    private static final int PLUS_LENGTH = 10;
    private static final int PLUS_DEPTH = 3;

    public TntFrisbeeEntity(EntityType<? extends ThrownEntity> type, World world) {
        super(type, world);
    }

    public TntFrisbeeEntity(World world, PlayerEntity owner) {
        super(ModEntities.TNT_FRISBEE, owner.getX(), owner.getEyeY() - 0.1, owner.getZ(), world);
        this.setOwner(owner);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        // Ekstra data tracker gerekmez
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        super.onBlockHit(blockHitResult);

        if (this.getEntityWorld().isClient()) return;

        BlockPos hitPos = blockHitResult.getBlockPos();
        World world = this.getEntityWorld();

        // + şeklinde patlama (yatay düzlemde)
        // Yatay kol: X yönü
        for (int x = -PLUS_LENGTH; x <= PLUS_LENGTH; x++) {
            for (int depth = 0; depth < PLUS_DEPTH; depth++) {
                BlockPos pos = hitPos.add(x, -depth, 0);
                if (!world.getBlockState(pos).isOf(Blocks.AIR) &&
                    !world.getBlockState(pos).isOf(Blocks.BEDROCK)) {
                    world.breakBlock(pos, false);
                }
            }
        }
        // Dikey kol: Z yönü
        for (int z = -PLUS_LENGTH; z <= PLUS_LENGTH; z++) {
            for (int depth = 0; depth < PLUS_DEPTH; depth++) {
                BlockPos pos = hitPos.add(0, -depth, z);
                if (!world.getBlockState(pos).isOf(Blocks.AIR) &&
                    !world.getBlockState(pos).isOf(Blocks.BEDROCK)) {
                    world.breakBlock(pos, false);
                }
            }
        }

        // Patlama efekti (bloklara ekstra zarar vermez)
        world.createExplosion(this.getOwner(), hitPos.getX() + 0.5, hitPos.getY(),
                hitPos.getZ() + 0.5, 2.0f, false, World.ExplosionSourceType.NONE);

        world.playSound(null, hitPos.getX(), hitPos.getY(), hitPos.getZ(),
                SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 1.0f, 1.0f);

        // Sahibine geri dön (item olarak)
        if (this.getOwner() instanceof PlayerEntity owner && owner.isAlive()) {
            ItemEntity returnItem = new ItemEntity(world,
                    owner.getX(), owner.getY() + 0.5, owner.getZ(),
                    new ItemStack(ModItems.TNT_FRISBEE));
            returnItem.setPickupDelay(0);
            returnItem.setVelocity(0, 0.2, 0);
            world.spawnEntity(returnItem);
        }

        this.discard();
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        // Sahibine çarparsa bir şey yapma
        if (entityHitResult.getEntity() == this.getOwner()) return;
        super.onEntityHit(entityHitResult);
    }
}
