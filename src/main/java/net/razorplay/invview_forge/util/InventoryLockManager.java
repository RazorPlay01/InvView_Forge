package net.razorplay.invview_forge.util;

import java.util.*;

public class InventoryLockManager {
    public enum InventoryType {
        PLAYER_INVENTORY,
        ENDER_CHEST,
        CURIOS,
        CURIOS_COSMETIC,
        INVENTORIO,
        TRAVELERS_BACKPACK,
        QUARK_BACKPACK,
        BACKPACKED_BACKPACK
    }

    private static final Map<InventoryType, Set<UUID>> lockedInventories = new EnumMap<>(InventoryType.class);

    static {
        // Inicializar los Sets para cada tipo de inventario
        for (InventoryType type : InventoryType.values()) {
            lockedInventories.put(type, Collections.synchronizedSet(new HashSet<>()));
        }
    }

    public static boolean tryLock(UUID playerUUID, InventoryType type) {
        Set<UUID> locks = lockedInventories.get(type);
        if (locks.contains(playerUUID)) {
            return false;
        }
        return locks.add(playerUUID);
    }

    public static void unlock(UUID playerUUID, InventoryType type) {
        lockedInventories.get(type).remove(playerUUID);
    }

    public static boolean isLocked(UUID playerUUID, InventoryType type) {
        return lockedInventories.get(type).contains(playerUUID);
    }

    public static boolean hasAnyLock(UUID playerUUID) {
        return lockedInventories.values().stream()
                .anyMatch(set -> set.contains(playerUUID));
    }

    public static void unlockAll(UUID playerUUID) {
        lockedInventories.values().forEach(set -> set.remove(playerUUID));
    }

    public static Optional<UUID> getLocker(InventoryType type) {
        Set<UUID> locks = lockedInventories.get(type);
        return locks.isEmpty() ? Optional.empty() : Optional.of(locks.iterator().next());
    }

    public static boolean isInventoryLocked(InventoryType type) {
        return !lockedInventories.get(type).isEmpty();
    }

    public static void clearAllLocks() {
        lockedInventories.values().forEach(Set::clear);
    }
}
