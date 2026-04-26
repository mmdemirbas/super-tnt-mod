package com.supertntmod.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Tünellenmiş bloğun içindeki boş sub-voxel'lere blok yerleştirir.
 * Drill aracıyla açılan boşlukların doldurulması için kullanılır.
 */
public final class TunnelFillHandler {
    private TunnelFillHandler() {
    }

    public static ActionResult handle(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
        if (world.isClient()) return ActionResult.PASS;

        BlockPos pos = hitResult.getBlockPos();
        BlockState state = world.getBlockState(pos);

        if (!(state.getBlock() instanceof TunneledBlock)) return ActionResult.PASS;

        ItemStack stack = player.getStackInHand(hand);
        if (!(stack.getItem() instanceof BlockItem blockItem)) return ActionResult.PASS;

        // Tünellenmiş bloğu kendi içine yerleştirmeye izin verme (sonsuz iç içe)
        if (blockItem.getBlock() instanceof TunneledBlock) return ActionResult.PASS;
        // BlockEntity'li bloklar için iç sub-voxel yerleştirme desteklenmiyor (state lifecycle karmaşık)
        if (blockItem.getBlock() instanceof BlockWithEntity) return ActionResult.PASS;

        Vec3d hitPos = hitResult.getPos();
        Direction face = hitResult.getSide();

        double localX = hitPos.x - pos.getX();
        double localY = hitPos.y - pos.getY();
        double localZ = hitPos.z - pos.getZ();

        int subX = clamp((int) (localX * 4));
        int subY = clamp((int) (localY * 4));
        int subZ = clamp((int) (localZ * 4));

        // Yerleşim hedefi: tıklanan yüzeye dik 1 sub-voxel
        int tx = subX + face.getOffsetX();
        int ty = subY + face.getOffsetY();
        int tz = subZ + face.getOffsetZ();

        // Sub-voxel komşu blokun içine taşarsa, normal place akışına bırak
        if (tx < 0 || tx >= 4 || ty < 0 || ty >= 4 || tz < 0 || tz >= 4) {
            return ActionResult.PASS;
        }

        if (!(world.getBlockEntity(pos) instanceof TunneledBlockEntity entity)) {
            return ActionResult.PASS;
        }

        if (entity.isSubVoxelFilled(tx, ty, tz)) return ActionResult.PASS;

        // Oyuncunun boyutuna göre tek seferde kaç sub-voxel doldurulacağını belirle
        int count = computeFillCount(player);

        // count^3 küp halinde, sınırları taşmadan ve boş olan sub-voxel'leri doldur
        List<int[]> coords = collectFillCoords(entity, tx, ty, tz, face, count);
        if (coords.isEmpty()) return ActionResult.PASS;

        Identifier blockId = Registries.BLOCK.getId(blockItem.getBlock());
        entity.fillSubVoxels(coords.toArray(new int[0][]), blockId);

        if (!player.getAbilities().creativeMode) stack.decrement(1);

        BlockSoundGroup soundGroup = blockItem.getBlock().getDefaultState().getSoundGroup();
        world.playSound(null, pos, soundGroup.getPlaceSound(), SoundCategory.BLOCKS,
                soundGroup.getVolume() * 0.5f, soundGroup.getPitch());

        return ActionResult.SUCCESS;
    }

    private static int computeFillCount(PlayerEntity player) {
        var attr = player.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.SCALE);
        double scale = attr != null ? attr.getValue() : 1.0;
        int count = Math.max(1, (int) Math.ceil(scale * 4));
        return Math.min(4, count);
    }

    private static List<int[]> collectFillCoords(TunneledBlockEntity entity,
                                                  int originX, int originY, int originZ,
                                                  Direction face, int count) {
        // Face yönüne dik eksenlerde ortalanmış, face yönünde ise origin'den başlayan küp
        int axisX = face.getOffsetX();
        int axisY = face.getOffsetY();
        int axisZ = face.getOffsetZ();
        int half = count / 2;

        List<int[]> coords = new ArrayList<>();
        for (int d = 0; d < count; d++) {
            for (int u = -half; u < count - half; u++) {
                for (int v = -half; v < count - half; v++) {
                    int x = originX, y = originY, z = originZ;
                    if (face.getAxis() == Direction.Axis.X) {
                        x = originX + axisX * d;
                        y = originY + u;
                        z = originZ + v;
                    } else if (face.getAxis() == Direction.Axis.Y) {
                        x = originX + u;
                        y = originY + axisY * d;
                        z = originZ + v;
                    } else {
                        x = originX + u;
                        y = originY + v;
                        z = originZ + axisZ * d;
                    }
                    if (x < 0 || x >= 4 || y < 0 || y >= 4 || z < 0 || z >= 4) continue;
                    if (entity.isSubVoxelFilled(x, y, z)) continue;
                    coords.add(new int[]{x, y, z});
                }
            }
        }
        return coords;
    }

    private static int clamp(int v) {
        return Math.min(3, Math.max(0, v));
    }
}
