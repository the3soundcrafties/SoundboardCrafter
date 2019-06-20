package de.soundboardcrafter.activity.audiofile.list;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * A folder containing audio files - or folders with (folders with...) audio files.
 */
class AudioFolder extends AbstractAudioFolderEntry {
    private final String path;
    private final int numAudioFiles;

    AudioFolder(String path, int numAudioFiles) {
        this.path = path;
        this.numAudioFiles = numAudioFiles;
    }

    /**
     * Returns only the folder name (not the whole path).
     */
    public String getName() {
        int lastIndexOfSlash = path.lastIndexOf("/");
        if (lastIndexOfSlash < 0) {
            return path;
        }

        return path.substring(lastIndexOfSlash + 1);
    }

    public String getPath() {
        return path;
    }

    int getNumAudioFiles() {
        return numAudioFiles;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        AudioFolder that = (AudioFolder) o;
        return numAudioFiles == that.numAudioFiles &&
                Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), path, numAudioFiles);
    }

    @Override
    @NonNull
    public String toString() {
        return "AudioFolder{" +
                "path='" + path + '\'' +
                ", numAudioFiles=" + numAudioFiles +
                '}';
    }
}
