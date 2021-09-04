package de.soundboardcrafter.model;

import androidx.annotation.Nullable;

/**
 * Data object for a certain folder where an audio file may reside:
 * <ul>
 * <li>In a certain folder in the file system
 * <li>In a certain folder in the assets
 * </ul>>
 * <p/>
 * Every subclass needs to provide {@link #equals(Object)} and {@link #hashCode()}
 * as appropriate for a <i>value object</i>.
 */
public abstract class AbstractAudioLocation implements IAudioFileSelection {
    /**
     * Returns the display name (not necessary the full path).
     */
    public abstract String getDisplayName();

    @Override
    public abstract boolean equals(@Nullable Object obj);

    @Override
    public abstract int hashCode();
}
