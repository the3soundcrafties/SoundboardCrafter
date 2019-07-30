package de.soundboardcrafter.model;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * The location <i>in the device's file system</i> where an audio file may reside.
 */
public class FileSystemAudioLocation implements IAudioLocation {
    /**
     * Path to the audio file in the device's file system
     */
    @NonNull
    private final String path;

    public FileSystemAudioLocation(@NonNull final String path) {
        this.path = path;
    }

    @Override
    public String getName() {
        int lastIndexOfSlash = path.lastIndexOf("/");
        if (lastIndexOfSlash < 0) {
            return path;
        }

        return path.substring(lastIndexOfSlash + 1);
    }

    @NonNull
    public String getPath() {
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
        FileSystemAudioLocation that = (FileSystemAudioLocation) o;
        return path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    @Override
    public String toString() {
        return "FileSystemAudioLocation{" +
                "path='" + path + '\'' +
                '}';
    }
}
