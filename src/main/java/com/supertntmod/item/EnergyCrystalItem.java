package com.supertntmod.item;

import net.minecraft.block.Blocks;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.function.Consumer;

/**
 * Enerji Kristali: Kırılmaz bedrock bloklarını kırmaya yarar.
 * Bedrock kırıldığında altından yeni bedrock çıkar.
 */
public class EnergyCrystalItem extends Item {

    public EnergyCrystalItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context,
                              TooltipDisplayComponent displayComponent,
                              Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
        textConsumer.accept(Text.translatable("item.supertntmod.energy_crystal.tooltip").formatted(Formatting.GRAY));
    }

    /**
     * Havaya sağ tıklama: Ametist Zırh gevşetme.
     * Oyuncu ametist zırh giyiyorsa, 5 saniyeliğine gevşetir.
     */
    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient()) return ActionResult.PASS;
        if (!(user instanceof ServerPlayerEntity player)) return ActionResult.PASS;

        // Ametist zırh giyiyor mu kontrol et
        boolean wearingAmethyst = false;
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getEquippedStack(slot);
            if (stack.isOf(ModItems.AMETHYST_HELMET) || stack.isOf(ModItems.AMETHYST_CHESTPLATE)
                    || stack.isOf(ModItems.AMETHYST_LEGGINGS) || stack.isOf(ModItems.AMETHYST_BOOTS)) {
                wearingAmethyst = true;
                break;
            }
        }

        if (!wearingAmethyst) return ActionResult.PASS;

        // Zırhı gevşet
        AmethystArmorState.loosen(player.getUuid());
        player.sendMessage(Text.translatable("message.supertntmod.amethyst_armor.loosened"), true);

        // Efektler
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.0f, 1.0f);
        if (world instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.END_ROD,
                    player.getX(), player.getBodyY(0.5), player.getZ(),
                    15, 0.3, 0.5, 0.3, 0.05);
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        PlayerEntity player = context.getPlayer();

        if (!world.getBlockState(pos).isOf(Blocks.BEDROCK)) {
            return ActionResult.PASS;
        }

        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        // Boşluğa düşmemek için: bu bedrock dünya tabanındaysa kırma.
        // Aksi halde altında bedrock varsa olduğu gibi kalır, yoksa altına yeni bedrock yerleşir.
        // Creative oyuncu zaten uçabilir, kısıtlama uygulamıyoruz.
        boolean creative = player != null && player.isCreative();
        if (!creative && pos.getY() <= world.getBottomY()) {
            if (player != null) {
                player.sendMessage(Text.literal("Dünya tabanı! Bunu kıramazsın.")
                        .formatted(Formatting.RED), true);
            }
            return ActionResult.FAIL;
        }

        // Bedrock'u kır (hava ile değiştir)
        world.setBlockState(pos, Blocks.AIR.getDefaultState());

        // Altından yeni bedrock çıkar (dünya tabanının dışına geçme)
        BlockPos below = pos.down();
        if (below.getY() >= world.getBottomY() && world.getBlockState(below).isAir()) {
            world.setBlockState(below, Blocks.BEDROCK.getDefaultState());
        }

        // Efektler
        world.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.BLOCKS, 1.0f, 0.5f);
        if (world instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    20, 0.3, 0.3, 0.3, 0.05);
        }

        // Durabilite düşür veya sayıyı azalt (burada sadece bilgi mesajı)
        if (player != null) {
            player.sendMessage(Text.translatable("item.supertntmod.energy_crystal.broken"), true);
        }

        return ActionResult.SUCCESS;
    }
}
