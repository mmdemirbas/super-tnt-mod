package com.supertntmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

import java.util.Set;

/**
 * Nether İncisi entity: yere düşünce sahibini Nether'e ışınlar ve 64 Blaze Rod verir.
 */
public class NetherPearlEntity extends ThrownEntity {

    public NetherPearlEntity(EntityType<? extends ThrownEntity> type, World world) {
        super(type, world);
    }

    public NetherPearlEntity(World world, LivingEntity owner) {
        super(ModEntities.NETHER_PEARL, owner.getX(), owner.getEyeY() - 0.1, owner.getZ(), world);
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
                SoundEvents.BLOCK_PORTAL_TRIGGER, SoundCategory.PLAYERS, 1.0f, 0.7f);

        // Nether'e ışınla
        ServerWorld netherWorld = serverWorld.getServer().getWorld(net.minecraft.world.World.NETHER);
        if (netherWorld != null) {
            // Blaze Rod'ları ver (ışınlanmadan önce)
            ItemStack blazeRods = new ItemStack(Items.BLAZE_ROD, 64);
            if (!player.getInventory().insertStack(blazeRods)) {
                player.dropItem(blazeRods, false);
            }
            player.sendMessage(
                    Text.literal("Nether'e hoş geldin! 64 Blaze Rod verildi.")
                            .formatted(Formatting.RED), false);

            player.teleport(netherWorld, player.getX() / 8.0, 64.0, player.getZ() / 8.0,
                    Set.of(), player.getYaw(), player.getPitch(), false);
        }
    }
}
