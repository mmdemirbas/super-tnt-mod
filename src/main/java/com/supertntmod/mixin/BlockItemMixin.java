package com.supertntmod.mixin;

import com.supertntmod.SuperTntMod;
import com.supertntmod.block.ModBlocks;
import com.supertntmod.block.TunneledBlock;
import com.supertntmod.block.TunneledBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.Registries;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Küçülmüş oyuncu blok yerleştirdiğinde, bloğu boyutuyla orantılı
 * küçük bir TunneledBlock olarak yerleştirir.
 */
@Mixin(BlockItem.class)
public class BlockItemMixin {

    @Inject(method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;",
            at = @At("RETURN"))
    private void supertntmod$onBlockPlaced(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (cir.getReturnValue() != ActionResult.SUCCESS) return;

        PlayerEntity player = context.getPlayer();
        if (player == null) return;

        double scale = player.getAttributeInstance(EntityAttributes.SCALE) != null
                ? player.getAttributeInstance(EntityAttributes.SCALE).getValue()
                : 1.0;
        if (scale >= 1.0) return;

        World world = context.getWorld();
        if (world.isClient()) return;

        BlockPos pos = context.getBlockPos();
        BlockState placedState = world.getBlockState(pos);

        // TunneledBlock, BlockEntity'li veya mod'a ait blokları dönüştürme
        if (placedState.getBlock() instanceof TunneledBlock) return;
        if (placedState.hasBlockEntity()) return;
        // Supertntmod bloklarını dönüştürme — TNT ve özel bloklar tam boyutlu kalmalı
        if (Registries.BLOCK.getId(placedState.getBlock()).getNamespace().equals(SuperTntMod.MOD_ID)) return;

        // Sub-voxel sayısını hesapla (eksen başına)
        int count = Math.max(1, (int) Math.ceil(scale * 4));
        if (count >= 4) return; // Tam boyut, dönüşüm gereksiz

        // Yerleştirme pozisyonunu hesapla: tıklanan noktaya en yakın köşe
        Vec3d hitPos = context.getHitPos();
        double localX = hitPos.x - pos.getX();
        double localY = hitPos.y - pos.getY();
        double localZ = hitPos.z - pos.getZ();

        int startX = Math.min(4 - count, Math.max(0, (int) (localX * 4)));
        int startY = Math.min(4 - count, Math.max(0, (int) (localY * 4)));
        int startZ = Math.min(4 - count, Math.max(0, (int) (localZ * 4)));

        // Yerleştirme yüzeyine göre ayarla: blok, tıklanan yüzeye yapışık olmalı
        Direction side = context.getSide();
        switch (side) {
            case UP -> startY = 0;
            case DOWN -> startY = 4 - count;
            case NORTH -> startZ = 4 - count;
            case SOUTH -> startZ = 0;
            case EAST -> startX = 0;
            case WEST -> startX = 4 - count;
        }

        // Bitmask oluştur: sadece belirtilen bölgeyi doldur
        long mask = 0L;
        for (int z = startZ; z < startZ + count; z++) {
            for (int y = startY; y < startY + count; y++) {
                for (int x = startX; x < startX + count; x++) {
                    mask |= (1L << TunneledBlockEntity.subVoxelIndex(x, y, z));
                }
            }
        }

        // Orijinal blok bilgisini kaydet ve TunneledBlock'a dönüştür
        Identifier blockId = Registries.BLOCK.getId(placedState.getBlock());
        world.setBlockState(pos, ModBlocks.TUNNELED_BLOCK.getDefaultState());
        if (world.getBlockEntity(pos) instanceof TunneledBlockEntity entity) {
            entity.setOriginalBlockId(blockId);
            entity.setSubVoxelMask(mask);
        }
    }
}
