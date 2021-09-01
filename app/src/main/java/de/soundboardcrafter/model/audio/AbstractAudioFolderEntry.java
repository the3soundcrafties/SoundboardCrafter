package de.soundboardcrafter.model.audio;

import androidx.annotation.NonNull;

import java.util.Comparator;

public class AbstractAudioFolderEntry {
    public static Comparator<AbstractAudioFolderEntry> byTypeAnd(
            AudioModelAndSound.SortOrder sortOrder) {
        return (one, other) -> {
            if (one instanceof AudioFolder) {
                // one instanceof AudioFolder
                if (!(other instanceof AudioFolder)) {
                    return -1;
                }

                // one and other instanceof AudioFolder
                return AudioFolder.BY_PATH
                        .compare((AudioFolder) one, (AudioFolder) other);
            }

            // one instanceof AudioModelAndSound
            if (!(other instanceof AudioModelAndSound)) {
                return 1;
            }

            // one and other instanceof AudioModelAndSound
            return sortOrder.getComparator()
                    .compare((AudioModelAndSound) one, (AudioModelAndSound) other);
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o != null && getClass() == o.getClass();
    }

    @Override
    public int hashCode() {
        return 1;
    }

    @Override
    @NonNull
    public String toString() {
        return "AbstractAudioFolderEntry";
    }
}
