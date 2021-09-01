package de.soundboardcrafter.model;

import androidx.annotation.NonNull;

import java.util.Objects;

import javax.annotation.Nonnull;

/**
 * The certain folder <i>in the device's file system</i> where an audio file may reside.
 */
public class FileSystemFolderAudioLocation implements IAudioLocation {
    /**
     * Path to the audio file in the device's file system
     */
    @NonNull
    private final String path;

    public FileSystemFolderAudioLocation(@NonNull final String path) {
        this.path = path;
    }

    @Override
    public String getDisplayName() {
        int lastIndexOfSlash = path.lastIndexOf("/");
        if (lastIndexOfSlash < 0) {
            return path;
        }

        return path.substring(lastIndexOfSlash + 1);
    }

    @Override
    public boolean isRoot() {
        return "/".equals(getInternalPath());
    }

    @Override
    @NonNull
    public String getInternalPath() {
        return path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FileSystemFolderAudioLocation that = (FileSystemFolderAudioLocation) o;
        return path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    @Override
    @Nonnull
    public String toString() {
        return "FileSystemFolderAudioLocation{path='" + path + '\'' + '}';
    }
}
