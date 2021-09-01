package de.soundboardcrafter.model;

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
 * Every subclass needs to provide {@link #equals(Object)} and {@link #hashCode()}
 * as appropriate for a <i>value object</i>.
 */
public interface IAudioFileSelection extends Serializable {
    @Nullable
    default String getDisplayPath() {
        return getInternalPath();
    }

    @Nullable
    String getInternalPath();

    boolean isRoot();
}
