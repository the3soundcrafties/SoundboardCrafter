package de.soundboardcrafter.activity.audiofile.list;

import androidx.annotation.NonNull;

import java.util.Objects;

import de.soundboardcrafter.model.AssetAudioLocation;
import de.soundboardcrafter.model.FileSystemAudioLocation;
import de.soundboardcrafter.model.IAudioLocation;

import static androidx.core.util.Preconditions.checkNotNull;

/**
 * A folder containing audio files - or folders with (folders with...) audio files.
 * Might be on the device or in the assets directory.
 */
class AudioFolder extends AbstractAudioFolderEntry {
    private final IAudioLocation audioLocation;
    private final int numAudioFiles;

    AudioFolder(@NonNull IAudioLocation audioLocation, int numAudioFiles) {
        this.audioLocation = checkNotNull(audioLocation, "audioLocation was null");
        this.numAudioFiles = numAudioFiles;
    }

    /**
     * Returns only the folder name (not the whole path).
     */
    public String getName() {
        return audioLocation.getName();
    }

    public String getPath() {
        if (audioLocation instanceof FileSystemAudioLocation) {
            return ((FileSystemAudioLocation) audioLocation).getPath();
        } else if (audioLocation instanceof AssetAudioLocation) {
            return ((AssetAudioLocation) audioLocation).getAssetPath();
        } else {
            throw new IllegalStateException("Unexpected audio location: " + audioLocation);
        }
    }

    public IAudioLocation getAudioLocation() {
        return audioLocation;
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
                Objects.equals(audioLocation, that.audioLocation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), audioLocation, numAudioFiles);
    }

    @Override
    @NonNull
    public String toString() {
        return "AudioFolder{" +
                "audioLocation='" + audioLocation + '\'' +
                ", numAudioFiles=" + numAudioFiles +
                '}';
    }
}
