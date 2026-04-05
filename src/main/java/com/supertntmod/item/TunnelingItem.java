package com.supertntmod.item;

import com.supertntmod.block.ModBlocks;
import com.supertntmod.block.TunneledBlock;
import com.supertntmod.block.TunneledBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.function.Consumer;

/**
 * Tünelleme aleti: 1/12 ölçeğe küçülünce otomatik olarak verilen özel item.
 * Bloklara sağ tıklayarak 4×4×4 sub-voxel grid üzerinden minik delikler açar.
 * Blok tamamen kırılmaz, sadece tıklanan sub-voxel kaldırılır.
 */
public class TunnelingItem extends Item {
    public static final double SCALE_THRESHOLD = 1.0 / 12.0;

    public TunnelingItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context,
                              TooltipDisplayComponent displayComponent,
                              Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
        textConsumer.accept(Text.translatable("item.supertntmod.tunneling_item.tooltip").formatted(Formatting.GRAY));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        PlayerEntity player = context.getPlayer();
        BlockPos pos = context.getBlockPos();

        if (world.isClient() || player == null) return ActionResult.PASS;

        // Ölçek kontrolü: sadece 1/12 veya daha küçükken çalışır
        EntityAttributeInstance scaleAttr = player.getAttributeInstance(EntityAttributes.SCALE);
        if (scaleAttr == null || scaleAttr.getValue() > SCALE_THRESHOLD) {
            player.sendMessage(Text.translatable("item.supertntmod.tunneling_item.too_large"), true);
            return ActionResult.FAIL;
        }

        BlockState targetState = world.getBlockState(pos);

        // Hava, sıvı ve BlockEntity'li bloklar hariç (sandık, fırın vb.)
        if (targetState.isAir() || !targetState.getFluidState().isEmpty()) {
            return ActionResult.PASS;
        }

        // Tıklanan noktayı sub-voxel koordinatına çevir
        Vec3d hitPos = context.getHitPos();
        double localX = hitPos.x - pos.getX();
        double localY = hitPos.y - pos.getY();
        double localZ = hitPos.z - pos.getZ();
        int subX = Math.min(3, Math.max(0, (int) (localX * 4)));
        int subY = Math.min(3, Math.max(0, (int) (localY * 4)));
        int subZ = Math.min(3, Math.max(0, (int) (localZ * 4)));

        // Delme boyutunu oyuncu boyutuna göre hesapla
        // Bir delme işlemi = yarı oyuncu yüksekliği, iki delme = tam beden deliği
        int drillH = Math.max(1, (int) Math.round(player.getHeight() / 2.0 / 0.25));
        int drillW = Math.max(1, (int) Math.round(player.getWidth() / 0.25));

        // Delme yönüne göre hangi eksenlerde genişleyeceğini belirle
        Direction face = context.getSide();
        int[][] offsets = computeDrillOffsets(face, subX, subY, subZ, drillW, drillH);

        // Sadece dolu sub-voxel'leri filtrele
        int[][] filledOffsets = java.util.Arrays.stream(offsets).filter(off -> {
            if (targetState.getBlock() instanceof TunneledBlock) {
                if (world.getBlockEntity(pos) instanceof TunneledBlockEntity entity) {
                    return entity.isSubVoxelFilled(off[0], off[1], off[2]);
                }
                return false;
            }
            return true; // Normal blok: tüm sub-voxel'ler dolu
        }).toArray(int[][]::new);

        if (filledOffsets.length == 0) {
            return ActionResult.PASS;
        }

        if (targetState.getBlock() instanceof TunneledBlock) {
            // Zaten tünellenmiş blok: sub-voxel kümesini toplu kaldır
            if (world.getBlockEntity(pos) instanceof TunneledBlockEntity entity) {
                entity.removeSubVoxels(filledOffsets);
                if (entity.getSubVoxels() == 0) {
                    world.removeBlock(pos, false);
                }
                playDrillEffect(world, pos, subX, subY, subZ);
                return ActionResult.SUCCESS;
            }
        } else {
            // Normal blok: TunneledBlock'a dönüştür ve sub-voxel kümesini kaldır
            if (targetState.hasBlockEntity()) {
                return ActionResult.PASS;
            }

            var originalId = Registries.BLOCK.getId(targetState.getBlock());
            world.setBlockState(pos, ModBlocks.TUNNELED_BLOCK.getDefaultState());
            if (world.getBlockEntity(pos) instanceof TunneledBlockEntity entity) {
                entity.setOriginalBlockId(originalId);
                entity.removeSubVoxels(filledOffsets);
                if (entity.getSubVoxels() == 0) {
                    world.removeBlock(pos, false);
                }
            }
            playDrillEffect(world, pos, subX, subY, subZ);
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    /**
     * Delme alanını hesaplar: tıklanan yüzeye dik düzlemde genişler.
     * Üst/alt yüzey tıklandığında X-Z düzleminde, yan yüzey tıklandığında
     * Y ekseni dikey, kalan eksen yatay olarak kullanılır.
     */
    private int[][] computeDrillOffsets(Direction face, int subX, int subY, int subZ, int drillW, int drillH) {
        java.util.List<int[]> result = new java.util.ArrayList<>();

        if (face.getAxis() == Direction.Axis.Y) {
            // Üst/alt yüzey: tıklanan Y katmanında X-Z düzleminde genişle
            int halfW = drillW / 2;
            for (int dx = -halfW; dx < drillW - halfW; dx++) {
                for (int dz = -halfW; dz < drillW - halfW; dz++) {
                    int sx = subX + dx;
                    int sz = subZ + dz;
                    if (sx >= 0 && sx < 4 && sz >= 0 && sz < 4) {
                        result.add(new int[]{sx, subY, sz});
                    }
                }
            }
        } else {
            // Yan yüzey: sadece tıklanan derinlik katmanında Y×lateral genişle
            int halfW = drillW / 2;
            for (int dy = 0; dy < drillH; dy++) {
                for (int dw = -halfW; dw < drillW - halfW; dw++) {
                    int sy = subY + dy;
                    int sx, sz;
                    if (face.getAxis() == Direction.Axis.X) {
                        sx = subX;
                        sz = subZ + dw;
                    } else {
                        sx = subX + dw;
                        sz = subZ;
                    }
                    if (sx >= 0 && sx < 4 && sy >= 0 && sy < 4 && sz >= 0 && sz < 4) {
                        result.add(new int[]{sx, sy, sz});
                    }
                }
            }
        }

        if (result.isEmpty()) {
            result.add(new int[]{subX, subY, subZ});
        }
        return result.toArray(new int[0][]);
    }

    private void playDrillEffect(World world, BlockPos pos, int subX, int subY, int subZ) {
        double cx = pos.getX() + (subX + 0.5) * 0.25;
        double cy = pos.getY() + (subY + 0.5) * 0.25;
        double cz = pos.getZ() + (subZ + 0.5) * 0.25;

        world.playSound(null, cx, cy, cz,
                SoundEvents.BLOCK_GRAVEL_BREAK, SoundCategory.BLOCKS, 0.5f, 2.0f);

        if (world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.DUST_PLUME,
                    cx, cy, cz, 3, 0.05, 0.05, 0.05, 0.01);
        }
    }
}
