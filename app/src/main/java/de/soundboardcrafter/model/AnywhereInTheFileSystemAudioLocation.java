package de.soundboardcrafter.model;

import androidx.annotation.NonNull;

import javax.annotation.Nullable;

public class AnywhereInTheFileSystemAudioLocation implements IAudioFileSelection {
    public static final AnywhereInTheFileSystemAudioLocation INSTANCE =
            new AnywhereInTheFileSystemAudioLocation();

    private AnywhereInTheFileSystemAudioLocation() {
    }

    @Override
    public boolean isRoot() {
        return false;
    }

    @Nullable
    @Override
    public String getInternalPath() {
        return null;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        return o != null && getClass() == o.getClass();
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @NonNull
    @Override
    public String toString() {
        return "AnywhereInTheFileSystemAudioLocation";
    }
}
