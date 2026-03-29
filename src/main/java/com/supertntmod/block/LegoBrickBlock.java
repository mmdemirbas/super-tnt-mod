package com.supertntmod.block;

import com.supertntmod.item.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Lego tuğla bloğu: 16 renk destekler (0-15).
 * Üzerinde lego çıkıntıları olan dekoratif blok.
 * Sadece Lego TNT tarafından oluşturulur.
 * Pembe (6) ve yeşil (13) renkleri kırılınca ilgili itemi düşürür.
 */
public class LegoBrickBlock extends Block {
    public static final IntProperty COLOR = IntProperty.of("color", 0, 15);
    public static final int PINK = 6;
    public static final int GREEN = 13;

    public LegoBrickBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(COLOR, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(COLOR);
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient() && !player.isCreative()) {
            int color = state.get(COLOR);
            ItemStack drop = null;
            if (color == PINK) {
                drop = new ItemStack(ModItems.PINK_LEGO_BRICK);
            } else if (color == GREEN) {
                drop = new ItemStack(ModItems.GREEN_LEGO_BRICK);
            }
            if (drop != null) {
                ItemEntity itemEntity = new ItemEntity(world,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
                world.spawnEntity(itemEntity);
            }
        }
        return super.onBreak(world, pos, state, player);
    }
}
