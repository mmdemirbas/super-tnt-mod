package com.supertntmod.item;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ametist Zırh gevşeme durumunu takip eder.
 * Gevşetilmemiş ametist zırh envanter etkileşimlerini engeller.
 * Enerji Kristali kullanılarak geçici olarak gevşetilir.
 */
public final class AmethystArmorState {

    // Oyuncu UUID → gevşeme bitiş zamanı (System.currentTimeMillis)
    private static final Map<UUID, Long> LOOSENED_UNTIL = new ConcurrentHashMap<>();

    // Gevşeme süresi: 5 saniye
    private static final long LOOSEN_DURATION_MS = 5000;

    public static void loosen(UUID playerUuid) {
        LOOSENED_UNTIL.put(playerUuid, System.currentTimeMillis() + LOOSEN_DURATION_MS);
    }

    public static boolean isLoosened(UUID playerUuid) {
        Long until = LOOSENED_UNTIL.get(playerUuid);
        if (until == null) return false;
        if (System.currentTimeMillis() > until) {
            LOOSENED_UNTIL.remove(playerUuid);
            return false;
        }
        return true;
    }

    public static void onPlayerDisconnect(UUID playerUuid) {
        LOOSENED_UNTIL.remove(playerUuid);
    }

    public static void clearAll() {
        LOOSENED_UNTIL.clear();
    }

    private AmethystArmorState() {}
}
