package com.supertntmod.entity;

import com.supertntmod.block.PortalBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Portal silahı mermisi: Bloğa çarptığında portal yerleştirir.
 * Pembe veya yeşil portal açar.
 */
public class PortalProjectileEntity extends ThrownEntity {
    private boolean isPink = true;

    public PortalProjectileEntity(EntityType<? extends ThrownEntity> type, World world) {
        super(type, world);
    }

    public PortalProjectileEntity(World world, PlayerEntity owner, boolean isPink) {
        super(ModEntities.PORTAL_PROJECTILE, owner.getX(), owner.getEyeY() - 0.1, owner.getZ(), world);
        this.setOwner(owner);
        this.isPink = isPink;
    }

    public boolean isPink() {
        return isPink;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        // Sunucu tarafında field yeterli, data tracker gerekmez
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        super.onBlockHit(blockHitResult);
        if (this.getEntityWorld().isClient()) return;

        World world = this.getEntityWorld();
        BlockPos hitPos = blockHitResult.getBlockPos();
        Direction side = blockHitResult.getSide();

        // Portalı çarpılan yüzün yanına yerleştir
        BlockPos portalPos = hitPos.offset(side);

        // Yerleştirilebilir mi kontrol et (hava veya değiştirilebilir blok)
        if (!world.getBlockState(portalPos).isReplaceable() && !world.getBlockState(portalPos).isAir()) {
            this.discard();
            return;
        }

        if (this.getOwner() instanceof PlayerEntity owner) {
            PortalBlock.placePortal(world, portalPos, isPink, owner.getUuid());
        }

        this.discard();
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        // Entity'lere çarpmayı yoksay
        if (entityHitResult.getEntity() == this.getOwner()) return;
    }
}
