package de.soundboardcrafter.activity.common.audioloader;

import static com.google.common.base.Strings.emptyToNull;

import androidx.annotation.Nullable;

import javax.annotation.Nonnull;

/**
 * Static utility methode for audio loading.
 */
class AudioLoaderUtil {
    @Nullable
    static String formatArtist(@Nullable String raw) {
        if (raw == null || raw.equals("<unknown>")) {
            return null;
        }

        return emptyToNull(raw.trim());
    }

    /**
     * Checks whether this <code>path</code> is in this <code>folder</code>
     * (not in a sub-folder).
     */
    static boolean isInFolder(@Nonnull String path, @Nonnull String folder) {
        if (!path.startsWith(folder)) {
            // /other/stuff
            return false;
        }

        if (path.equals(folder)) {
            return false;
        }

        return path.indexOf("/", folder.length()) < 0;
    }
}
