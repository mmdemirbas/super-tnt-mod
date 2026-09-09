package com.supertntmod.item;

import com.supertntmod.block.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.function.Consumer;

/**
 * 🔵 Ender Çakmağı — tıklanan yüzeye Ender Ateşi koyar.
 * Ateşin ışınlama davranışı {@link com.supertntmod.block.EnderFireBlock} içinde.
 */
public class EnderFlintItem extends Item {

    public EnderFlintItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos firePos = context.getBlockPos().offset(context.getSide());
        if (!world.getBlockState(firePos).isReplaceable()) return ActionResult.PASS;

        if (!world.isClient()) {
            world.setBlockState(firePos, ModBlocks.ENDER_FIRE.getDefaultState(), Block.NOTIFY_ALL);
            PlayerEntity player = context.getPlayer();
            if (player != null) {
                context.getStack().damage(1, player);
            }
        }
        world.playSound(null, firePos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1.0f, 0.6f);
        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context,
                              TooltipDisplayComponent displayComponent,
                              Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
        textConsumer.accept(Text.translatable("item.supertntmod.ender_flint.tooltip")
                .formatted(Formatting.LIGHT_PURPLE));
        textConsumer.accept(Text.translatable("item.supertntmod.ender_flint.tooltip2")
                .formatted(Formatting.GRAY));
    }
}
