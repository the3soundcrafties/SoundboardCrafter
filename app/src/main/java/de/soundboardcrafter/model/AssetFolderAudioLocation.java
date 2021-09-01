package de.soundboardcrafter.model;

import androidx.annotation.NonNull;

import java.util.Objects;

import de.soundboardcrafter.activity.common.audioloader.AudioLoader;

/**
 * The certain folder <i>inside the assets folder</i> where an audio file may reside.
 */
public class AssetFolderAudioLocation implements IAudioLocation {
    /**
     * Path to the audio file in the assets folder
     */
    @NonNull
    private final String path;

    public AssetFolderAudioLocation(@NonNull final String path) {
        this.path = path;
    }

    @Override
    @NonNull
    public String getDisplayPath() {
        if (isRoot()) {
            return "/";
        }

        return getInternalPath().substring(AudioLoader.ASSET_SOUND_PATH.length());
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
        return AudioLoader.ASSET_SOUND_PATH.equals(getInternalPath());
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
        AssetFolderAudioLocation that = (AssetFolderAudioLocation) o;
        return path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    @NonNull
    @Override
    public String toString() {
        return "AssetFolderAudioLocation{" +
                "assetPath='" + path + '\'' +
                '}';
    }
}
