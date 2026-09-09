package com.supertntmod.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 🔵 Ender Ateşi — Ender Çakmağı ile yakılan mavi ateş.
 *
 * İçine giren canlı üç hedeften birine ışınlanır. Ağırlıklar çocukların
 * tarifinden geliyor (90 / 50 / 1): en sık başka bir Ender Ateşi, ara sıra
 * Nether'daki bir ruh ateşi, çok seyrek Nether'daki turuncu ateş. Sayılar
 * yüzde değil ağırlık; normalize edilmiş halleri yaklaşık %64 / %35 / %0,7.
 *
 * Seçilen hedef türünde uygun yer bulunamazsa o seçenek elenir ve kalanlar
 * arasında yeniden çekiliş yapılır; hiçbiri tutmazsa oyuncuya bilgi verilir.
 *
 * Ateş kendiliğinden sönmez — yumrukla kırılır.
 */
public class EnderFireBlock extends Block {

    /** Işınlanma sonrası bekleme (tick). Hedefteki ateş anında geri ışınlamasın diye. */
    private static final int TELEPORT_COOLDOWN = 60;

    private static final int WEIGHT_ENDER_FIRE = 90;
    private static final int WEIGHT_SOUL_FIRE = 50;
    private static final int WEIGHT_NETHER_FIRE = 1;

    /** Nether taraması: yatay yarıçap ve Y aralığı. Tarama chunk üretebilir, dar tutuluyor. */
    private static final int NETHER_SEARCH_RADIUS = 12;
    private static final int NETHER_MIN_Y = 24;
    private static final int NETHER_MAX_Y = 100;
    private static final int MAX_NETHER_CANDIDATES = 64;

    /** Ender ateşi kayıtlarından en fazla kaç tanesi blok kontrolüyle doğrulanır. */
    private static final int MAX_VERIFIED_CANDIDATES = 16;

    private static final Map<UUID, Long> TELEPORT_COOLDOWNS = new ConcurrentHashMap<>();

    private record Target(ServerWorld world, BlockPos pos) {}

    private record Option(int weight, Supplier<Target> finder) {}

    public static void clearCooldowns() {
        TELEPORT_COOLDOWNS.clear();
    }

    public static void onPlayerDisconnect(UUID playerId) {
        TELEPORT_COOLDOWNS.remove(playerId);
    }

    public EnderFireBlock(Settings settings) {
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
    public void onBlockAdded(BlockState state, World world, BlockPos pos,
                             BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        if (world instanceof ServerWorld serverWorld) {
            EnderFirePersistentState.get(serverWorld).add(pos);
        }
    }

    @Override
    public void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        super.onStateReplaced(state, world, pos, moved);
        EnderFirePersistentState.get(world).remove(pos);
    }

