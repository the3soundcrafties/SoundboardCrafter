package de.soundboardcrafter.activity.audiofile.list;

import androidx.annotation.NonNull;

import java.util.Objects;

import de.soundboardcrafter.model.AssetFolderAudioLocation;
import de.soundboardcrafter.model.FileSystemFolderAudioLocation;
import de.soundboardcrafter.model.IAudioLocation;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A folder containing audio files - or folders with (folders with...) audio files.
 * Might be on the device or in the assets directory.
 */
class AudioFolder extends AbstractAudioFolderEntry {
    private final IAudioLocation folderLocation;
    private final int numAudioFiles;

    AudioFolder(@NonNull IAudioLocation folderLocation, int numAudioFiles) {
        this.folderLocation = checkNotNull(folderLocation, "audioLocation was null");
        this.numAudioFiles = numAudioFiles;
    }

    /**
     * Returns only the folder name (not the whole path).
     */
    public String getName() {
        return folderLocation.getDisplayName();
    }

    public String getPath() {
        if (folderLocation instanceof FileSystemFolderAudioLocation) {
            return ((FileSystemFolderAudioLocation) folderLocation).getPath();
        } else if (folderLocation instanceof AssetFolderAudioLocation) {
            return ((AssetFolderAudioLocation) folderLocation).getAssetPath();
        } else {
            throw new IllegalStateException("Unexpected audio location: " + folderLocation);
        }
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
                Objects.equals(folderLocation, that.folderLocation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), folderLocation, numAudioFiles);
    }

    @Override
    @NonNull
    public String toString() {
        return "AudioFolder{" +
                "audioLocation='" + folderLocation + '\'' +
                ", numAudioFiles=" + numAudioFiles +
                '}';
    }
}
