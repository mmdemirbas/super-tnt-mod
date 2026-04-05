package com.supertntmod.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Şifreli TNT Sandık:
 * - İlk sağ tık: Şifre belirleme (sahip olur)
 * - Sahip doğru şifreyi girerse: açılır
 * - Başka biri girerse (doğru/yanlış fark etmez): patlar
 * - Etraftaki bloklara zarar vermez
 *
 * Şifre sistemi: Chat mesajı ile şifre girişi
 */
public class EncryptedTntChestBlock extends Block {
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;

    // Sandık şekli: 14×14 taban, 14 piksel yükseklik (vanilla sandık boyutu)
    private static final VoxelShape SHAPE = Block.createCuboidShape(1, 0, 1, 15, 14, 15);

    private record DimPos(RegistryKey<World> dimension, BlockPos pos) {}

    // Şifre girişi bekleyen oyuncular (ephemeral, no persistence needed)
    static final Map<UUID, DimPos> AWAITING_PASSWORD = new HashMap<>();
    public static final Map<UUID, Boolean> AWAITING_SET_PASSWORD = new HashMap<>();

    public EncryptedTntChestBlock(Settings settings) {
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

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos,
                                  PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) return ActionResult.SUCCESS;
        if (!(world instanceof net.minecraft.server.world.ServerWorld serverWorld)) return ActionResult.PASS;
        ChestPersistentState chestState = ChestPersistentState.get(serverWorld);

        if (!chestState.owners.containsKey(pos)) {
            // İlk kullanım: sahibi ol ve şifre belirle
            chestState.owners.put(pos, player.getUuid());
            SimpleInventory inv = new SimpleInventory(27);
            inv.addListener(sender -> chestState.markDirty());
            chestState.inventories.put(pos, inv);
            chestState.markDirty();
            AWAITING_PASSWORD.put(player.getUuid(), new DimPos(world.getRegistryKey(), pos.toImmutable()));
            AWAITING_SET_PASSWORD.put(player.getUuid(), true);
            player.sendMessage(Text.translatable("message.supertntmod.encrypted_tnt_chest.owner_set"), false);
            return ActionResult.SUCCESS;
        }

        UUID ownerUuid = chestState.owners.get(pos);

        if (player.getUuid().equals(ownerUuid)) {
            if (!chestState.passwords.containsKey(pos)) {
                // Henüz şifre belirlenmemiş
                AWAITING_PASSWORD.put(player.getUuid(), new DimPos(world.getRegistryKey(), pos.toImmutable()));
                AWAITING_SET_PASSWORD.put(player.getUuid(), true);
                player.sendMessage(Text.translatable("message.supertntmod.encrypted_tnt_chest.set_password"), false);
            } else {
                // Sahibi: şifre sor
                AWAITING_PASSWORD.put(player.getUuid(), new DimPos(world.getRegistryKey(), pos.toImmutable()));
                AWAITING_SET_PASSWORD.put(player.getUuid(), false);
                player.sendMessage(Text.translatable("message.supertntmod.encrypted_tnt_chest.enter_password"), false);
            }
        } else {
            // Başkası: hemen patla!
            player.damage(serverWorld, world.getDamageSources().explosion(null, null), 30.0f);
            world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 1.0f, 1.0f);
            world.createExplosion(null, pos.getX() + 0.5, pos.getY() + 0.5,
                    pos.getZ() + 0.5, 0.0f, false, World.ExplosionSourceType.NONE);
            player.sendMessage(Text.translatable("message.supertntmod.encrypted_tnt_chest.not_owner"), true);
        }

        return ActionResult.SUCCESS;
    }

    /**
     * Chat mesajıyla şifre girişini işler.
     * SuperTntMod'dan çağrılır.
     */
    public static boolean handleChatMessage(ServerPlayerEntity player, String message) {
        UUID uuid = player.getUuid();
        if (!AWAITING_PASSWORD.containsKey(uuid)) return false;

        DimPos dimPos = AWAITING_PASSWORD.remove(uuid);
        boolean isSettingPassword = AWAITING_SET_PASSWORD.remove(uuid);
        if (!(player.getEntityWorld() instanceof net.minecraft.server.world.ServerWorld currentWorld)) return false;
        net.minecraft.server.world.ServerWorld targetWorld = currentWorld.getServer().getWorld(dimPos.dimension());
        if (targetWorld == null) return false;
        BlockPos pos = dimPos.pos();
        ChestPersistentState chestState = ChestPersistentState.get(targetWorld);

        if (isSettingPassword) {
            chestState.passwords.put(pos, message);
            chestState.markDirty();
            player.sendMessage(Text.translatable("message.supertntmod.encrypted_tnt_chest.password_set"), false);
            return true;
        }

        // Şifre doğrulama (sadece sahip buraya ulaşır)
        String correctPassword = chestState.passwords.get(pos);
        if (correctPassword != null && correctPassword.equals(message)) {
            // Doğru şifre - sandığı aç
            SimpleInventory inventory = chestState.inventories.get(pos);
            if (inventory != null) {
                player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                        (syncId, playerInventory, p) -> GenericContainerScreenHandler.createGeneric9x3(syncId, playerInventory, inventory),
                        Text.translatable("message.supertntmod.encrypted_tnt_chest.title")
                ));
            }
            player.sendMessage(Text.translatable("message.supertntmod.encrypted_tnt_chest.password_correct"), true);
        } else {
            player.sendMessage(Text.translatable("message.supertntmod.encrypted_tnt_chest.password_wrong"), true);
        }
        return true;
    }

    public static void onPlayerDisconnect(UUID uuid) {
        AWAITING_PASSWORD.remove(uuid);
        AWAITING_SET_PASSWORD.remove(uuid);
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
            ChestPersistentState chestState = ChestPersistentState.get(serverWorld);
            chestState.owners.remove(pos);
            chestState.passwords.remove(pos);
            chestState.inventories.remove(pos);
            chestState.markDirty();
        }
        return super.onBreak(world, pos, state, player);
    }
}
