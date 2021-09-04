package de.soundboardcrafter.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Objects;

import de.soundboardcrafter.activity.common.audioloader.AudioLoader;

/**
 * The certain folder <i>inside the assets folder</i> where an audio file may reside.
 */
public class AssetFolderAudioLocation extends AbstractAudioLocation {
    public static final Parcelable.Creator<AssetFolderAudioLocation> CREATOR
            = new Parcelable.Creator<AssetFolderAudioLocation>() {
        @Override
        public AssetFolderAudioLocation createFromParcel(@NonNull Parcel in) {
            return new AssetFolderAudioLocation(in);
        }

        @Override
        public AssetFolderAudioLocation[] newArray(int size) {
            return new AssetFolderAudioLocation[size];
        }
    };

    /**
     * Path to the audio file in the assets folder
     */
    @NonNull
    private final String path;

    private AssetFolderAudioLocation(@NonNull Parcel in) {
        this(in.readString());
    }

    public AssetFolderAudioLocation(@NonNull final String path) {
        this.path = path;
    }

    @Override
    @NonNull
    public String getDisplayPath() {
        if (isRoot()) {
            return "/";
        }

        return path.substring(AudioLoader.ASSET_SOUND_PATH.length());
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
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(path);
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
