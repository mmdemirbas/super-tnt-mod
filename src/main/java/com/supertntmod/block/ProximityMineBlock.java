package com.supertntmod.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Yakınlık Mayını: yere yerleştirilince 2 blok yarıçapını izler.
 * Herhangi bir canlı yaklaşınca tetiklenerek patlar.
 *
 * <p>Mekanik: her 10 tick'te bir {@code scheduledTick} çalıştırır ve
 * yakında canlı var mı kontrol eder. Blok sahibi koyar koymaz 2 saniyelik
 * (40 tick) gecikmeli tetikleme başlar, bu süre içinde uzaklaşılabilir.</p>
 */
public class ProximityMineBlock extends Block {

    /** Tetiklenme için tarama yarıçapı (blok) */
    private static final double DETECTION_RADIUS = 2.0;
    /** İlk tetiklenme gecikmesi (tick): koyandan kaçmak için zaman */
    private static final int ARM_DELAY = 40;
    /** Aktif tarama aralığı (tick) */
    private static final int SCAN_INTERVAL = 10;

    /** Tooltip "kacmak icin 2 saniyeniz var" diyor; algilama ile patlama
     *  arasinda gercekten bu kadar sure olmali. Eskiden ayni tick'te
     *  patliyordu ve kacis penceresi diye bir sey yoktu. */
    private static final int ESCAPE_DELAY = 40;

    /** Geri sayima giren mayinlar. Blok state'e ozellik eklemek blockstate
     *  JSON'unu da degistirmeyi gerektirirdi; bu daha ucuz. */
    private static final java.util.Set<net.minecraft.util.math.GlobalPos> COUNTING =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    public static void clearAll() {
        COUNTING.clear();
    }

    public ProximityMineBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state,
                         @Nullable LivingEntity placer, ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);
        if (!world.isClient()) {
            // ARM_DELAY sonra ilk taramayı başlat
            world.scheduleBlockTick(pos, this, ARM_DELAY);
        }
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // Blok hâlâ yerinde mi?
        if (!world.getBlockState(pos).isOf(this)) return;

        net.minecraft.util.math.GlobalPos key =
                new net.minecraft.util.math.GlobalPos(world.getRegistryKey(), pos.toImmutable());

        // Geri sayim bittiyse patlat
        if (COUNTING.remove(key)) {
            world.setBlockState(pos, Blocks.AIR.getDefaultState());
            world.createExplosion(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    3.0f, false, World.ExplosionSourceType.TNT);
            return;
        }

        // Yakında canlı var mı?
        boolean triggered = !world.getEntitiesByClass(
                LivingEntity.class,
                new Box(pos).expand(DETECTION_RADIUS),
                e -> e.isAlive() && !(e instanceof net.minecraft.entity.decoration.ArmorStandEntity)
        ).isEmpty();

        if (triggered) {
            // Uyarı: ses + partikül, sonra ESCAPE_DELAY kadar kaçış süresi
            COUNTING.add(key);
            world.scheduleBlockTick(pos, this, ESCAPE_DELAY);
            world.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                    SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 2.0f, 1.4f);
            world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    2, 0.5, 0.5, 0.5, 0.0);

        } else {
            // Henüz tetiklenmediyse bir sonraki taramayı zamanla
            world.scheduleBlockTick(pos, this, SCAN_INTERVAL);
        }
    }

    // Kırılma durumunda özel işlem gerekmez:
    // scheduleBlockTick, blok artık bu tür olmadığında kendiliğinden geçersiz kalır.
}
