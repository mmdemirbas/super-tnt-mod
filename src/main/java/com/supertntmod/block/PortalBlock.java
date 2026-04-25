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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Portal bloğu: Pembe ve yeşil olmak üzere iki renk.
 * Oyuncu bir portala girince diğer renkteki eşleşen portala ışınlanır.
 * Her oyuncunun kendi portal çifti vardır.
 *
 * PORTAL_COLOR: true = pembe, false = yeşil
 */
public class PortalBlock extends Block {
    public static final BooleanProperty IS_PINK = BooleanProperty.of("is_pink");

    // Işınlanma sonrası bekleme süresi (tick)
    private static final int TELEPORT_COOLDOWN = 40;
    private static final Map<UUID, Long> TELEPORT_COOLDOWNS = new ConcurrentHashMap<>();

    public static void clearCooldowns() {
        TELEPORT_COOLDOWNS.clear();
    }

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
        if (!(world instanceof ServerWorld serverWorld)) return;
        PortalPersistentState state = PortalPersistentState.get(serverWorld);

        // Eski portalı kaldır
        Map<UUID, BlockPos> portals = isPink ? state.pinkPortals : state.greenPortals;
        BlockPos oldPos = portals.get(ownerUuid);
        if (oldPos != null && world.getBlockState(oldPos).getBlock() instanceof PortalBlock) {
            world.removeBlock(oldPos, false);
            state.portalOwners.remove(oldPos);
        }

        // Yeni portalı yerleştir
        world.setBlockState(pos, ModBlocks.PORTAL_BLOCK.getDefaultState().with(IS_PINK, isPink));
        portals.put(ownerUuid, pos.toImmutable());
        state.portalOwners.put(pos.toImmutable(), ownerUuid);
        state.markDirty();

        // Ses efekti
        world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.BLOCKS, 0.5f, isPink ? 1.5f : 0.8f);
    }

    @Override
    protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity,
                                     EntityCollisionHandler handler, boolean pushable) {
        if (world.isClient() || !(entity instanceof LivingEntity)) return;
        if (!(world instanceof ServerWorld serverWorld)) return;
        PortalPersistentState portalState = PortalPersistentState.get(serverWorld);

        UUID ownerUuid = portalState.portalOwners.get(pos.toImmutable());
        if (ownerUuid == null) return;

        // Bekleme süresi kontrolü
        long currentTick = world.getTime();
        Long lastTeleport = TELEPORT_COOLDOWNS.get(entity.getUuid());
        if (lastTeleport != null && currentTick - lastTeleport < TELEPORT_COOLDOWN) return;

        boolean isPink = state.get(IS_PINK);
        Map<UUID, BlockPos> targetPortals = isPink ? portalState.greenPortals : portalState.pinkPortals;
        BlockPos targetPos = targetPortals.get(ownerUuid);

        if (targetPos == null) return;

        // Hedef portalın hala var olduğunu kontrol et
        if (!(world.getBlockState(targetPos).getBlock() instanceof PortalBlock)) {
            targetPortals.remove(ownerUuid);
            portalState.portalOwners.remove(targetPos);
            portalState.markDirty();
            return;
        }

        // Işınla — duvar/blok içine düşmemek için güvenli Y bul
        BlockPos safeDest = findSafeTeleportSpot(world, targetPos);
        entity.teleport((ServerWorld) world,
                safeDest.getX() + 0.5, safeDest.getY(), safeDest.getZ() + 0.5,
                java.util.Set.of(), entity.getYaw(), entity.getPitch(), false);

        TELEPORT_COOLDOWNS.entrySet().removeIf(e -> currentTick - e.getValue() > TELEPORT_COOLDOWN);
        TELEPORT_COOLDOWNS.put(entity.getUuid(), currentTick);

        world.playSound(null, targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5,
                SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    /**
     * Hedef portaldan başlayıp yukarı doğru iki üst üste hava bloğu bulur.
     * Bulunamazsa hedef portalın 1 üstü (mevcut davranış) döner.
     */
    private static BlockPos findSafeTeleportSpot(World world, BlockPos portalPos) {
        for (int dy = 1; dy <= 4; dy++) {
            BlockPos candidate = portalPos.up(dy);
            BlockState s1 = world.getBlockState(candidate);
            BlockState s2 = world.getBlockState(candidate.up());
            // Hava + üstte hava + alttaki blok (portal veya solid) — güvenli ayak basma noktası
            if ((s1.isAir() || s1.getBlock() instanceof PortalBlock)
                    && (s2.isAir() || s2.getBlock() instanceof PortalBlock)) {
                return candidate;
            }
        }
        return portalPos.up();
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, net.minecraft.entity.player.PlayerEntity player) {
        clearOwner(world, pos);
        return super.onBreak(world, pos, state, player);
    }

    @Override
    public void onDestroyedByExplosion(ServerWorld world, BlockPos pos,
            net.minecraft.world.explosion.Explosion explosion) {
        // Patlama onBreak'i atlar; portal-sahip eşlemesi yetim kalmasın.
        // Bu noktada blok zaten hava olduğu için BlockState'e güvenemeyiz —
        // owner UUID üzerinden ters arama ile pinkPortals/greenPortals'dan
        // doğru olanı kaldırıyoruz.
        clearOwner(world, pos);
        super.onDestroyedByExplosion(world, pos, explosion);
    }

    private static void clearOwner(World world, BlockPos pos) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        PortalPersistentState portalState = PortalPersistentState.get(serverWorld);
        BlockPos key = pos.toImmutable();
        UUID ownerUuid = portalState.portalOwners.remove(key);
        if (ownerUuid == null) return;
        // Hangi renk olduğunu bilmediğimiz için her iki haritayı da kontrol et.
        if (key.equals(portalState.pinkPortals.get(ownerUuid))) {
            portalState.pinkPortals.remove(ownerUuid);
        }
        if (key.equals(portalState.greenPortals.get(ownerUuid))) {
            portalState.greenPortals.remove(ownerUuid);
        }
        portalState.markDirty();
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
