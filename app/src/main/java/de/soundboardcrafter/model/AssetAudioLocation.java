package de.soundboardcrafter.model;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * The location <i>in the assets folder</i> where an audio file may reside.
 */
public class AssetAudioLocation implements IAudioLocation {
    /**
     * Path to the audio file in the assets folder
     */
    @NonNull
    private final String assetPath;

    public AssetAudioLocation(@NonNull final String assetPath) {
        this.assetPath = assetPath;
    }

    @Override
    public String getName() {
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
        AssetAudioLocation that = (AssetAudioLocation) o;
        return assetPath.equals(that.assetPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assetPath);
    }

    @Override
    public String toString() {
        return "AssetAudioLocation{" +
                "assetPath='" + assetPath + '\'' +
                '}';
    }
}
