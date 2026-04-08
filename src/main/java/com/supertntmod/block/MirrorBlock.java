package com.supertntmod.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * Ayna Bloğu: Koyulduğunda parlar (luminance 15).
 * Her saniye çevresindeki enderman'ları kontrol eder:
 * aynaya bakan enderman kendine hasar alır.
 */
public class MirrorBlock extends Block {

    private static final int ENDERMAN_RADIUS = 16;
    private static final float LOOK_THRESHOLD = 0.95f; // ~18 derece koni

    public MirrorBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        if (!world.isClient()) {
            world.scheduleBlockTick(pos, this, 20);
        }
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        Vec3d blockCenter = Vec3d.ofCenter(pos);

        world.getEntitiesByClass(
                EndermanEntity.class,
                new net.minecraft.util.math.Box(pos).expand(ENDERMAN_RADIUS),
                e -> true
        ).forEach(enderman -> {
            Vec3d eyePos = enderman.getEyePos();
            Vec3d toBlock = blockCenter.subtract(eyePos).normalize();
            Vec3d lookVec = enderman.getRotationVec(1.0f);

            if (lookVec.dotProduct(toBlock) > LOOK_THRESHOLD) {
                // Enderman aynaya bakıyor: kendine zarar ver.
                // Enderman'lar hasar aldığında vanilla davranışıyla otomatik ışınlanır.
                enderman.damage(world, world.getDamageSources().magic(), 4.0f);
            }
        });

        // 1 saniye sonra tekrar tetikle
        world.scheduleBlockTick(pos, this, 20);
    }
}
