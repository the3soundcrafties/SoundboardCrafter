package de.soundboardcrafter.model;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Static methods for dealing with paths.
 */
@ParametersAreNonnullByDefault
public class PathUtil {
    private PathUtil() {
    }

    @Nonnull
    public static String extractFileName(String path) {
        int lastIndexOfSlash = path.lastIndexOf("/");
        if (lastIndexOfSlash < 0) {
            return path;
        }

        return path.substring(lastIndexOfSlash + 1);
    }

    @Nonnull
    public static String removeExtension(String fileName) {
        int indexOfDot = fileName.lastIndexOf(".");
        if (indexOfDot <= 0) {
            return fileName;
        }

        return fileName.substring(0, indexOfDot);
    }
}
