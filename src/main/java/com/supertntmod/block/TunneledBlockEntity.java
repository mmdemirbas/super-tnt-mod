package com.supertntmod.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

/**
 * Tünellenmiş bloğun verilerini tutar: orijinal blok kimliği ve
 * 4×4×4 sub-voxel bitmask (64 bit).
 * Bit 1 = dolu, bit 0 = kazılmış.
 */
public class TunneledBlockEntity extends BlockEntity {
    // 64 bit: 4×4×4 grid. Tüm bitler 1 = tam dolu blok.
    private long subVoxels = 0xFFFFFFFFFFFFFFFFL;
    private Identifier originalBlockId;
    private VoxelShape cachedShape;

    public TunneledBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.TUNNELED_BLOCK_ENTITY_TYPE, pos, state);
    }

    public Identifier getOriginalBlockId() {
        return originalBlockId;
    }

    public void setOriginalBlockId(Identifier id) {
        this.originalBlockId = id;
    }

    public long getSubVoxels() {
        return subVoxels;
    }

    /**
     * Sub-voxel indeksini hesaplar: x + y*4 + z*16 (her biri 0-3).
     */
    public static int subVoxelIndex(int x, int y, int z) {
        return x + y * 4 + z * 16;
    }

    public boolean isSubVoxelFilled(int x, int y, int z) {
        return (subVoxels & (1L << subVoxelIndex(x, y, z))) != 0;
    }

    /**
     * Belirli sub-voxel'i kaldırır. Tüm sub-voxel'ler boşsa true döner
     * (blok tamamen kaldırılmalı).
     */
    public boolean removeSubVoxel(int x, int y, int z) {
        subVoxels &= ~(1L << subVoxelIndex(x, y, z));
        cachedShape = null; // Shape yeniden hesaplanmalı
        markDirty();
        if (world != null) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
        return subVoxels == 0;
    }

    /**
     * Kalan sub-voxel'lerden VoxelShape hesaplar.
     * Her sub-voxel 0.25×0.25×0.25 blok boyutunda.
     */
    public VoxelShape getVoxelShape() {
        if (cachedShape != null) return cachedShape;

        VoxelShape shape = VoxelShapes.empty();
        for (int z = 0; z < 4; z++) {
            for (int y = 0; y < 4; y++) {
                for (int x = 0; x < 4; x++) {
                    if (isSubVoxelFilled(x, y, z)) {
                        shape = VoxelShapes.union(shape,
                                VoxelShapes.cuboid(
                                        x * 0.25, y * 0.25, z * 0.25,
                                        (x + 1) * 0.25, (y + 1) * 0.25, (z + 1) * 0.25));
                    }
                }
            }
        }
        cachedShape = shape;
        return shape;
    }

    // --- Serialization (1.21.11 ReadView/WriteView API) ---

    @Override
    protected void readData(ReadView reader) {
        this.subVoxels = reader.getLong("subVoxels", 0xFFFFFFFFFFFFFFFFL);
        reader.getOptionalString("originalBlock").ifPresent(s -> this.originalBlockId = Identifier.of(s));
        this.cachedShape = null;
    }

    @Override
    protected void writeData(WriteView writer) {
        writer.putLong("subVoxels", this.subVoxels);
        if (this.originalBlockId != null) {
            writer.putString("originalBlock", this.originalBlockId.toString());
        }
    }

    // --- Client sync ---

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        return createNbt(registries);
    }

    /**
     * Orijinal bloğun BlockState'ini döndürür (render için).
     */
    public BlockState getOriginalBlockState() {
        if (originalBlockId == null) return null;
        var block = Registries.BLOCK.get(originalBlockId);
        return block != null ? block.getDefaultState() : null;
    }
}
