package de.soundboardcrafter.dao;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.soundboardcrafter.model.Sound;

/**
 * Contains static methods for the creation of Sounds from audio files.
 */
public class SoundFromFileCreationUtil {
    private static final Pattern ONLY_THE_INTERESTING_PARTS = Pattern.compile(".*\\s.*\\-(.*)");

    private SoundFromFileCreationUtil() {
    }

    /**
     * Creates a sound for this filename and this path. Chooses the interesting parts
     * from the filename.
     */
    public static Sound createSound(String filename, String path) {
        Matcher matcher = ONLY_THE_INTERESTING_PARTS.matcher(filename);
        if (matcher.matches()) {
            filename = matcher.group(1);
        }

        filename = filename.substring(0, filename.indexOf("."));

        return new Sound(path, filename);
    }
}
