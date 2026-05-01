package com.supertntmod.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

/**
 * Eşya Çalmaca için proxy: bir başka oyuncunun ana envanterini (36 slot)
 * bir Inventory olarak dışa açar; vanilla GenericContainerScreenHandler buna
 * "sandık" gibi bağlanabilir.
 *
 * Hot bar dahil tüm ana envanter (slot 0-35) erişilebilir. Zırh ve off-hand
 * dışarıda bırakılır (kasıtlı: vurkaç hırsızlığı, çıplaklaştırma değil).
 */
public class PlayerInventoryProxy implements Inventory {

    private final PlayerInventory target;

    public PlayerInventoryProxy(PlayerInventory target) {
        this.target = target;
    }

    @Override
    public int size() {
        return 36;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < size(); i++) {
            if (!target.getStack(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        if (slot < 0 || slot >= size()) return ItemStack.EMPTY;
        return target.getStack(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        if (slot < 0 || slot >= size()) return ItemStack.EMPTY;
        return target.removeStack(slot, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        if (slot < 0 || slot >= size()) return ItemStack.EMPTY;
        return target.removeStack(slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (slot < 0 || slot >= size()) return;
        target.setStack(slot, stack);
    }

    @Override
    public void markDirty() {
        target.markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        // Mesafe kontrolü Item.use() tarafında yapıldı; UI açık iken target uzaklaşabilir
        // ama zaten 10 blok kontrolü use'da bir kerelik. UI içinde kapatma entegrasyonu
        // ekstra karmaşıklık; pragmatik olarak true döndür.
        return true;
    }

    @Override
    public void clear() {
        // Hedef envanteri toptan silmek istemiyoruz; bu UI hareketi değil.
        // Vanilla genelde bu yöntemi sadece BlockEntity destruction'da kullanır.
    }
}
