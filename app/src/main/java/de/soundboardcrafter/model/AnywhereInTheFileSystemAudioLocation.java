package de.soundboardcrafter.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import javax.annotation.Nullable;

public class AnywhereInTheFileSystemAudioLocation implements IAudioFileSelection {
    public static final Parcelable.Creator<AnywhereInTheFileSystemAudioLocation> CREATOR
            = new Parcelable.Creator<AnywhereInTheFileSystemAudioLocation>() {
        @Override
        public AnywhereInTheFileSystemAudioLocation createFromParcel(Parcel in) {
            return INSTANCE;
        }

        @Override
        public AnywhereInTheFileSystemAudioLocation[] newArray(int size) {
            return new AnywhereInTheFileSystemAudioLocation[size];
        }
    };

    public static final AnywhereInTheFileSystemAudioLocation INSTANCE =
            new AnywhereInTheFileSystemAudioLocation();

    private AnywhereInTheFileSystemAudioLocation() {
    }

    @Override
    public boolean isRoot() {
        return false;
    }

    @Nullable
    @Override
    public String getInternalPath() {
        return null;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        // Nothing to write
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        return o != null && getClass() == o.getClass();
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @NonNull
    @Override
    public String toString() {
        return "AnywhereInTheFileSystemAudioLocation";
    }

}
