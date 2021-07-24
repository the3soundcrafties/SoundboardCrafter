package de.soundboardcrafter.model.audio;

import androidx.annotation.NonNull;

public class AbstractAudioFolderEntry {
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
