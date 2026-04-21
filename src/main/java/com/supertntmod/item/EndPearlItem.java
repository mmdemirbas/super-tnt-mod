package com.supertntmod.item;

import com.supertntmod.entity.EndPearlEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/**
 * End İncisi: atılınca End kaynakları verir; End boyutunda kullanılırsa Dragon ölür.
 */
public class EndPearlItem extends Item {

    public EndPearlItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient()) {
            EndPearlEntity pearl = new EndPearlEntity(world, user);
            pearl.setVelocity(user, user.getPitch(), user.getYaw(), 0.0f, 1.5f, 0.0f);
            world.spawnEntity(pearl);
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENTITY_ENDER_PEARL_THROW, SoundCategory.PLAYERS, 1.0f, 1.0f);
            if (!user.isCreative()) {
                stack.decrement(1);
            }
        }
        return ActionResult.SUCCESS;
    }
}
