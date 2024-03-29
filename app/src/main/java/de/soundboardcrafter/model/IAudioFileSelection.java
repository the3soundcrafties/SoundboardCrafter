package de.soundboardcrafter.model;

import android.os.Parcelable;

import java.io.Serializable;

import javax.annotation.Nullable;

/**
 * A selection for audio files:
 * <ul>
 * <li>All files from the file system
 * <li>Only files from a certain folder in the file system
 * <li>Only files from a certain folder in the assets
 * </ul>>
 * <p/>
 * Every subclass needs to provide <code>equals()</code> and <code>hashCode()</code>
 * as appropriate for a <i>value object</i>.
 */
public interface IAudioFileSelection extends Parcelable, Serializable {
    @Nullable
    default String getDisplayPath() {
        return getInternalPath();
    }

    @Nullable
    String getInternalPath();

    boolean isRoot();

    @Override
    default int describeContents() {
        return 0;
    }
}
