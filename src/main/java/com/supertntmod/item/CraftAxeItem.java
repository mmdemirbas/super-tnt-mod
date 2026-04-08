package com.supertntmod.item;

import net.minecraft.block.BlockState;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Craft Baltası: Birinci blok kır → tip + konum kaydet.
 * İkinci blok kır → iki konum arasına birinci blok tipinden duvar inşa et.
 * Maksimum 32x32x32 = 32768 blok sınırı.
 */
public class CraftAxeItem extends Item {

    private static final int MAX_FILL = 32768;

    private record Target(BlockState blockState, BlockPos pos) {}
    private static final ConcurrentHashMap<UUID, Target> FIRST_TARGET = new ConcurrentHashMap<>();

    public CraftAxeItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context,
                              TooltipDisplayComponent displayComponent,
                              Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
        textConsumer.accept(Text.translatable("item.supertntmod.craft_axe.tooltip").formatted(Formatting.GRAY));
    }

    @Override
    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        if (world.isClient() || !(miner instanceof ServerPlayerEntity player)) {
            return super.postMine(stack, world, state, pos, miner);
        }

        UUID id = player.getUuid();

        if (!FIRST_TARGET.containsKey(id)) {
            // Birinci kırma: blok tipini ve konumu kaydet
            FIRST_TARGET.put(id, new Target(state, pos.toImmutable()));
            player.sendMessage(
                    Text.translatable("item.supertntmod.craft_axe.first_set",
                            pos.getX(), pos.getY(), pos.getZ())
                            .formatted(Formatting.AQUA),
                    true);
        } else {
            // İkinci kırma: duvar inşa et
            Target first = FIRST_TARGET.remove(id);
            buildWall(world, first.pos(), pos.toImmutable(), first.blockState(), player);
        }

        return true;
    }

    private void buildWall(World world, BlockPos pos1, BlockPos pos2,
                           BlockState block, PlayerEntity player) {
        int minX = Math.min(pos1.getX(), pos2.getX());
        int maxX = Math.max(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int maxY = Math.max(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());

        long volume = (long)(maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        if (volume > MAX_FILL) {
            player.sendMessage(
                    Text.translatable("item.supertntmod.craft_axe.too_large")
                            .formatted(Formatting.RED),
                    true);
            return;
        }

        int placed = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (world.getBlockState(p).isAir()) {
                        world.setBlockState(p, block);
                        placed++;
                    }
                }
            }
        }

        player.sendMessage(
                Text.translatable("item.supertntmod.craft_axe.built", placed)
                        .formatted(Formatting.GREEN),
                true);
    }

    /** Sunucu kapanınca tüm bekleyen durumları temizle. */
    public static void clearAll() {
        FIRST_TARGET.clear();
    }
}
