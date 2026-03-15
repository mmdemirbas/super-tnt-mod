package com.supertntmod.entity;

import com.supertntmod.SuperTntMod;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModEntities {

    public static final EntityType<DiamondTntEntity> DIAMOND_TNT = regTnt("diamond_tnt",
            EntityType.Builder.<DiamondTntEntity>create(
                    (type, world) -> new DiamondTntEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f));

    public static final EntityType<GoldTntEntity> GOLD_TNT = regTnt("gold_tnt",
            EntityType.Builder.<GoldTntEntity>create(
                    (type, world) -> new GoldTntEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f));

    public static final EntityType<BedrockTntEntity> BEDROCK_TNT = regTnt("bedrock_tnt",
            EntityType.Builder.<BedrockTntEntity>create(
                    (type, world) -> new BedrockTntEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f));

    public static final EntityType<EmeraldTntEntity> EMERALD_TNT = regTnt("emerald_tnt",
            EntityType.Builder.<EmeraldTntEntity>create(
                    (type, world) -> new EmeraldTntEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f));

    public static final EntityType<LightningTntEntity> LIGHTNING_TNT = regTnt("lightning_tnt",
            EntityType.Builder.<LightningTntEntity>create(
                    (type, world) -> new LightningTntEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f));

    public static final EntityType<NuclearTntEntity> NUCLEAR_TNT = regTnt("nuclear_tnt",
            EntityType.Builder.<NuclearTntEntity>create(
                    (type, world) -> new NuclearTntEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f));

    public static final EntityType<FreezeTntEntity> FREEZE_TNT = regTnt("freeze_tnt",
            EntityType.Builder.<FreezeTntEntity>create(
                    (type, world) -> new FreezeTntEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f));

    // Yeni entity türleri

    public static final EntityType<WoodTntEntity> WOOD_TNT = regTnt("wood_tnt",
            EntityType.Builder.<WoodTntEntity>create(
                    (type, world) -> new WoodTntEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f));

    public static final EntityType<CommandTntEntity> COMMAND_TNT = regTnt("command_tnt",
            EntityType.Builder.<CommandTntEntity>create(
                    (type, world) -> new CommandTntEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f));

    public static final EntityType<WalkingTntEntity> WALKING_TNT = regPathAware("walking_tnt",
            EntityType.Builder.<WalkingTntEntity>create(
                    (type, world) -> new WalkingTntEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f));

    public static final EntityType<TntFrisbeeEntity> TNT_FRISBEE = regThrown("tnt_frisbee",
            EntityType.Builder.<TntFrisbeeEntity>create(
                    (type, world) -> new TntFrisbeeEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.25f, 0.25f));

    public static final EntityType<MobFreezeTntEntity> MOB_FREEZE_TNT = regTnt("mob_freeze_tnt",
            EntityType.Builder.<MobFreezeTntEntity>create(
                    (type, world) -> new MobFreezeTntEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f));

    public static final EntityType<RainbowTntEntity> RAINBOW_TNT = regTnt("rainbow_tnt",
            EntityType.Builder.<RainbowTntEntity>create(
                    (type, world) -> new RainbowTntEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f));

    private static <T extends TntEntity> EntityType<T> regTnt(String name, EntityType.Builder<T> builder) {
        Identifier id = Identifier.of(SuperTntMod.MOD_ID, name);
        EntityType<T> type = builder.build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, id));
        return Registry.register(Registries.ENTITY_TYPE, id, type);
    }

    private static <T extends PathAwareEntity> EntityType<T> regPathAware(String name, EntityType.Builder<T> builder) {
        Identifier id = Identifier.of(SuperTntMod.MOD_ID, name);
        EntityType<T> type = builder.build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, id));
        return Registry.register(Registries.ENTITY_TYPE, id, type);
    }

    private static <T extends ThrownEntity> EntityType<T> regThrown(String name, EntityType.Builder<T> builder) {
        Identifier id = Identifier.of(SuperTntMod.MOD_ID, name);
        EntityType<T> type = builder.build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, id));
        return Registry.register(Registries.ENTITY_TYPE, id, type);
    }

    public static void register() {
        SuperTntMod.LOGGER.info("13 entity türü kaydedildi.");
    }
}
