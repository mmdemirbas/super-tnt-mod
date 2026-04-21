package com.supertntmod.block;

import com.supertntmod.entity.CommandTntEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Komut Bloğu TNT: Elindeki blokla sağ tıkla hedef ayarla, eğil+sağ tıkla yarıçap değiştir.
 *
 * Kullanım:
 * - Elinde blok tutarak sağ tıkla → hedef blok ayarlanır
 * - Eğilerek (sneak) boş elle sağ tıkla → yarıçap değiştirir
 * - Boş elle sağ tıkla → mevcut ayarları gösterir
 * - Çakmak/ateş topu ile sağ tıkla → ateşler
 */
public class CommandTntBlock extends CustomTntBlock {

    // Blok pozisyonuna göre ayarlar
    private static final Map<BlockPos, Block> TARGET_BLOCKS = new HashMap<>();
    private static final Map<BlockPos, Integer> RADII = new HashMap<>();

    // Yarıçap seçenekleri
    private static final int[] RADIUS_OPTIONS = {10, 20, 30, 50};

    public CommandTntBlock(Settings settings) { super(settings); }

    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world,
                                         BlockPos pos, PlayerEntity player, Hand hand,
                                         BlockHitResult hit) {
        // Çakmak ve ateş topu → ateşle (CustomTntBlock davranışı)
        if (stack.isOf(net.minecraft.item.Items.FLINT_AND_STEEL) || stack.isOf(net.minecraft.item.Items.FIRE_CHARGE)) {
            return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
        }

        // Blok item'i → hedef blok ayarla
        if (stack.getItem() instanceof BlockItem blockItem) {
            if (!world.isClient()) {
                Block targetBlock = blockItem.getBlock();
                TARGET_BLOCKS.put(pos.toImmutable(), targetBlock);
                Identifier blockId = Registries.BLOCK.getId(targetBlock);
                int radius = RADII.getOrDefault(pos, 30);
                player.sendMessage(Text.literal("§a✔ Hedef: §f" + blockId.getPath()
                        + " §7| Yarıçap: " + radius), true);
            }
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos,
                                  PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) return ActionResult.SUCCESS;

        if (player.isSneaking()) {
            // Eğilerek sağ tıkla → yarıçap değiştir
            int currentRadius = RADII.getOrDefault(pos, 30);
            int nextRadius = RADIUS_OPTIONS[0];
            for (int i = 0; i < RADIUS_OPTIONS.length; i++) {
                if (RADIUS_OPTIONS[i] == currentRadius) {
                    nextRadius = RADIUS_OPTIONS[(i + 1) % RADIUS_OPTIONS.length];
                    break;
                }
            }
            RADII.put(pos.toImmutable(), nextRadius);

            Block target = TARGET_BLOCKS.getOrDefault(pos, Blocks.STONE);
            Identifier blockId = Registries.BLOCK.getId(target);
            player.sendMessage(Text.literal("§e⟳ Yarıçap: §f" + nextRadius
                    + " §7| Hedef: " + blockId.getPath()), true);
        } else {
            // Boş elle sağ tıkla → ayarları göster
            Block target = TARGET_BLOCKS.getOrDefault(pos, Blocks.STONE);
            Identifier blockId = Registries.BLOCK.getId(target);
            int radius = RADII.getOrDefault(pos, 30);
            player.sendMessage(Text.literal("§6Hedef: §f" + blockId.getPath()
                    + " §6| Yarıçap: §f" + radius
                    + " §7(Blok tut→hedef ayarla, Eğil+tıkla→yarıçap)"), true);
        }

        return ActionResult.SUCCESS;
    }

    @Override
    protected void spawnEntity(World world, double x, double y, double z,
                               @Nullable LivingEntity igniter) {
        BlockPos pos = new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
        Block targetBlock = TARGET_BLOCKS.getOrDefault(pos, Blocks.STONE);
        int radius = RADII.getOrDefault(pos, 30);

        CommandTntEntity entity = new CommandTntEntity(world, x, y, z, igniter);
        entity.setTargetBlock(targetBlock);
        entity.setRadius(radius);
        world.spawnEntity(entity);

        // Ayarları temizle (kullanıldı)
        TARGET_BLOCKS.remove(pos);
        RADII.remove(pos);
    }

    public static void clearAll() {
        TARGET_BLOCKS.clear();
        RADII.clear();
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        TARGET_BLOCKS.remove(pos);
        RADII.remove(pos);
        return super.onBreak(world, pos, state, player);
    }
}
