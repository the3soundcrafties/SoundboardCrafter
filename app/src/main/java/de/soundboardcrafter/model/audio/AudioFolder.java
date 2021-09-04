package de.soundboardcrafter.model.audio;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import java.util.Comparator;
import java.util.Objects;

import de.soundboardcrafter.model.AbstractAudioLocation;
import de.soundboardcrafter.model.AssetFolderAudioLocation;
import de.soundboardcrafter.model.FileSystemFolderAudioLocation;

/**
 * A folder containing audio files - or folders with (folders with...) audio files.
 * Might be on the device or in the assets directory.
 */
public class AudioFolder extends AbstractAudioFolderEntry {
    static final Comparator<AudioFolder> BY_PATH =
            Comparator.comparing(AudioFolder::getPath)
                    .thenComparing(AudioFolder::getNumAudioFiles);

    private final AbstractAudioLocation folderLocation;
    private final int numAudioFiles;

    public AudioFolder(@NonNull AbstractAudioLocation folderLocation, int numAudioFiles) {
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
            return ((FileSystemFolderAudioLocation) folderLocation).getInternalPath();
        } else if (folderLocation instanceof AssetFolderAudioLocation) {
            return ((AssetFolderAudioLocation) folderLocation).getInternalPath();
        } else {
            throw new IllegalStateException("Unexpected audio location: " + folderLocation);
        }
    }

    public int getNumAudioFiles() {
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
