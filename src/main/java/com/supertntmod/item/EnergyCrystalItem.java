package com.supertntmod.item;

import net.minecraft.block.Blocks;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.function.Consumer;

/**
 * Enerji Kristali: Kırılmaz bedrock bloklarını kırmaya yarar.
 * Bedrock kırıldığında altından yeni bedrock çıkar.
 */
public class EnergyCrystalItem extends Item {

    public EnergyCrystalItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context,
                              TooltipDisplayComponent displayComponent,
                              Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
        textConsumer.accept(Text.translatable("item.supertntmod.energy_crystal.tooltip").formatted(Formatting.GRAY));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        PlayerEntity player = context.getPlayer();

        if (!world.getBlockState(pos).isOf(Blocks.BEDROCK)) {
            return ActionResult.PASS;
        }

        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        // Bedrock'u kır (hava ile değiştir)
        world.setBlockState(pos, Blocks.AIR.getDefaultState());

        // Altından yeni bedrock çıkar (dünya tabanının dışına geçme)
        BlockPos below = pos.down();
        if (below.getY() >= world.getBottomY() && world.getBlockState(below).isAir()) {
            world.setBlockState(below, Blocks.BEDROCK.getDefaultState());
        }

        // Efektler
        world.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.BLOCKS, 1.0f, 0.5f);
        if (world instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    20, 0.3, 0.3, 0.3, 0.05);
        }

        // Durabilite düşür veya sayıyı azalt (burada sadece bilgi mesajı)
        if (player != null) {
            player.sendMessage(Text.translatable("item.supertntmod.energy_crystal.broken"), true);
        }

        return ActionResult.SUCCESS;
    }
}
