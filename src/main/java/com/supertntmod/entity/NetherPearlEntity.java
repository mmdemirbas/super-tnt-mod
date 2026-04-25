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
import net.minecraft.util.math.BlockPos;
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

            double tx = player.getX() / 8.0;
            double tz = player.getZ() / 8.0;
            double safeY = findSafeY(netherWorld, tx, tz);
            player.teleport(netherWorld, tx, safeY, tz,
                    Set.of(), player.getYaw(), player.getPitch(), false);
        }
    }

    /**
     * Nether'de iki ardışık hava bloğu bulur — oyuncu içinde durabileceği güvenli Y.
     * Nether'in Y aralığı sabit 0-127; tavanın altından (Y=120) aşağı tarar.
     */
    private static double findSafeY(ServerWorld nether, double x, double z) {
        BlockPos.Mutable cursor = new BlockPos.Mutable();
        int floorX = (int) Math.floor(x);
        int floorZ = (int) Math.floor(z);
        for (int y = 120; y >= 8; y--) {
            cursor.set(floorX, y, floorZ);
            if (!nether.getBlockState(cursor).isAir()) continue;
            cursor.set(floorX, y + 1, floorZ);
            if (!nether.getBlockState(cursor).isAir()) continue;
            cursor.set(floorX, y - 1, floorZ);
            if (nether.getBlockState(cursor).isAir()) continue;
            // y ve y+1 hava, y-1 katı: güvenli
            return y;
        }
        // Fallback: Nether tavanının hemen altı
        return 122.0;
    }
}
