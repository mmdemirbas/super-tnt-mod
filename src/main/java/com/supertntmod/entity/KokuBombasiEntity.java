package com.supertntmod.entity;

import com.supertntmod.block.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 🥚 Koku Bombası Entity — atılan yeşil yumurta.
 * Çarptığı noktada zehir toprağı saçar; bu toprağa değen herkes anında ölür.
 */
public class KokuBombasiEntity extends ThrownEntity {

    private static final int RADIUS = 3;       // (x,z) yarıçap
    private static final int VERTICAL_SCAN = 4; // çarpma noktasına göre yukarı/aşağı tarama
    private static final float DENSITY = 0.65f; // hücre başına saçma olasılığı

    public KokuBombasiEntity(EntityType<? extends ThrownEntity> type, World world) {
        super(type, world);
    }

    public KokuBombasiEntity(World world, LivingEntity owner) {
        super(ModEntities.KOKU_BOMBASI, owner.getX(), owner.getEyeY() - 0.1, owner.getZ(), world);
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

        BlockPos impact = this.getBlockPos();
        double cx = this.getX(), cy = this.getY(), cz = this.getZ();

        world.playSound(null, cx, cy, cz,
                SoundEvents.ENTITY_EGG_THROW, SoundCategory.PLAYERS, 1.0f, 0.6f);
        world.playSound(null, cx, cy, cz,
                SoundEvents.BLOCK_SLIME_BLOCK_BREAK, SoundCategory.BLOCKS, 1.2f, 0.7f);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, cx, cy + 0.2, cz, 30, 0.6, 0.4, 0.6, 0.05);

        scatterPoison(world, impact);

        this.discard();
    }

    private static void scatterPoison(ServerWorld world, BlockPos impact) {
        var random = world.random;
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                int dist2 = dx * dx + dz * dz;
                if (dist2 > RADIUS * RADIUS) continue;
                // Merkez yaklaştıkça olasılık artar
                float p = DENSITY * (1.0f - (float) Math.sqrt(dist2) / (RADIUS + 1));
                if (random.nextFloat() > p) continue;

                BlockPos surface = findGround(world, impact.getX() + dx, impact.getZ() + dz, impact.getY());
                if (surface == null) continue;
                BlockState here = world.getBlockState(surface);
                if (!here.isAir() && !here.isReplaceable()) continue;

                world.setBlockState(surface, ModBlocks.ZEHIR_TOPRAK.getDefaultState(), 3);
                // ScheduledTick onBlockAdded'da kuruldu; ayrıca burada da garanti olsun:
                world.scheduleBlockTick(surface, ModBlocks.ZEHIR_TOPRAK,
                        com.supertntmod.block.ZehirToprakBlock.DECAY_TICKS);
            }
        }
    }

    /**
     * Verilen (x,z)'de çarpma yüksekliğine yakın bir zemin üstü konum bul.
     * Önce çarpma seviyesine yakın bir yere bak; orada hava ve altı sağlam ise oraya yerleştir.
     */
    private static BlockPos findGround(World world, int x, int z, int baseY) {
        for (int dy = VERTICAL_SCAN; dy >= -VERTICAL_SCAN; dy--) {
            BlockPos candidate = new BlockPos(x, baseY + dy, z);
            BlockState here = world.getBlockState(candidate);
            BlockState below = world.getBlockState(candidate.down());
            boolean spaceFree = here.isAir() || here.isReplaceable();
            boolean groundSolid = below.isSolidBlock(world, candidate.down());
            if (spaceFree && groundSolid) return candidate;
        }
        return null;
    }
}
