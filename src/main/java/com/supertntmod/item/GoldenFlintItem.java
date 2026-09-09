package com.supertntmod.item;

import com.supertntmod.block.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
 * 🟡 Altın Flint — tıklanan yüzeye Altın Ateş koyar ve tıklanan bloğu
 * altın bloğuna çevirir.
 *
 * Dönüşüm burada, blokta değil: "hangi bloğun üzerine yakıldıysa" bilgisi
 * yalnızca kullanım bağlamında var; ateş bloğu kendi altındakini bilse yan
 * yüzeye yakıldığında yanlış bloğu çevirirdi.
 */
public class GoldenFlintItem extends Item {

    public GoldenFlintItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos clickedPos = context.getBlockPos();
        BlockPos firePos = clickedPos.offset(context.getSide());
        if (!world.getBlockState(firePos).isReplaceable()) return ActionResult.PASS;

        if (!world.isClient()) {
            BlockState clickedState = world.getBlockState(clickedPos);
            if (canTurnToGold(world, clickedPos, clickedState)) {
                world.setBlockState(clickedPos, Blocks.GOLD_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
            }
            world.setBlockState(firePos, ModBlocks.GOLD_FIRE.getDefaultState(), Block.NOTIFY_ALL);
            PlayerEntity player = context.getPlayer();
            if (player != null) {
                context.getStack().damage(1, player);
            }
        }
        world.playSound(null, firePos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1.0f, 1.4f);
        return ActionResult.SUCCESS;
    }

    /**
     * Kırılamaz bloklar (bedrock, bariyer, portal çerçevesi) ve sandık gibi
     * içerik taşıyan bloklar dışarıda: birincisi dünyanın tabanını delerdi,
     * ikincisi eşyaları sessizce yok ederdi.
     */
    private static boolean canTurnToGold(World world, BlockPos pos, BlockState state) {
        if (state.isAir() || state.isLiquid() || state.hasBlockEntity()) return false;
        if (state.isOf(Blocks.GOLD_BLOCK)) return false;
        return state.getHardness(world, pos) >= 0;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context,
                              TooltipDisplayComponent displayComponent,
                              Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
        textConsumer.accept(Text.translatable("item.supertntmod.golden_flint.tooltip")
                .formatted(Formatting.GOLD));
        textConsumer.accept(Text.translatable("item.supertntmod.golden_flint.tooltip2")
                .formatted(Formatting.RED));
    }
}
