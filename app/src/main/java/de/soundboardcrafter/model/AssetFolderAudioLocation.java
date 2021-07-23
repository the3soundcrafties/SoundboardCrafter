package de.soundboardcrafter.model;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * The certain folder <i>inside the assets folder</i> where an audio file may reside.
 */
public class AssetFolderAudioLocation implements IAudioLocation {
    /**
     * Path to the audio file in the assets folder
     */
    @NonNull
    private final String assetPath;

    public AssetFolderAudioLocation(@NonNull final String assetPath) {
        this.assetPath = assetPath;
    }

    @Override
    public String getDisplayName() {
        int lastIndexOfSlash = assetPath.lastIndexOf("/");
        if (lastIndexOfSlash < 0) {
            return assetPath;
        }

        return assetPath.substring(lastIndexOfSlash + 1);
    }

    @NonNull
    public String getAssetPath() {
        return assetPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AssetFolderAudioLocation that = (AssetFolderAudioLocation) o;
        return assetPath.equals(that.assetPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assetPath);
    }

    @NonNull
    @Override
    public String toString() {
        return "AssetFolderAudioLocation{" +
                "assetPath='" + assetPath + '\'' +
                '}';
    }
}
