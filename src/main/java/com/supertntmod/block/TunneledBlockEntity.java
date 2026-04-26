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
import org.jetbrains.annotations.Nullable;

/**
 * Tünellenmiş bloğun verilerini tutar: 4×4×4 sub-voxel grid (64 bit) ve
 * her dolu sub-voxel için orijinal blok kimliği.
 * Bit 1 = dolu, bit 0 = kazılmış.
 *
 * Eski format (subBlocks NBT yok) ile yüklenirse, dolu tüm sub-voxel'ler
 * originalBlockId ile yüklenir.
 */
public class TunneledBlockEntity extends BlockEntity {
    private static final int VOXEL_COUNT = 64;

    // 64 bit: 4×4×4 grid. Tüm bitler 1 = tam dolu blok.
    private long subVoxels = 0xFFFFFFFFFFFFFFFFL;
    private Identifier originalBlockId;

    // Sub-voxel başına blok kimliği (lazy: null = tümü originalBlockId)
    @Nullable private Identifier[] subVoxelBlocks;

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
     * Sub-voxel bitmask'ını doğrudan ayarlar. Yeni dolu sub-voxel'ler
     * originalBlockId varsayılanını kullanır (subVoxelBlocks değişmez).
     * Küçülmüş oyuncunun blok yerleştirmesi için kullanılır.
     */
    public void setSubVoxelMask(long mask) {
        this.subVoxels = mask;
        this.cachedShape = null;
        sync();
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
     * Belirli sub-voxel'in blok kimliğini döndürür. Belirli ID
     * atanmamışsa originalBlockId fallback'i döner.
     */
    public Identifier getBlockIdAt(int x, int y, int z) {
        if (subVoxelBlocks != null) {
            Identifier id = subVoxelBlocks[subVoxelIndex(x, y, z)];
            if (id != null) return id;
        }
        return originalBlockId;
    }

    /**
     * Belirli sub-voxel'i kaldırır. Tüm sub-voxel'ler boşsa true döner
     * (blok tamamen kaldırılmalı).
     */
    public boolean removeSubVoxel(int x, int y, int z) {
        int idx = subVoxelIndex(x, y, z);
        subVoxels &= ~(1L << idx);
        if (subVoxelBlocks != null) subVoxelBlocks[idx] = null;
        cachedShape = null;
        sync();
        return subVoxels == 0;
    }

    /**
     * Birden fazla sub-voxel'i tek seferde kaldırır.
     */
    public void removeSubVoxels(int[][] coords) {
        for (int[] c : coords) {
            int idx = subVoxelIndex(c[0], c[1], c[2]);
            subVoxels &= ~(1L << idx);
            if (subVoxelBlocks != null) subVoxelBlocks[idx] = null;
        }
        cachedShape = null;
        sync();
    }

    /**
     * Bir sub-voxel'i belirli bir blok kimliği ile doldurur.
     * Yerleştirme akışı için kullanılır.
     */
    public void fillSubVoxel(int x, int y, int z, Identifier blockId) {
        int idx = subVoxelIndex(x, y, z);
        subVoxels |= (1L << idx);
        if (blockId != null && !blockId.equals(originalBlockId)) {
            if (subVoxelBlocks == null) subVoxelBlocks = new Identifier[VOXEL_COUNT];
            subVoxelBlocks[idx] = blockId;
        } else if (subVoxelBlocks != null) {
            subVoxelBlocks[idx] = null;
        }
        cachedShape = null;
        sync();
    }

    /**
     * Birden fazla sub-voxel'i tek seferde doldurur.
     */
    public void fillSubVoxels(int[][] coords, Identifier blockId) {
        for (int[] c : coords) {
            int idx = subVoxelIndex(c[0], c[1], c[2]);
            subVoxels |= (1L << idx);
            if (blockId != null && !blockId.equals(originalBlockId)) {
                if (subVoxelBlocks == null) subVoxelBlocks = new Identifier[VOXEL_COUNT];
                subVoxelBlocks[idx] = blockId;
            } else if (subVoxelBlocks != null) {
                subVoxelBlocks[idx] = null;
            }
        }
        cachedShape = null;
        sync();
    }

    private void sync() {
        markDirty();
        if (world != null) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
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
        this.subVoxelBlocks = null;
        // Sparse map: anahtarlar "0".."63", değerler blok kimlikleri
        reader.read("subBlocks", NbtCompound.CODEC).ifPresent(nbt -> {
            this.subVoxelBlocks = new Identifier[VOXEL_COUNT];
            for (String key : nbt.getKeys()) {
                try {
                    int idx = Integer.parseInt(key);
                    if (idx < 0 || idx >= VOXEL_COUNT) continue;
                    nbt.getString(key).ifPresent(s -> this.subVoxelBlocks[idx] = Identifier.of(s));
                } catch (NumberFormatException ignored) {
                }
            }
        });
        this.cachedShape = null;
    }

    @Override
    protected void writeData(WriteView writer) {
        writer.putLong("subVoxels", this.subVoxels);
        if (this.originalBlockId != null) {
            writer.putString("originalBlock", this.originalBlockId.toString());
        }
        if (subVoxelBlocks != null) {
            NbtCompound nbt = new NbtCompound();
            for (int i = 0; i < VOXEL_COUNT; i++) {
                Identifier id = subVoxelBlocks[i];
                if (id != null) nbt.putString(Integer.toString(i), id.toString());
            }
            if (!nbt.isEmpty()) {
                writer.put("subBlocks", NbtCompound.CODEC, nbt);
            }
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
     * Orijinal bloğun BlockState'ini döndürür (fallback render için).
     */
    public BlockState getOriginalBlockState() {
        if (originalBlockId == null) return null;
        var block = Registries.BLOCK.get(originalBlockId);
        return block != null ? block.getDefaultState() : null;
    }

    /**
     * Sub-voxel'in bağlı olduğu blok için BlockState döner.
     * Sub-voxel boş ise null döner.
     */
    public @Nullable BlockState getBlockStateAt(int x, int y, int z) {
        if (!isSubVoxelFilled(x, y, z)) return null;
        Identifier id = getBlockIdAt(x, y, z);
        if (id == null) return null;
        var block = Registries.BLOCK.get(id);
        return block != null ? block.getDefaultState() : null;
    }
}
