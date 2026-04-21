package com.supertntmod.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * End Kapısı: içinden geçen oyuncuyu End boyutuna ışınlar.
 * Portal silahından bağımsız olarak yerleştirilebilir.
 */
public class EndGateBlock extends Block {
    private static final int COOLDOWN = 60;
    private static final Map<UUID, Long> COOLDOWNS = new ConcurrentHashMap<>();

    public static void clearCooldowns() {
        COOLDOWNS.clear();
    }

    public EndGateBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.fullCube();
    }

    @Override
    protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity,
                                     EntityCollisionHandler handler, boolean pushable) {
        if (world.isClient() || !(entity instanceof LivingEntity)) return;
        if (!(world instanceof ServerWorld serverWorld)) return;

        long now = world.getTime();
        Long last = COOLDOWNS.get(entity.getUuid());
        if (last != null && now - last < COOLDOWN) return;
        COOLDOWNS.entrySet().removeIf(e -> now - e.getValue() > COOLDOWN);
        COOLDOWNS.put(entity.getUuid(), now);

        ServerWorld endWorld = serverWorld.getServer().getWorld(net.minecraft.world.World.END);
        if (endWorld == null) return;

        world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.BLOCKS, 1.0f, 1.0f);

        entity.teleport(endWorld, 100.5, 49.0, 0.5, Set.of(),
                entity.getYaw(), entity.getPitch(), false);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        for (int i = 0; i < 3; i++) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            world.addParticleClient(ParticleTypes.PORTAL, x, y, z, 0, 0, 0);
        }
    }
}
