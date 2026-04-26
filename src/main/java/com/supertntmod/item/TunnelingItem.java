package com.supertntmod.item;

import com.supertntmod.block.ModBlocks;
import com.supertntmod.block.TunneledBlock;
import com.supertntmod.block.TunneledBlockEntity;
import net.minecraft.block.BlockState;
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
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.function.Consumer;

/**
 * Tünelleme aleti: 1/12 ölçeğe küçülünce otomatik olarak verilen özel item.
 * Sağ tıklayarak ışın yönündeki ilk dolu sub-voxel'i tek tek deler.
 *
 * Standart MC raycast'i sadece blok kenarını yakalar; tünel boyunca düz bakan
 * oyuncunun tıklaması "boşluğa" giderse useOnBlock hiç çağrılmaz. Bu yüzden
 * use() içinde özel sub-voxel raycast yapılır: ışın boyunca ilk dolu malzeme
 * (normal blok veya TunneledBlock'un dolu sub-voxel'i) aranıp delinir.
 */
public class TunnelingItem extends Item {
    public static final double SCALE_THRESHOLD = 1.0 / 12.0;
    private static final double REACH = 6.0;
    private static final double STEP = 0.05;

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

    /**
     * Standart blok tıklamaları için useOnBlock; ancak hiçbir şey yapmadan PASS
     * döner ki MC akışı use()'a düşsün ve oradaki birleşik sub-voxel raycast
     * çalışsın. Aksi halde tünelden geçen ışınlar useOnBlock'u tetiklemediği
     * için drill çalışmıyor görünüyordu.
     */
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        return ActionResult.PASS;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient()) return ActionResult.SUCCESS;

        EntityAttributeInstance scaleAttr = user.getAttributeInstance(EntityAttributes.SCALE);
        if (scaleAttr == null || scaleAttr.getValue() > SCALE_THRESHOLD) {
            user.sendMessage(Text.translatable("item.supertntmod.tunneling_item.too_large"), true);
            return ActionResult.FAIL;
        }

        Vec3d eye = user.getEyePos();
        Vec3d direction = user.getRotationVec(1.0f);
        Hit hit = subVoxelRaycast(world, eye, direction);
        if (hit == null) return ActionResult.PASS;

        drill(world, hit);
        return ActionResult.SUCCESS;
    }

    /**
     * Oyuncunun bakış ışını boyunca ilk dolu sub-voxel'i bulur.
     * STEP=0.05 (sub-voxel boyu 0.25'in 1/5'i) ile yeterli çözünürlük sağlar.
     */
    private Hit subVoxelRaycast(World world, Vec3d eye, Vec3d direction) {
        int maxSteps = (int) Math.ceil(REACH / STEP);
        BlockPos lastBlockPos = null;
        for (int i = 1; i <= maxSteps; i++) {
            Vec3d point = eye.add(direction.multiply(i * STEP));
            BlockPos blockPos = BlockPos.ofFloored(point);
            // Aynı block içinde tekrar tekrar BlockState/BlockEntity okumayı önlemek için
            // bir mikro-optimizasyon yapılabilirdi; ancak 6/0.05 = 120 adımda anlamlı değil.
            if (lastBlockPos != null && blockPos.equals(lastBlockPos)) {
                // continue with finer-grained sub-voxel check below
            }
            lastBlockPos = blockPos;

            BlockState state = world.getBlockState(blockPos);
            if (state.isAir()) continue;
            if (!state.getFluidState().isEmpty()) continue;

            double localX = point.x - blockPos.getX();
            double localY = point.y - blockPos.getY();
            double localZ = point.z - blockPos.getZ();
            int subX = clamp((int) (localX * 4));
            int subY = clamp((int) (localY * 4));
            int subZ = clamp((int) (localZ * 4));

            if (state.getBlock() instanceof TunneledBlock) {
                if (world.getBlockEntity(blockPos) instanceof TunneledBlockEntity entity) {
                    if (!entity.isSubVoxelFilled(subX, subY, subZ)) continue;
                    return new Hit(blockPos, subX, subY, subZ, true);
                }
                continue;
            }

            // Normal blok: BlockEntity'li blokları es geç (sandık, fırın vb.)
            if (state.hasBlockEntity()) continue;
            return new Hit(blockPos, subX, subY, subZ, false);
        }
        return null;
    }

    private void drill(World world, Hit hit) {
        if (hit.fromTunneled) {
            if (world.getBlockEntity(hit.pos) instanceof TunneledBlockEntity entity) {
                entity.removeSubVoxel(hit.subX, hit.subY, hit.subZ);
                if (entity.getSubVoxels() == 0) {
                    world.removeBlock(hit.pos, false);
                }
                playDrillEffect(world, hit.pos, hit.subX, hit.subY, hit.subZ);
            }
            return;
        }

        // Normal bloku TunneledBlock'a dönüştür ve hit sub-voxel'i kaldır
        BlockState original = world.getBlockState(hit.pos);
        Identifier originalId = Registries.BLOCK.getId(original.getBlock());
        world.setBlockState(hit.pos, ModBlocks.TUNNELED_BLOCK.getDefaultState());
        if (world.getBlockEntity(hit.pos) instanceof TunneledBlockEntity entity) {
            entity.setOriginalBlockId(originalId);
            entity.removeSubVoxel(hit.subX, hit.subY, hit.subZ);
            if (entity.getSubVoxels() == 0) {
                world.removeBlock(hit.pos, false);
            }
        }
        playDrillEffect(world, hit.pos, hit.subX, hit.subY, hit.subZ);
    }

    private static int clamp(int v) {
        return Math.min(3, Math.max(0, v));
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

    private record Hit(BlockPos pos, int subX, int subY, int subZ, boolean fromTunneled) {
    }
}
