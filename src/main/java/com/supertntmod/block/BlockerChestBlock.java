package com.supertntmod.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Blocker Sandık: Sadece sahibi açabilir.
 * Başkası açarsa anında ölür ve 1 dakika boyunca hiçbir sandık açamaz.
 */
public class BlockerChestBlock extends Block {
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Block.createCuboidShape(1, 0, 1, 15, 14, 15);

    // 1 dakika sandık yasağı takibi (UUID → yasak bitiş zamanı ms)
    private static final Map<UUID, Long> CHEST_BAN = new ConcurrentHashMap<>();
    private static final long BAN_DURATION_MS = 60_000;

    public BlockerChestBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected boolean isTransparent(BlockState state) {
        return true;
    }

    public static boolean isChestBanned(UUID playerUuid) {
        Long until = CHEST_BAN.get(playerUuid);
        if (until == null) return false;
        if (System.currentTimeMillis() > until) {
            CHEST_BAN.remove(playerUuid);
            return false;
        }
        return true;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos,
                                  PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) return ActionResult.SUCCESS;
        if (!(world instanceof ServerWorld serverWorld)) return ActionResult.PASS;

        // Sandık yasağı kontrolü
        if (isChestBanned(player.getUuid())) {
            player.sendMessage(Text.translatable("message.supertntmod.blocker_chest.banned"), true);
            return ActionResult.SUCCESS;
        }

        BlockerChestPersistentState chestState = BlockerChestPersistentState.get(serverWorld);

        if (!chestState.owners.containsKey(pos)) {
            // İlk kullanım: sahibi ol
            chestState.owners.put(pos, player.getUuid());
            SimpleInventory inv = new SimpleInventory(27);
            inv.addListener(sender -> chestState.markDirty());
            chestState.inventories.put(pos, inv);
            chestState.markDirty();
            player.sendMessage(Text.translatable("message.supertntmod.blocker_chest.owner_set"), false);
            openInventory(player, chestState.inventories.get(pos));
            return ActionResult.SUCCESS;
        }

        UUID ownerUuid = chestState.owners.get(pos);

        if (player.getUuid().equals(ownerUuid)) {
            // Sahip: direkt aç
            openInventory(player, chestState.inventories.get(pos));
        } else {
            // Başkası: anında öldür + 1 dk sandık yasağı
            if (player instanceof ServerPlayerEntity sp) {
                sp.kill(serverWorld);
            }
            // Sandık yasağı uygula
            CHEST_BAN.put(player.getUuid(), System.currentTimeMillis() + BAN_DURATION_MS);

            // Efektler
            world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 1.0f, 1.0f);
            player.sendMessage(Text.translatable("message.supertntmod.blocker_chest.killed"), true);
        }

        return ActionResult.SUCCESS;
    }

    private void openInventory(PlayerEntity player, SimpleInventory inventory) {
        if (inventory == null) return;
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInventory, p) ->
                        GenericContainerScreenHandler.createGeneric9x3(syncId, playerInventory, inventory),
                Text.translatable("message.supertntmod.blocker_chest.title")
        ));
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        clearAndDrop(world, pos);
        return super.onBreak(world, pos, state, player);
    }

    @Override
    public void onDestroyedByExplosion(ServerWorld world, BlockPos pos,
            net.minecraft.world.explosion.Explosion explosion) {
        // Patlama ile yıkıldığında onBreak çalışmaz; envanter ve sahip UUID
        // map'te yetim kalır. Burada da temizle ve eşyaları yere düşür.
        clearAndDrop(world, pos);
        super.onDestroyedByExplosion(world, pos, explosion);
    }

    private static void clearAndDrop(World world, BlockPos pos) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        BlockerChestPersistentState chestState = BlockerChestPersistentState.get(serverWorld);
        SimpleInventory inv = chestState.inventories.remove(pos);
        if (inv != null) {
            for (int i = 0; i < inv.size(); i++) {
                net.minecraft.item.ItemStack stack = inv.getStack(i);
                if (!stack.isEmpty()) {
                    Block.dropStack(world, pos, stack);
                }
            }
        }
        chestState.owners.remove(pos);
        chestState.markDirty();
    }

    public static void clearBans() {
        CHEST_BAN.clear();
    }

    public static void onPlayerDisconnect(UUID uuid) {
        CHEST_BAN.remove(uuid);
    }
}
