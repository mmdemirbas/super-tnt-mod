package com.supertntmod.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

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

    // Sandığın sahibi ve şifresini takip eden map'ler
    private static final Map<BlockPos, UUID> OWNERS = new HashMap<>();
    private static final Map<BlockPos, String> PASSWORDS = new HashMap<>();
    private static final Map<BlockPos, SimpleInventory> INVENTORIES = new HashMap<>();

    // Şifre girişi bekleyen oyuncular
    public static final Map<UUID, BlockPos> AWAITING_PASSWORD = new HashMap<>();
    public static final Map<UUID, Boolean> AWAITING_SET_PASSWORD = new HashMap<>();

    public EncryptedTntChestBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
        tooltip.add(Text.translatable("block.supertntmod.encrypted_tnt_chest.tooltip").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("block.supertntmod.encrypted_tnt_chest.tooltip2").formatted(Formatting.RED));
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos,
                                  PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) return ActionResult.SUCCESS;

        if (!OWNERS.containsKey(pos)) {
            // İlk kullanım: sahibi ol ve şifre belirle
            OWNERS.put(pos, player.getUuid());
            INVENTORIES.put(pos, new SimpleInventory(27));
            AWAITING_PASSWORD.put(player.getUuid(), pos.toImmutable());
            AWAITING_SET_PASSWORD.put(player.getUuid(), true);
            player.sendMessage(Text.literal("§aBu sandığın sahibi oldunuz! Chat'e şifrenizi yazın:"), false);
            return ActionResult.SUCCESS;
        }

        UUID ownerUuid = OWNERS.get(pos);

        if (player.getUuid().equals(ownerUuid)) {
            if (!PASSWORDS.containsKey(pos)) {
                // Henüz şifre belirlenmemiş
                AWAITING_PASSWORD.put(player.getUuid(), pos.toImmutable());
                AWAITING_SET_PASSWORD.put(player.getUuid(), true);
                player.sendMessage(Text.literal("§eChat'e şifrenizi yazın:"), false);
            } else {
                // Sahibi: şifre sor
                AWAITING_PASSWORD.put(player.getUuid(), pos.toImmutable());
                AWAITING_SET_PASSWORD.put(player.getUuid(), false);
                player.sendMessage(Text.literal("§eŞifreyi girin:"), false);
            }
        } else {
            // Başkası: hemen patla!
            if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                player.damage(serverWorld, world.getDamageSources().explosion(null, null), 30.0f);
            }
            world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 1.0f, 1.0f);
            world.createExplosion(null, pos.getX() + 0.5, pos.getY() + 0.5,
                    pos.getZ() + 0.5, 0.0f, false, World.ExplosionSourceType.NONE);
            player.sendMessage(Text.literal("§cBu sandık sana ait değil! BOOM!"), true);
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

        BlockPos pos = AWAITING_PASSWORD.remove(uuid);
        boolean isSettingPassword = AWAITING_SET_PASSWORD.remove(uuid);

        if (isSettingPassword) {
            PASSWORDS.put(pos, message);
            player.sendMessage(Text.literal("§aŞifre belirlendi! Sandığınız korunuyor."), false);
            return true;
        }

        // Şifre doğrulama (sadece sahip buraya ulaşır)
        String correctPassword = PASSWORDS.get(pos);
        if (correctPassword != null && correctPassword.equals(message)) {
            // Doğru şifre - sandığı aç
            SimpleInventory inventory = INVENTORIES.get(pos);
            if (inventory != null) {
                player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                        (syncId, playerInventory, p) -> GenericContainerScreenHandler.createGeneric9x3(syncId, playerInventory, inventory),
                        Text.literal("Şifreli TNT Sandık")
                ));
            }
            player.sendMessage(Text.literal("§aŞifre doğru! Sandık açılıyor."), true);
        } else {
            player.sendMessage(Text.literal("§cYanlış şifre!"), true);
        }
        return true;
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        OWNERS.remove(pos);
        PASSWORDS.remove(pos);
        INVENTORIES.remove(pos);
        return super.onBreak(world, pos, state, player);
    }
}
