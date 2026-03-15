package com.supertntmod.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;

/**
 * Lego tuğla bloğu: 16 renk destekler (0-15).
 * Üzerinde lego çıkıntıları olan dekoratif blok.
 * Sadece Lego TNT tarafından oluşturulur.
 */
public class LegoBrickBlock extends Block {
    public static final IntProperty COLOR = IntProperty.of("color", 0, 15);

    public LegoBrickBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(COLOR, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(COLOR);
    }
}
