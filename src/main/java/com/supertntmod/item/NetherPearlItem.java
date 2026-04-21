package com.supertntmod.item;

import com.supertntmod.entity.NetherPearlEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/**
 * Nether İncisi: atılınca Nether'e ışınlar ve Blaze Rod verir.
 */
public class NetherPearlItem extends Item {

    public NetherPearlItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient()) {
            NetherPearlEntity pearl = new NetherPearlEntity(world, user);
            pearl.setVelocity(user, user.getPitch(), user.getYaw(), 0.0f, 1.5f, 0.0f);
            world.spawnEntity(pearl);
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.BLOCK_PORTAL_TRIGGER, SoundCategory.PLAYERS, 1.0f, 0.7f);
            if (!user.isCreative()) {
                stack.decrement(1);
            }
        }
        return ActionResult.SUCCESS;
    }
}
