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

    public static final EntityType<PortalProjectileEntity> PORTAL_PROJECTILE = regThrown("portal_projectile",
            EntityType.Builder.<PortalProjectileEntity>create(
                    (type, world) -> new PortalProjectileEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.25f, 0.25f));

    public static final EntityType<TntFrisbeeEntity> TNT_FRISBEE = regThrown("tnt_frisbee",
            EntityType.Builder.<TntFrisbeeEntity>create(
                    (type, world) -> new TntFrisbeeEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.25f, 0.25f));

    public static final EntityType<GrapplingHookEntity> GRAPPLING_HOOK = regThrown("grappling_hook",
            EntityType.Builder.<GrapplingHookEntity>create(
                    (type, world) -> new GrapplingHookEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.25f, 0.25f));

    public static final EntityType<MagnetTntEntity> MAGNET_TNT = regTnt("magnet_tnt",
            EntityType.Builder.<MagnetTntEntity>create(
                    (type, world) -> new MagnetTntEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f));

    public static final EntityType<GravityTntEntity> GRAVITY_TNT = regTnt("gravity_tnt",
            EntityType.Builder.<GravityTntEntity>create(
                    (type, world) -> new GravityTntEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f));

    public static final EntityType<InvisibleTntEntity> INVISIBLE_TNT = regTnt("invisible_tnt",
            EntityType.Builder.<InvisibleTntEntity>create(
                    (type, world) -> new InvisibleTntEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f));

    public static final EntityType<SwapTntEntity> SWAP_TNT = regTnt("swap_tnt",
            EntityType.Builder.<SwapTntEntity>create(
                    (type, world) -> new SwapTntEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f));

    public static final EntityType<BounceTntEntity> BOUNCE_TNT = regTnt("bounce_tnt",
            EntityType.Builder.<BounceTntEntity>create(
                    (type, world) -> new BounceTntEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f));

    public static final EntityType<ShrinkBallEntity> SHRINK_BALL = regThrown("shrink_ball",
            EntityType.Builder.<ShrinkBallEntity>create(
                    (type, world) -> new ShrinkBallEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.25f, 0.25f));

    public static final EntityType<GrowBallEntity> GROW_BALL = regThrown("grow_ball",
            EntityType.Builder.<GrowBallEntity>create(
                    (type, world) -> new GrowBallEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.25f, 0.25f));

    public static final EntityType<ShrinkPotionEntity> SHRINK_POTION = regThrown("shrink_potion",
            EntityType.Builder.<ShrinkPotionEntity>create(
                    (type, world) -> new ShrinkPotionEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.25f, 0.25f));

    public static final EntityType<GrowPotionEntity> GROW_POTION = regThrown("grow_potion",
            EntityType.Builder.<GrowPotionEntity>create(
                    (type, world) -> new GrowPotionEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.25f, 0.25f));

    public static final EntityType<MobFreezeTntEntity> MOB_FREEZE_TNT = regTnt("mob_freeze_tnt",
            EntityType.Builder.<MobFreezeTntEntity>create(
                    (type, world) -> new MobFreezeTntEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f));

    public static final EntityType<WaterTntEntity> WATER_TNT = regTnt("water_tnt",
            EntityType.Builder.<WaterTntEntity>create(
                    (type, world) -> new WaterTntEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f));

    public static final EntityType<RainbowTntEntity> RAINBOW_TNT = regTnt("rainbow_tnt",
            EntityType.Builder.<RainbowTntEntity>create(
                    (type, world) -> new RainbowTntEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f));

    public static final EntityType<LegoTntEntity> LEGO_TNT = regTnt("lego_tnt",
            EntityType.Builder.<LegoTntEntity>create(
                    (type, world) -> new LegoTntEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f));

    public static final EntityType<MakarnaTntEntity> MAKARNA_TNT = regTnt("makarna_tnt",
            EntityType.Builder.<MakarnaTntEntity>create(
                    (type, world) -> new MakarnaTntEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f));

    public static final EntityType<SekerTntEntity> SEKER_TNT = regTnt("seker_tnt",
            EntityType.Builder.<SekerTntEntity>create(
                    (type, world) -> new SekerTntEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f));

    public static final EntityType<ShrinkTntEntity> SHRINK_TNT = regTnt("shrink_tnt",
            EntityType.Builder.<ShrinkTntEntity>create(
                    (type, world) -> new ShrinkTntEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f));

    public static final EntityType<GrowthTntEntity> GROWTH_TNT = regTnt("growth_tnt",
            EntityType.Builder.<GrowthTntEntity>create(
                    (type, world) -> new GrowthTntEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f));

    public static final EntityType<CleanseTntEntity> CLEANSE_TNT = regTnt("cleanse_tnt",
            EntityType.Builder.<CleanseTntEntity>create(
                    (type, world) -> new CleanseTntEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f));

    // Ender Send — 20 blok boyunda dev mob
    public static final EntityType<EnderSendEntity> ENDER_SEND = regHostile("ender_send",
            EntityType.Builder.<EnderSendEntity>create(
                    (type, world) -> new EnderSendEntity(type, world), SpawnGroup.MONSTER)
                    .dimensions(3.0f, 20.0f));

    public static final EntityType<NetherTntEntity> NETHER_TNT = regTnt("nether_tnt",
            EntityType.Builder.<NetherTntEntity>create(
                    (type, world) -> new NetherTntEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f));

    public static final EntityType<EndTntEntity> END_TNT = regTnt("end_tnt",
            EntityType.Builder.<EndTntEntity>create(
                    (type, world) -> new EndTntEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f));

    public static final EntityType<EndPearlEntity> END_PEARL = regThrown("end_pearl",
            EntityType.Builder.<EndPearlEntity>create(
                    (type, world) -> new EndPearlEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.25f, 0.25f));

    public static final EntityType<NetherPearlEntity> NETHER_PEARL = regThrown("nether_pearl",
            EntityType.Builder.<NetherPearlEntity>create(
                    (type, world) -> new NetherPearlEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.25f, 0.25f));

    public static final EntityType<CamTntEntity> CAM_TNT = regTnt("cam_tnt",
            EntityType.Builder.<CamTntEntity>create(
                    (type, world) -> new CamTntEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f));

    public static final EntityType<GizliTntEntity> GIZLI_TNT = regTnt("gizli_tnt",
            EntityType.Builder.<GizliTntEntity>create(
                    (type, world) -> new GizliTntEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f));

    public static final EntityType<UreyenTntEntity> UREYEN_TNT = regTnt("ureyen_tnt",
            EntityType.Builder.<UreyenTntEntity>create(
                    (type, world) -> new UreyenTntEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f));

    public static final EntityType<KupTntEntity> KUP_TNT = regTnt("kup_tnt",
            EntityType.Builder.<KupTntEntity>create(
                    (type, world) -> new KupTntEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f));

    // Herobrine — 200 HP, tüm canlıları hedef alır
    public static final EntityType<HerobrineEntity> HEROBRINE = regHostile("herobrine",
            EntityType.Builder.<HerobrineEntity>create(
                    (type, world) -> new HerobrineEntity(type, world), SpawnGroup.MONSTER)
                    .dimensions(0.6f, 1.8f));

    private static <T extends net.minecraft.entity.mob.HostileEntity> EntityType<T> regHostile(String name, EntityType.Builder<T> builder) {
        Identifier id = Identifier.of(SuperTntMod.MOD_ID, name);
        EntityType<T> type = builder.build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, id));
        return Registry.register(Registries.ENTITY_TYPE, id, type);
    }

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
        SuperTntMod.LOGGER.info("27 entity türü kaydedildi.");
    }
}
