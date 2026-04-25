package com.supertntmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class GizliTntEntity extends TntEntity {
    private static final int RADIUS = 25;
    private boolean done = false;
    private @Nullable UUID igniterUuid = null;

    public GizliTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
        this.setFuse(80);
    }

    public GizliTntEntity(World world, double x, double y, double z, @Nullable LivingEntity igniter) {
        super(ModEntities.GIZLI_TNT, world);
        this.setPosition(x, y, z);
        this.setFuse(80);
        if (igniter instanceof PlayerEntity player) {
            this.igniterUuid = player.getUuid();
        }
    }

    @Override
    public void tick() {
        if (!done && this.getFuse() <= 1 && !getEntityWorld().isClient()) {
            done = true;
            double x = getX(), y = getY(), z = getZ();
            World world = getEntityWorld();
            this.discard();

            if (world instanceof ServerWorld serverWorld) {
                world.getEntitiesByClass(LivingEntity.class,
                        new Box(x - RADIUS, y - RADIUS, z - RADIUS, x + RADIUS, y + RADIUS, z + RADIUS),
                        e -> igniterUuid == null || !e.getUuid().equals(igniterUuid)
                ).forEach(entity -> entity.damage(serverWorld,
                        world.getDamageSources().genericKill(), Float.MAX_VALUE));
            }
            return;
        }
        if (!done) super.tick();
    }

    @Override
    public void readData(ReadView reader) {
        super.readData(reader);
        reader.getOptionalString("IgniterUuid").ifPresent(s -> igniterUuid = UUID.fromString(s));
    }

    @Override
    public void writeData(WriteView writer) {
        super.writeData(writer);
        if (igniterUuid != null) {
            writer.putString("IgniterUuid", igniterUuid.toString());
        }
    }
}
