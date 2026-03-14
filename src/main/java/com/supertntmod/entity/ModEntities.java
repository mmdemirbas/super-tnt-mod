package com.supertntmod.entity;

import com.supertntmod.SuperTntMod;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.TntEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModEntities {

    public static final EntityType<DiamondTntEntity> DIAMOND_TNT = reg("diamond_tnt",
            EntityType.Builder.<DiamondTntEntity>create(
                    (type, world) -> new DiamondTntEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f).build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(SuperTntMod.MOD_ID, "diamond_tnt"))));

    public static final EntityType<GoldTntEntity> GOLD_TNT = reg("gold_tnt",
            EntityType.Builder.<GoldTntEntity>create(
                    (type, world) -> new GoldTntEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f).build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(SuperTntMod.MOD_ID, "gold_tnt"))));

    public static final EntityType<BedrockTntEntity> BEDROCK_TNT = reg("bedrock_tnt",
            EntityType.Builder.<BedrockTntEntity>create(
                    (type, world) -> new BedrockTntEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f).build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(SuperTntMod.MOD_ID, "bedrock_tnt"))));

    public static final EntityType<EmeraldTntEntity> EMERALD_TNT = reg("emerald_tnt",
            EntityType.Builder.<EmeraldTntEntity>create(
                    (type, world) -> new EmeraldTntEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f).build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(SuperTntMod.MOD_ID, "emerald_tnt"))));

    public static final EntityType<LightningTntEntity> LIGHTNING_TNT = reg("lightning_tnt",
            EntityType.Builder.<LightningTntEntity>create(
                    (type, world) -> new LightningTntEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f).build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(SuperTntMod.MOD_ID, "lightning_tnt"))));

    public static final EntityType<NuclearTntEntity> NUCLEAR_TNT = reg("nuclear_tnt",
            EntityType.Builder.<NuclearTntEntity>create(
                    (type, world) -> new NuclearTntEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f).build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(SuperTntMod.MOD_ID, "nuclear_tnt"))));

    public static final EntityType<FreezeTntEntity> FREEZE_TNT = reg("freeze_tnt",
            EntityType.Builder.<FreezeTntEntity>create(
                    (type, world) -> new FreezeTntEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f).build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(SuperTntMod.MOD_ID, "freeze_tnt"))));

    private static <T extends TntEntity> EntityType<T> reg(String name, EntityType<T> type) {
        return Registry.register(Registries.ENTITY_TYPE, Identifier.of(SuperTntMod.MOD_ID, name), type);
    }

    public static void register() {
        SuperTntMod.LOGGER.info("7 TNT entity türü kaydedildi.");
    }
}
