package com.supertntmod.block;

import com.supertntmod.entity.WalkingTntEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Yürüyen TNT Blok: Yerleştirildiğinde entity'ye dönüşür.
 * Enderman gibi göz göze gelince yaklaşır ve patlar.
 */
public class WalkingTntBlock extends Block {

    public WalkingTntBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
                         ItemStack itemStack) {
        if (!world.isClient()) {
            // Bloğu kaldır ve entity spawn et
            world.removeBlock(pos, false);
            WalkingTntEntity entity = new WalkingTntEntity(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            if (placer instanceof PlayerEntity player) {
                entity.setOwnerUuid(player.getUuid());
            }
            world.spawnEntity(entity);
        }
    }
}
