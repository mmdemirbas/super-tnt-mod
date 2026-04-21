package com.supertntmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

/**
 * End İncisi entity: yere düşünce sahibine End kaynakları verir.
 * End boyutunda düşürülürse Ender Dragon'ı anında öldürür.
 */
public class EndPearlEntity extends ThrownEntity {

    public EndPearlEntity(EntityType<? extends ThrownEntity> type, World world) {
        super(type, world);
    }

    public EndPearlEntity(World world, LivingEntity owner) {
        super(ModEntities.END_PEARL, owner.getX(), owner.getEyeY() - 0.1, owner.getZ(), world);
        this.setOwner(owner);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        super.onBlockHit(blockHitResult);
        if (getEntityWorld().isClient()) return;
        applyEffect();
        discard();
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);
        if (getEntityWorld().isClient()) return;
        applyEffect();
        discard();
    }

    private void applyEffect() {
        World world = getEntityWorld();
        if (!(world instanceof ServerWorld serverWorld)) return;
        if (!(getOwner() instanceof PlayerEntity player)) return;

        world.playSound(null, getX(), getY(), getZ(),
                SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.PLAYERS, 1.0f, 1.0f);

        // End boyutunda: Ender Dragon'ı öldür
        if (world.getRegistryKey() == net.minecraft.world.World.END) {
            serverWorld.getEntitiesByType(net.minecraft.entity.EntityType.ENDER_DRAGON, e -> true)
                    .forEach(dragon -> dragon.damage(serverWorld,
                            world.getDamageSources().genericKill(), Float.MAX_VALUE));
            player.sendMessage(
                    Text.literal("Ender Dragon yok edildi!").formatted(Formatting.LIGHT_PURPLE), false);
        }

        // Eşyaları ver
        give(player, new ItemStack(net.minecraft.block.Blocks.END_STONE, 64));
        give(player, new ItemStack(net.minecraft.item.Items.END_CRYSTAL, 8));
        give(player, new ItemStack(net.minecraft.block.Blocks.BEDROCK, 64));

        player.sendMessage(
                Text.literal("End kaynakları verildi!").formatted(Formatting.AQUA), true);
    }

    private static void give(PlayerEntity player, ItemStack stack) {
        if (!player.getInventory().insertStack(stack)) {
            player.dropItem(stack, false);
        }
    }
}
