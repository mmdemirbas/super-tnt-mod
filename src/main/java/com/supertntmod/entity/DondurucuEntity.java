package com.supertntmod.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

/**
 * 🧊 Dondurucu Entity — denizin kalbi mermisi.
 * Bir canlıya çarpınca onu öldürür, atan oyuncuya o canlının spawn egg'ini verir
 * (fonksiyonel olarak "bloğa dönüştürmek" — yeniden yerleştirilebilir taşınır eşya).
 * Spawn egg'i olmayan canlılar (ender dragon, wither vb.) işlenemez; mermi etkisiz çarpar.
 */
public class DondurucuEntity extends ThrownEntity {

    public DondurucuEntity(EntityType<? extends ThrownEntity> type, World world) {
        super(type, world);
    }

    public DondurucuEntity(World world, LivingEntity owner) {
        super(ModEntities.DONDURUCU, owner.getX(), owner.getEyeY() - 0.1, owner.getZ(), world);
        this.setOwner(owner);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        // Ek veri gerekmez
    }

    @Override
    protected void onCollision(HitResult hit) {
        super.onCollision(hit);
        if (this.getEntityWorld().isClient()) return;
        if (!(this.getEntityWorld() instanceof ServerWorld world)) return;

        if (hit.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) hit;
            Entity target = entityHit.getEntity();

            // Atan oyuncuya çarpmasın
            Entity owner = this.getOwner();
            if (owner != null && owner.equals(target)) {
                return; // mermi geçsin
            }

            if (target instanceof LivingEntity living && living.isAlive()) {
                tryFreeze(world, living);
                discardFx(world);
                this.discard();
                return;
            }
        }

        // Bloğa veya işlenemeyen şeye çarptı: sadece ses + partikül
        discardFx(world);
        this.discard();
    }

    private void discardFx(ServerWorld world) {
        world.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.7f, 1.4f);
        world.spawnParticles(ParticleTypes.SNOWFLAKE, this.getX(), this.getY(), this.getZ(),
                20, 0.4, 0.4, 0.4, 0.05);
    }

    private void tryFreeze(ServerWorld world, LivingEntity target) {
        EntityType<?> type = target.getType();

        // Spawn egg'i bul
        SpawnEggItem egg = SpawnEggItem.forEntity(type);
        if (egg == null) {
            // Spawn egg yok — boss veya özel canlı; "donmaz"
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.4f, 0.6f);
            sendOwner(Text.translatable("item.supertntmod.dondurucu.cant_freeze",
                    type.getName()));
            return;
        }

        // Görsel etki: dondurma
        world.spawnParticles(ParticleTypes.SNOWFLAKE,
                target.getX(), target.getY() + target.getHeight() / 2.0,
                target.getZ(), 50, 0.5, target.getHeight() / 2.0, 0.5, 0.05);
        world.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.BLOCK_GLASS_PLACE, SoundCategory.PLAYERS, 1.0f, 1.4f);

        // Hedefi öldür
        target.kill(world);

        // Spawn egg'i atan oyuncuya ver
        ItemStack stack = new ItemStack((Item) egg, 1);
        Entity owner = this.getOwner();
        if (owner instanceof PlayerEntity player) {
            if (!player.getInventory().insertStack(stack)) {
                // Envanter dolu: yere düşür
                ItemEntity drop = new ItemEntity(world, player.getX(), player.getY(), player.getZ(), stack);
                world.spawnEntity(drop);
            }
        } else {
            // Sahip yok: hedef konumunda yere düşür
            ItemEntity drop = new ItemEntity(world,
                    target.getX(), target.getY(), target.getZ(), stack);
            world.spawnEntity(drop);
        }
    }

    private void sendOwner(Text message) {
        if (this.getOwner() instanceof PlayerEntity player) {
            player.sendMessage(message, true);
        }
    }
}
