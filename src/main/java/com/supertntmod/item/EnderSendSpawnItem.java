package com.supertntmod.item;

import com.supertntmod.entity.EnderSendEntity;
import com.supertntmod.entity.ModEntities;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.function.Consumer;

/**
 * Ender Send spawn yumurtası: Bloğa sağ tıklayınca Ender Send oluşturur.
 */
public class EnderSendSpawnItem extends Item {

    public EnderSendSpawnItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context,
                              TooltipDisplayComponent displayComponent,
                              Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
        textConsumer.accept(Text.translatable("item.supertntmod.ender_send_spawn_egg.tooltip")
                .formatted(Formatting.GRAY));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if (world.isClient()) return ActionResult.SUCCESS;
        if (!(world instanceof ServerWorld serverWorld)) return ActionResult.PASS;

        BlockPos pos = context.getBlockPos().up();
        PlayerEntity player = context.getPlayer();

        EnderSendEntity entity = new EnderSendEntity(ModEntities.ENDER_SEND, world);
        entity.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        world.spawnEntity(entity);

        world.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1.0f, 0.5f);

        if (player != null && !player.isCreative()) {
            context.getStack().decrement(1);
        }

        return ActionResult.SUCCESS;
    }
}