    @Override
    protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity,
                                     EntityCollisionHandler handler, boolean pushable) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        if (!(entity instanceof LivingEntity)) return;

        long now = serverWorld.getTime();
        Long last = TELEPORT_COOLDOWNS.get(entity.getUuid());
        if (last != null && now - last < TELEPORT_COOLDOWN) return;

        // Bekleme süresi aramadan ÖNCE yazılıyor: hedef bulunamazsa da yazılmalı,
        // yoksa ateşin içinde duran oyuncu her tick Nether taraması tetikler.
        TELEPORT_COOLDOWNS.entrySet().removeIf(e -> now - e.getValue() > TELEPORT_COOLDOWN);
        TELEPORT_COOLDOWNS.put(entity.getUuid(), now);

        Target target = pickTarget(serverWorld, pos);
        if (target == null) {
            if (entity instanceof PlayerEntity player) {
                player.sendMessage(Text.translatable("item.supertntmod.ender_flint.no_target"), true);
            }
            return;
        }

        serverWorld.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.BLOCKS, 1.0f, 0.8f);

        entity.teleport(target.world(),
                target.pos().getX() + 0.5, target.pos().getY(), target.pos().getZ() + 0.5,
                java.util.Set.of(), entity.getYaw(), entity.getPitch(), false);

        target.world().playSound(null,
                target.pos().getX() + 0.5, target.pos().getY() + 0.5, target.pos().getZ() + 0.5,
                SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.BLOCKS, 1.0f, 1.2f);
        target.world().spawnParticles(ParticleTypes.PORTAL,
                target.pos().getX() + 0.5, target.pos().getY() + 0.5, target.pos().getZ() + 0.5,
                24, 0.4, 0.6, 0.4, 0.1);
    }

    /**
     * Ağırlıklı çekiliş; boş çıkan seçenek elenip kalanlarla yeniden çekilir.
     */
    private static Target pickTarget(ServerWorld sourceWorld, BlockPos sourcePos) {
        MinecraftServer server = sourceWorld.getServer();
        if (server == null) return null;

        List<Option> options = new ArrayList<>(List.of(
                new Option(WEIGHT_ENDER_FIRE, () -> findEnderFire(server, sourceWorld, sourcePos)),
                new Option(WEIGHT_SOUL_FIRE, () -> findNetherFire(server, sourceWorld, sourcePos, Blocks.SOUL_FIRE)),
                new Option(WEIGHT_NETHER_FIRE, () -> findNetherFire(server, sourceWorld, sourcePos, Blocks.FIRE))
        ));

        Random random = sourceWorld.getRandom();
        while (!options.isEmpty()) {
            int total = 0;
            for (Option option : options) total += option.weight();
            int roll = random.nextInt(total);
            int index = 0;
            for (; index < options.size() - 1; index++) {
                roll -= options.get(index).weight();
                if (roll < 0) break;
            }
            Target target = options.remove(index).finder().get();
            if (target != null) return target;
        }
        return null;
    }

    private static Target findEnderFire(MinecraftServer server, ServerWorld sourceWorld, BlockPos sourcePos) {
        List<Target> candidates = new ArrayList<>();
        for (ServerWorld world : server.getWorlds()) {
            for (BlockPos pos : EnderFirePersistentState.get(world).snapshot()) {
                if (world == sourceWorld && pos.equals(sourcePos)) continue;
                candidates.add(new Target(world, pos));
            }
        }
        Collections.shuffle(candidates);

        int checked = 0;
        for (Target candidate : candidates) {
            if (checked++ >= MAX_VERIFIED_CANDIDATES) break;
            if (candidate.world().getBlockState(candidate.pos()).isOf(ModBlocks.ENDER_FIRE)) {
                return candidate;
            }
            // Kayıt var, blok yok: patlama/akış gibi bir yolla kaybolmuş, kaydı temizle.
            EnderFirePersistentState.get(candidate.world()).remove(candidate.pos());
        }
        return null;
    }

    /**
     * Nether'da verilen ateş bloğundan birini arar. Overworld'den geliniyorsa
     * koordinatlar vanilla portal ölçeğiyle (1/8) eşlenir.
     */
    private static Target findNetherFire(MinecraftServer server, ServerWorld sourceWorld,
                                         BlockPos sourcePos, Block fireBlock) {
        ServerWorld nether = server.getWorld(World.NETHER);
        if (nether == null) return null;

        double scale = sourceWorld.getRegistryKey() == World.OVERWORLD ? 0.125 : 1.0;
        int centerX = (int) Math.floor(sourcePos.getX() * scale);
        int centerZ = (int) Math.floor(sourcePos.getZ() * scale);

        List<BlockPos> found = new ArrayList<>();
        BlockPos.Mutable cursor = new BlockPos.Mutable();
        for (int dx = -NETHER_SEARCH_RADIUS; dx <= NETHER_SEARCH_RADIUS; dx++) {
            for (int dz = -NETHER_SEARCH_RADIUS; dz <= NETHER_SEARCH_RADIUS; dz++) {
                for (int y = NETHER_MIN_Y; y <= NETHER_MAX_Y; y++) {
                    cursor.set(centerX + dx, y, centerZ + dz);
                    if (nether.getBlockState(cursor).isOf(fireBlock)) {
                        found.add(cursor.toImmutable());
                        if (found.size() >= MAX_NETHER_CANDIDATES) {
                            return new Target(nether, found.get(nether.getRandom().nextInt(found.size())));
                        }
                    }
                }
            }
        }
        if (found.isEmpty()) return null;
        return new Target(nether, found.get(nether.getRandom().nextInt(found.size())));
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (random.nextInt(6) == 0) {
            world.playSoundClient(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.BLOCK_PORTAL_AMBIENT, SoundCategory.BLOCKS,
                    0.3f, random.nextFloat() * 0.4f + 0.8f, false);
        }
        for (int i = 0; i < 4; i++) {
            world.addParticleClient(ParticleTypes.PORTAL,
                    pos.getX() + random.nextDouble(),
                    pos.getY() + random.nextDouble() * 0.6,
                    pos.getZ() + random.nextDouble(),
                    0, 0.04, 0);
        }
    }
}
