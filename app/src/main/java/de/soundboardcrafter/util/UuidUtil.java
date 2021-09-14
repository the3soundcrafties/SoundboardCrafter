package de.soundboardcrafter.util;

import java.util.UUID;

/**
 * Static utility methods for {@link UUID}s.
 */
public class UuidUtil {
    private UuidUtil() {
    }

    public static long toLong(UUID uuid) {
        return uuid.getMostSignificantBits() & Long.MAX_VALUE;
    }
}
