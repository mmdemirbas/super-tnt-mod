package com.supertntmod.entity;

import com.supertntmod.SuperTntMod;
import com.supertntmod.item.ModItems;
import com.supertntmod.item.TunnelingItem;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Küçülten TNT: 15 blok yarıçapındaki tüm canlıları küçültür.
 * Scale attribute modifier kullanarak 0.3x boyuta indirir.
 */
public class ShrinkTntEntity extends TntEntity {
    private static final int RADIUS = 15;
    public static final Identifier SCALE_MODIFIER_ID = Identifier.of(SuperTntMod.MOD_ID, "scale");
    private boolean done = false;

    public ShrinkTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public ShrinkTntEntity(World world, double x, double y, double z,
                           @Nullable LivingEntity igniter) {
        super(ModEntities.SHRINK_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
    }

    @Override
    public void tick() {
        if (!done && this.getFuse() <= 1 && !this.getEntityWorld().isClient()) {
            done = true;
            BlockPos center = this.getBlockPos();
            World world = getEntityWorld();
            this.discard();

            // Küçülme sesi
            world.playSound(null, center.getX(), center.getY(), center.getZ(),
                    SoundEvents.ENTITY_PUFFER_FISH_BLOW_OUT, SoundCategory.BLOCKS, 2.0f, 2.0f);

            // Yakındaki tüm canlıları küçült
            world.getEntitiesByClass(LivingEntity.class,
                    new net.minecraft.util.math.Box(center).expand(RADIUS),
                    e -> true
            ).forEach(entity -> {
                EntityAttributeInstance scaleAttr = entity.getAttributeInstance(EntityAttributes.SCALE);
                if (scaleAttr != null) {
                    // Mevcut ölçeği oku ve 0.3 ile çarp (kümülatif küçültme)
                    double currentScale = scaleAttr.getValue();
                    double newScale = currentScale * 0.3;
                    double newModifierValue = newScale - 1.0;

                    scaleAttr.removeModifier(SCALE_MODIFIER_ID);
                    scaleAttr.addPersistentModifier(new EntityAttributeModifier(
                            SCALE_MODIFIER_ID, newModifierValue,
                            EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE));

                    // 1/12 ölçeğe ulaşan oyunculara tünelleme aleti ver
                    if (entity instanceof PlayerEntity player && newScale <= TunnelingItem.SCALE_THRESHOLD) {
                        if (!player.getInventory().contains(new ItemStack(ModItems.TUNNELING_ITEM))) {
                            player.getInventory().insertStack(new ItemStack(ModItems.TUNNELING_ITEM));
                            player.sendMessage(Text.translatable("item.supertntmod.tunneling_item.granted"), false);
                        }
                    }
                }
            });

            // Küçülme partikülleri (içe doğru çekilen efekt)
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.WITCH,
                        center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5,
                        200, 5.0, 3.0, 5.0, 0.02);
                serverWorld.spawnParticles(ParticleTypes.ENCHANT,
                        center.getX() + 0.5, center.getY() + 2, center.getZ() + 0.5,
                        100, 4.0, 3.0, 4.0, 0.5);
            }

            // Görsel patlama
            world.createExplosion(null, center.getX() + 0.5, center.getY(),
                    center.getZ() + 0.5, 0.0f, false, World.ExplosionSourceType.NONE);
            return;
        }
        if (!done) super.tick();
    }
}
