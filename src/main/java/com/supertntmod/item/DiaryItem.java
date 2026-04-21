package com.supertntmod.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Günlük: sadece sahibi okuyabilir/yazabilir.
 * Sağ tık: içeriği göster (yalnızca sahibine) veya sahiplen.
 * Shift + sağ tık: yazma moduna gir.
 */
public class DiaryItem extends Item {
    private static final Map<UUID, Boolean> WRITE_MODE = new ConcurrentHashMap<>();

    public DiaryItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient()) return ActionResult.SUCCESS;
        ItemStack stack = user.getStackInHand(hand);
        NbtCompound data = getData(stack);

        String ownerStr = getStr(data, "owner");
        UUID playerId = user.getUuid();

        if (ownerStr.isEmpty()) {
            // Sahiplen
            data.putString("owner", playerId.toString());
            data.putString("content", "");
            setData(stack, data);
            user.sendMessage(Text.literal("Günlük sana ait oldu. Yazmak için eğil ve sağ tıkla.")
                    .formatted(Formatting.GREEN), true);
            return ActionResult.SUCCESS;
        }

        if (!ownerStr.equals(playerId.toString())) {
            user.sendMessage(Text.literal("Bu günlük boş.").formatted(Formatting.GRAY), true);
            return ActionResult.SUCCESS;
        }

        if (user.isSneaking()) {
            WRITE_MODE.put(playerId, true);
            user.sendMessage(Text.literal("Günlüğe yazmak istediğini chat'e yaz...")
                    .formatted(Formatting.YELLOW), false);
        } else {
            String content = getStr(data, "content");
            if (content.isEmpty()) {
                user.sendMessage(Text.literal("(Günlük boş)")
                        .formatted(Formatting.ITALIC, Formatting.GRAY), false);
            } else {
                user.sendMessage(Text.literal("--- Günlük ---").formatted(Formatting.GOLD), false);
                user.sendMessage(Text.literal(content).formatted(Formatting.WHITE), false);
            }
        }
        return ActionResult.SUCCESS;
    }

    public static boolean handleChatMessage(ServerPlayerEntity player, String message) {
        if (!WRITE_MODE.containsKey(player.getUuid())) return false;
        WRITE_MODE.remove(player.getUuid());

        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isOf(ModItems.DIARY)) continue;
            NbtCompound data = getData(stack);
            if (player.getUuidAsString().equals(getStr(data, "owner"))) {
                data.putString("content", message);
                setData(stack, data);
                player.sendMessage(Text.literal("Günlük yazıldı.").formatted(Formatting.GREEN), true);
                return true;
            }
        }
        return false;
    }

    public static void clearWriteMode(UUID playerId) {
        WRITE_MODE.remove(playerId);
    }

    public static void clearAll() {
        WRITE_MODE.clear();
    }

    private static String getStr(NbtCompound nbt, String key) {
        return nbt.getString(key).orElse("");
    }

    private static NbtCompound getData(ItemStack stack) {
        NbtComponent comp = stack.get(DataComponentTypes.CUSTOM_DATA);
        return comp != null ? comp.copyNbt() : new NbtCompound();
    }

    private static void setData(ItemStack stack, NbtCompound nbt) {
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }
}
