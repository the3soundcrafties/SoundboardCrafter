package de.soundboardcrafter.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Objects;

import javax.annotation.Nonnull;

/**
 * The certain folder <i>in the device's file system</i> where an audio file may reside.
 */
public class FileSystemFolderAudioLocation extends AbstractAudioLocation {
    public static final Parcelable.Creator<FileSystemFolderAudioLocation> CREATOR
            = new Parcelable.Creator<FileSystemFolderAudioLocation>() {
        @Override
        public FileSystemFolderAudioLocation createFromParcel(@NonNull Parcel in) {
            return new FileSystemFolderAudioLocation(in);
        }

        @Override
        public FileSystemFolderAudioLocation[] newArray(int size) {
            return new FileSystemFolderAudioLocation[size];
        }
    };

    /**
     * Path to the audio file in the device's file system
     */
    @NonNull
    private final String path;

    private FileSystemFolderAudioLocation(@NonNull Parcel in) {
        this(in.readString());
    }

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
