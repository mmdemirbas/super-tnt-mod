package com.supertntmod.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Portal bloğu: Pembe ve yeşil olmak üzere iki renk.
 * Oyuncu bir portala girince diğer renkteki eşleşen portala ışınlanır.
 * Her oyuncunun kendi portal çifti vardır.
 *
 * PORTAL_COLOR: true = pembe, false = yeşil
 */
public class PortalBlock extends Block {
    public static final BooleanProperty IS_PINK = BooleanProperty.of("is_pink");

    // Oyuncu başına portal pozisyonları
    private static final Map<UUID, BlockPos> PINK_PORTALS = new HashMap<>();
    private static final Map<UUID, BlockPos> GREEN_PORTALS = new HashMap<>();
    // Portal pozisyonu → sahip UUID
    private static final Map<BlockPos, UUID> PORTAL_OWNERS = new HashMap<>();

    // Işınlanma sonrası bekleme süresi (tick)
    private static final int TELEPORT_COOLDOWN = 40;
    private static final Map<UUID, Long> TELEPORT_COOLDOWNS = new HashMap<>();

    public PortalBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(IS_PINK, true));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(IS_PINK);
    }

    // Geçilebilir: collision yok
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.fullCube();
    }

    /**
     * Portal yerleştirildiğinde çağrılır. Eski aynı renk portalı kaldırır.
     */
    public static void placePortal(World world, BlockPos pos, boolean isPink, UUID ownerUuid) {
        // Eski portalı kaldır
        Map<UUID, BlockPos> portals = isPink ? PINK_PORTALS : GREEN_PORTALS;
        BlockPos oldPos = portals.get(ownerUuid);
        if (oldPos != null && world.getBlockState(oldPos).getBlock() instanceof PortalBlock) {
            world.removeBlock(oldPos, false);
            PORTAL_OWNERS.remove(oldPos);
        }

        // Yeni portalı yerleştir
        world.setBlockState(pos, ModBlocks.PORTAL_BLOCK.getDefaultState().with(IS_PINK, isPink));
        portals.put(ownerUuid, pos.toImmutable());
        PORTAL_OWNERS.put(pos.toImmutable(), ownerUuid);

        // Ses efekti
        world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.BLOCKS, 0.5f, isPink ? 1.5f : 0.8f);
    }

    @Override
    protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity,
                                     EntityCollisionHandler handler, boolean pushable) {
        if (world.isClient() || !(entity instanceof LivingEntity)) return;

        UUID ownerUuid = PORTAL_OWNERS.get(pos.toImmutable());
        if (ownerUuid == null) return;

        // Bekleme süresi kontrolü
        long currentTick = world.getTime();
        Long lastTeleport = TELEPORT_COOLDOWNS.get(entity.getUuid());
        if (lastTeleport != null && currentTick - lastTeleport < TELEPORT_COOLDOWN) return;

        boolean isPink = state.get(IS_PINK);
        Map<UUID, BlockPos> targetPortals = isPink ? GREEN_PORTALS : PINK_PORTALS;
        BlockPos targetPos = targetPortals.get(ownerUuid);

        if (targetPos == null) return;

        // Hedef portalın hala var olduğunu kontrol et
        if (!(world.getBlockState(targetPos).getBlock() instanceof PortalBlock)) {
            targetPortals.remove(ownerUuid);
            PORTAL_OWNERS.remove(targetPos);
            return;
        }

        // Işınla
        entity.teleport((ServerWorld) world,
                targetPos.getX() + 0.5, targetPos.getY() + 1.0, targetPos.getZ() + 0.5,
                java.util.Set.of(), entity.getYaw(), entity.getPitch(), false);

        TELEPORT_COOLDOWNS.put(entity.getUuid(), currentTick);

        world.playSound(null, targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5,
                SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, net.minecraft.entity.player.PlayerEntity player) {
        UUID ownerUuid = PORTAL_OWNERS.remove(pos.toImmutable());
        if (ownerUuid != null) {
            boolean isPink = state.get(IS_PINK);
            (isPink ? PINK_PORTALS : GREEN_PORTALS).remove(ownerUuid);
        }
        return super.onBreak(world, pos, state, player);
    }

    // Partikül efektleri
    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        boolean isPink = state.get(IS_PINK);
        // RGB renk: pembe = 0xFF66B2, yeşil = 0x33FF66
        int color = isPink ? 0xFF66B2 : 0x33FF66;
        DustParticleEffect particle = new DustParticleEffect(color, 1.0f);

        for (int i = 0; i < 4; i++) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            world.addParticleClient(particle, x, y, z, 0, 0.05, 0);
        }
    }
}
